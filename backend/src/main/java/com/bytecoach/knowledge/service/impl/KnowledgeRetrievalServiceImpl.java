package com.bytecoach.knowledge.service.impl;

import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.knowledge.service.KnowledgeRetrievalService;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final CategoryService categoryService;

    @Override
    public List<KnowledgeSearchVO.Reference> searchReferences(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        // DB-level pre-filter: only load chunks whose content matches at least one keyword
        // This avoids loading the entire chunk table into memory.
        // For better performance, consider adding FULLTEXT INDEX on knowledge_chunk.content:
        //   ALTER TABLE knowledge_chunk ADD FULLTEXT INDEX ft_content (content) WITH PARSER ngram;
        // and switching to MATCH ... AGAINST syntax.
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk> chunkQuery =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>();
        // Build OR conditions: content LIKE '%keyword1%' OR content LIKE '%keyword2%' ...
        boolean first = true;
        for (String keyword : keywords) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
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
        Set<Long> categoryIds = docMap.values().stream()
                .map(KnowledgeDoc::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));

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
            Category category = categoryMap.get(doc.getCategoryId());
            String docTitle = category == null ? doc.getTitle() : category.getName() + " · " + doc.getTitle();
            scored.add(new ScoredChunk(score, KnowledgeSearchVO.Reference.builder()
                    .docId(doc.getId())
                    .chunkId(chunk.getId())
                    .docTitle(docTitle)
                    .snippet(snippet(chunk.getContent(), keywords))
                    .build()));
        }
        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(limit)
                .map(ScoredChunk::reference)
                .toList();
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

    private record ScoredChunk(int score, KnowledgeSearchVO.Reference reference) {
    }
}
