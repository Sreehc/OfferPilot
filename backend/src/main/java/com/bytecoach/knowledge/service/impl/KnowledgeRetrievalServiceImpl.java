package com.bytecoach.knowledge.service.impl;

import com.bytecoach.ai.config.VectorProperties;
import com.bytecoach.ai.service.EmbeddingGateway;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.knowledge.service.KnowledgeRetrievalService;
import com.bytecoach.knowledge.service.VectorStoreService;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final CategoryService categoryService;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStoreService vectorStoreService;
    private final VectorProperties vectorProperties;

    @Override
    public List<KnowledgeSearchVO.Reference> searchReferences(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        // Strategy: try vector search first, merge with keyword search for hybrid results
        List<ScoredChunk> vectorResults = vectorSearch(query, limit * 2);
        List<ScoredChunk> keywordResults = keywordSearch(query, limit * 2);

        List<ScoredChunk> merged = mergeResults(vectorResults, keywordResults);
        return merged.stream()
                .limit(limit)
                .map(ScoredChunk::reference)
                .toList();
    }

    private List<ScoredChunk> vectorSearch(String query, int limit) {
        float[] queryEmbedding = embeddingGateway.embed(query);
        if (queryEmbedding.length == 0) {
            log.debug("Embedding disabled or failed, skipping vector search");
            return List.of();
        }

        List<VectorStoreService.VectorSearchResult> vectorHits = vectorStoreService.search(queryEmbedding, limit);
        if (vectorHits.isEmpty()) {
            return List.of();
        }

        double threshold = vectorProperties.getSimilarityThreshold();
        Set<Long> chunkIds = vectorHits.stream()
                .filter(hit -> hit.score() >= threshold)
                .map(VectorStoreService.VectorSearchResult::chunkId)
                .collect(Collectors.toSet());
        if (chunkIds.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeChunk> chunkMap = knowledgeChunkMapper.selectBatchIds(chunkIds).stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));
        Map<Long, KnowledgeDoc> docMap = knowledgeDocMapper.selectBatchIds(
                        chunkMap.values().stream().map(KnowledgeChunk::getDocId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(KnowledgeDoc::getId, Function.identity()));
        Map<Long, Category> categoryMap = loadCategoryMap(docMap.values());

        List<ScoredChunk> results = new ArrayList<>();
        for (var hit : vectorHits) {
            if (hit.score() < threshold) continue;
            KnowledgeChunk chunk = chunkMap.get(hit.chunkId());
            if (chunk == null) continue;
            KnowledgeDoc doc = docMap.get(chunk.getDocId());
            if (doc == null || !"indexed".equals(doc.getStatus())) continue;

            String docTitle = buildDocTitle(doc, categoryMap);
            String snippet = snippet(chunk.getContent(), 120);
            results.add(new ScoredChunk(hit.score(),
                    KnowledgeSearchVO.Reference.builder()
                            .docId(doc.getId())
                            .chunkId(chunk.getId())
                            .docTitle(docTitle)
                            .snippet(snippet)
                            .score(hit.score())
                            .build()));
        }
        log.info("Vector search: query='{}', hits={}, aboveThreshold={}", query, vectorHits.size(), results.size());
        return results;
    }

    private List<ScoredChunk> keywordSearch(String query, int limit) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk> chunkQuery =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        boolean first = true;
        for (String keyword : keywords) {
            if (first) {
                chunkQuery.like(KnowledgeChunk::getContent, keyword);
                first = false;
            } else {
                chunkQuery.or().like(KnowledgeChunk::getContent, keyword);
            }
        }
        chunkQuery.orderByAsc(KnowledgeChunk::getDocId, KnowledgeChunk::getChunkIndex);
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(chunkQuery);

        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<Long, KnowledgeDoc> docMap = knowledgeDocMapper.selectBatchIds(
                        chunks.stream().map(KnowledgeChunk::getDocId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeDoc::getId, Function.identity()));
        Map<Long, Category> categoryMap = loadCategoryMap(docMap.values());

        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            KnowledgeDoc doc = docMap.get(chunk.getDocId());
            if (doc == null || !"indexed".equals(doc.getStatus())) {
                continue;
            }
            String haystack = (doc.getTitle() == null ? "" : doc.getTitle()) + "\n"
                    + (doc.getSummary() == null ? "" : doc.getSummary()) + "\n"
                    + (chunk.getContent() == null ? "" : chunk.getContent());
            int score = score(haystack, keywords);
            if (score <= 0) {
                continue;
            }
            String docTitle = buildDocTitle(doc, categoryMap);
            // Normalize keyword score to 0-1 range for merging
            float normalizedScore = Math.min(1.0f, score / 15.0f);
            scored.add(new ScoredChunk(normalizedScore,
                    KnowledgeSearchVO.Reference.builder()
                            .docId(doc.getId())
                            .chunkId(chunk.getId())
                            .docTitle(docTitle)
                            .snippet(snippet(chunk.getContent(), keywords))
                            .score(normalizedScore)
                            .build()));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Merge vector and keyword results, deduplicating by chunkId.
     * Vector results get priority; keyword-only results fill remaining slots.
     */
    private List<ScoredChunk> mergeResults(List<ScoredChunk> vector, List<ScoredChunk> keyword) {
        // Use LinkedHashMap to preserve insertion order
        Map<Long, ScoredChunk> merged = new LinkedHashMap<>();

        // Add vector results first (they have semantic relevance)
        for (ScoredChunk chunk : vector) {
            merged.put(chunk.reference().getChunkId(), chunk);
        }

        // Add keyword results that aren't already in vector results
        for (ScoredChunk chunk : keyword) {
            merged.putIfAbsent(chunk.reference().getChunkId(), chunk);
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();
    }

    private Map<Long, Category> loadCategoryMap(java.util.Collection<KnowledgeDoc> docs) {
        Set<Long> categoryIds = docs.stream()
                .map(KnowledgeDoc::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return categoryIds.isEmpty()
                ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private String buildDocTitle(KnowledgeDoc doc, Map<Long, Category> categoryMap) {
        Category category = categoryMap.get(doc.getCategoryId());
        return category == null ? doc.getTitle() : category.getName() + " · " + doc.getTitle();
    }

    private List<String> extractKeywords(String query) {
        return java.util.Arrays.stream(query.trim().split("[\\s,，、;；]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    if (!list.contains(query.trim())) {
                        list.add(0, query.trim());
                    }
                    return list;
                }));
    }

    private int score(String haystack, List<String> keywords) {
        int score = 0;
        String source = haystack.toLowerCase();
        for (int i = 0; i < keywords.size(); i++) {
            String keyword = keywords.get(i).toLowerCase();
            int index = source.indexOf(keyword);
            if (index >= 0) {
                score += i == 0 ? 8 : 3;
                if (index < 120) {
                    score += 2;
                }
            }
        }
        return score;
    }

    private String snippet(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) + "..." : normalized;
    }

    private String snippet(String content, List<String> keywords) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        for (String keyword : keywords) {
            int index = normalized.toLowerCase().indexOf(keyword.toLowerCase());
            if (index >= 0) {
                int start = Math.max(0, index - 30);
                int end = Math.min(normalized.length(), index + keyword.length() + 70);
                return normalized.substring(start, end);
            }
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private record ScoredChunk(double score, KnowledgeSearchVO.Reference reference) {
    }
}
