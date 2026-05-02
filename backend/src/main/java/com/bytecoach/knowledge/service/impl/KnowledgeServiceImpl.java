package com.bytecoach.knowledge.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytecoach.ai.service.EmbeddingGateway;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.knowledge.dto.KnowledgeImportRequest;
import com.bytecoach.knowledge.dto.KnowledgeListQuery;
import com.bytecoach.knowledge.dto.KnowledgeSearchRequest;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.knowledge.service.KnowledgeRetrievalService;
import com.bytecoach.knowledge.service.KnowledgeService;
import com.bytecoach.knowledge.service.VectorStoreService;
import com.bytecoach.knowledge.vo.KnowledgeDocVO;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeDocMapper, KnowledgeDoc> implements KnowledgeService {

    private static final String SEED_INDEX_PATH = "seed/knowledge/index.json";

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStoreService vectorStoreService;

    @Override
    public PageResult<KnowledgeDocVO> listDocs(KnowledgeListQuery query) {
        Page<KnowledgeDoc> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<KnowledgeDoc> result = page(page, lambdaQuery()
                .eq(query.getCategoryId() != null, KnowledgeDoc::getCategoryId, query.getCategoryId())
                .eq(StringUtils.hasText(query.getStatus()), KnowledgeDoc::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                        .like(KnowledgeDoc::getTitle, query.getKeyword())
                        .or()
                        .like(KnowledgeDoc::getSummary, query.getKeyword()))
                .orderByDesc(KnowledgeDoc::getUpdateTime));
        List<KnowledgeDocVO> voList = buildDocVOs(result.getRecords());
        return PageResult.<KnowledgeDocVO>builder()
                .records(voList)
                .total(result.getTotal())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .totalPages((int) result.getPages())
                .build();
    }

    @Override
    public KnowledgeSearchVO search(KnowledgeSearchRequest request) {
        return KnowledgeSearchVO.builder()
                .query(request.getQuery())
                .references(knowledgeRetrievalService.searchReferences(request.getQuery(), 5))
                .build();
    }

    @Override
    @Transactional
    public KnowledgeDocVO importSeed(KnowledgeImportRequest request) {
        SeedMetadata metadata = findSeed(request.getSeedKey());
        Long categoryId = request.getCategoryId();
        if (categoryId == null) {
            Category category = categoryService.findOrCreateKnowledgeCategory(metadata.getCategoryName());
            categoryId = category.getId();
        } else {
            categoryService.getRequiredById(categoryId);
        }

        KnowledgeDoc doc = lambdaQuery()
                .eq(KnowledgeDoc::getFileUrl, fileUrl(metadata.getFileName()))
                .one();
        if (doc != null && !Boolean.TRUE.equals(request.getForceRebuild())) {
            return buildDocVOs(List.of(doc)).get(0);
        }
        if (doc == null) {
            doc = new KnowledgeDoc();
            doc.setSourceType("seed");
            doc.setFileUrl(fileUrl(metadata.getFileName()));
        }
        doc.setTitle(metadata.getTitle());
        doc.setCategoryId(categoryId);
        doc.setSummary(metadata.getSummary());
        doc.setStatus("draft");
        saveOrUpdate(doc);
        rebuildChunks(doc, metadata.getFileName());
        doc.setStatus("indexed");
        updateById(doc);
        return buildDocVOs(List.of(doc)).get(0);
    }

    @Override
    @Transactional
    public KnowledgeDocVO rechunk(Long docId) {
        KnowledgeDoc doc = getRequiredDoc(docId);
        rebuildChunks(doc, fileNameFromUrl(doc.getFileUrl()));
        doc.setStatus("parsed");
        updateById(doc);
        return buildDocVOs(List.of(doc)).get(0);
    }

    @Override
    @Transactional
    public KnowledgeDocVO reindex(Long docId) {
        KnowledgeDoc doc = getRequiredDoc(docId);
        if (knowledgeChunkMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getDocId, docId)) == 0) {
            rebuildChunks(doc, fileNameFromUrl(doc.getFileUrl()));
        }
        doc.setStatus("indexed");
        updateById(doc);
        return buildDocVOs(List.of(doc)).get(0);
    }

    private KnowledgeDoc getRequiredDoc(Long docId) {
        KnowledgeDoc doc = getById(docId);
        if (doc == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge doc not found");
        }
        return doc;
    }

    private List<KnowledgeDocVO> buildDocVOs(List<KnowledgeDoc> docs) {
        if (docs.isEmpty()) {
            return List.of();
        }
        Map<Long, Category> categoryMap = categoryService.listByIds(
                        docs.stream().map(KnowledgeDoc::getCategoryId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, Long> chunkCountMap = knowledgeChunkMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                                .in(KnowledgeChunk::getDocId, docs.stream().map(KnowledgeDoc::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(KnowledgeChunk::getDocId, Collectors.counting()));
        return docs.stream()
                .map(doc -> {
                    Category category = categoryMap.get(doc.getCategoryId());
                    return KnowledgeDocVO.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .categoryId(doc.getCategoryId())
                            .categoryName(category == null ? null : category.getName())
                            .sourceType(doc.getSourceType())
                            .fileUrl(doc.getFileUrl())
                            .status(doc.getStatus())
                            .summary(doc.getSummary())
                            .chunkCount(chunkCountMap.getOrDefault(doc.getId(), 0L).intValue())
                            .updateTime(doc.getUpdateTime())
                            .build();
                })
                .sorted(Comparator.comparing(KnowledgeDocVO::getUpdateTime).reversed())
                .toList();
    }

    private void rebuildChunks(KnowledgeDoc doc, String fileName) {
        String markdown = loadSeedMarkdown(fileName);
        List<String> chunkTexts = splitMarkdown(markdown);

        // Remove old chunks from DB and vector store
        knowledgeChunkMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocId, doc.getId()));
        vectorStoreService.removeByDocId(doc.getId());

        // Insert new chunks into DB
        List<KnowledgeChunk> insertedChunks = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkIndex(i + 1);
            chunk.setContent(chunkTexts.get(i));
            chunk.setTokenCount(estimateTokenCount(chunkTexts.get(i)));
            chunk.setVectorId(null);
            knowledgeChunkMapper.insert(chunk);
            insertedChunks.add(chunk);
        }

        // Vectorize chunks in batch (if embedding is enabled)
        vectorizeChunks(insertedChunks, chunkTexts);
    }

    private void vectorizeChunks(List<KnowledgeChunk> chunks, List<String> texts) {
        try {
            List<float[]> embeddings = embeddingGateway.embedBatch(texts);
            for (int i = 0; i < chunks.size(); i++) {
                float[] embedding = embeddings.get(i);
                if (embedding.length > 0) {
                    String key = vectorStoreService.store(chunks.get(i).getId(), chunks.get(i).getDocId(), texts.get(i), embedding);
                    chunks.get(i).setVectorId(key);
                    knowledgeChunkMapper.updateById(chunks.get(i));
                }
            }
            log.info("Vectorized {} chunks for doc {}", chunks.size(), chunks.get(0).getDocId());
        } catch (Exception e) {
            log.warn("Vectorization failed for doc {}, chunks stored without vectors: {}",
                    chunks.isEmpty() ? "?" : chunks.get(0).getDocId(), e.getMessage());
        }
    }

    private List<String> splitMarkdown(String markdown) {
        String normalized = markdown.replace("\r\n", "\n").trim();
        String[] sections = normalized.split("(?m)^#{1,3}\\s+");
        List<String> chunks = new ArrayList<>();
        for (String raw : sections) {
            String section = raw.trim();
            if (!StringUtils.hasText(section)) {
                continue;
            }
            if (section.length() <= 520) {
                chunks.add(section);
                continue;
            }
            for (String paragraph : section.split("\\n\\n+")) {
                String trimmed = paragraph.trim();
                if (!StringUtils.hasText(trimmed)) {
                    continue;
                }
                if (trimmed.length() <= 520) {
                    chunks.add(trimmed);
                } else {
                    int start = 0;
                    while (start < trimmed.length()) {
                        int end = Math.min(trimmed.length(), start + 520);
                        chunks.add(trimmed.substring(start, end));
                        start = end;
                    }
                }
            }
        }
        return chunks;
    }

    private int estimateTokenCount(String content) {
        return Math.max(1, content.length() / 4);
    }

    private SeedMetadata findSeed(String seedKey) {
        return loadSeedIndex().stream()
                .filter(item -> item.getSeedKey().equals(seedKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "seed knowledge not found"));
    }

    private List<SeedMetadata> loadSeedIndex() {
        try (InputStream inputStream = new ClassPathResource(SEED_INDEX_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<SeedMetadata>>() {
            });
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "failed to load knowledge seed index");
        }
    }

    private String loadSeedMarkdown(String fileName) {
        try (InputStream inputStream = new ClassPathResource("seed/knowledge/" + fileName).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "failed to load knowledge seed markdown");
        }
    }

    private String fileUrl(String fileName) {
        return "seed://knowledge/" + fileName;
    }

    private String fileNameFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith("seed://knowledge/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "doc source is not a built-in seed");
        }
        return fileUrl.substring("seed://knowledge/".length());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SeedMetadata {
        private String seedKey;
        private String title;
        private String categoryName;
        private String summary;
        private String fileName;
    }
}
