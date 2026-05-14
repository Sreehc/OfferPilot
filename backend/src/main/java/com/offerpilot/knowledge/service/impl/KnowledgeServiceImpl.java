package com.offerpilot.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.offerpilot.ai.service.EmbeddingGateway;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.cards.service.KnowledgeCardService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.knowledge.dto.KnowledgeImportRequest;
import com.offerpilot.knowledge.dto.KnowledgeListQuery;
import com.offerpilot.knowledge.dto.KnowledgeSearchRequest;
import com.offerpilot.knowledge.entity.KnowledgeChunk;
import com.offerpilot.knowledge.entity.KnowledgeDoc;
import com.offerpilot.knowledge.mapper.KnowledgeChunkMapper;
import com.offerpilot.knowledge.mapper.KnowledgeDocMapper;
import com.offerpilot.cards.entity.KnowledgeCard;
import com.offerpilot.cards.entity.KnowledgeCardTask;
import com.offerpilot.cards.mapper.KnowledgeCardMapper;
import com.offerpilot.cards.mapper.KnowledgeCardTaskMapper;
import com.offerpilot.knowledge.service.DocumentParserService;
import com.offerpilot.knowledge.service.KnowledgeRetrievalService;
import com.offerpilot.knowledge.service.KnowledgeService;
import com.offerpilot.knowledge.service.VectorStoreService;
import com.offerpilot.knowledge.vo.KnowledgeDocVO;
import com.offerpilot.knowledge.vo.KnowledgeSearchVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
    private final DocumentParserService documentParserService;
    private final KnowledgeCardService knowledgeCardService;
    private final KnowledgeCardTaskMapper knowledgeCardTaskMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;
    private final FileStorageService fileStorageService;
    private final UploadPolicyService uploadPolicyService;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private KnowledgeServiceImpl self;

    @Override
    public PageResult<KnowledgeDocVO> listDocs(KnowledgeListQuery query) {
        Page<KnowledgeDoc> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<KnowledgeDoc> queryWrapper = new LambdaQueryWrapper<KnowledgeDoc>()
                .eq(query.getCategoryId() != null, KnowledgeDoc::getCategoryId, query.getCategoryId())
                .eq(StringUtils.hasText(query.getLibraryScope()), KnowledgeDoc::getLibraryScope, query.getLibraryScope())
                .eq(StringUtils.hasText(query.getBusinessType()), KnowledgeDoc::getBusinessType, query.getBusinessType())
                .eq(StringUtils.hasText(query.getFileType()), KnowledgeDoc::getFileType, query.getFileType())
                .eq(StringUtils.hasText(query.getParseStatus()), KnowledgeDoc::getParseStatus, query.getParseStatus())
                .eq(StringUtils.hasText(query.getIndexStatus()), KnowledgeDoc::getIndexStatus, query.getIndexStatus())
                .eq(StringUtils.hasText(query.getStatus()), KnowledgeDoc::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), keywordWrapper -> keywordWrapper
                        .like(KnowledgeDoc::getTitle, query.getKeyword())
                        .or()
                        .like(KnowledgeDoc::getSummary, query.getKeyword()))
                .orderByDesc(KnowledgeDoc::getUpdateTime);
        IPage<KnowledgeDoc> result = page(page, queryWrapper);
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
            doc.setLibraryScope("system");
            doc.setBusinessType("system_knowledge");
            doc.setSourceType("seed");
            doc.setFileType(fileTypeFromName(metadata.getFileName()));
            doc.setFileUrl(fileUrl(metadata.getFileName()));
        }
        doc.setTitle(metadata.getTitle());
        doc.setCategoryId(categoryId);
        doc.setSummary(metadata.getSummary());
        doc.setParseStatus("parsed");
        doc.setIndexStatus("indexed");
        doc.setStatus("indexed");
        saveOrUpdate(doc);
        rebuildChunks(doc, metadata.getFileName());
        updateById(doc);
        return buildDocVOs(List.of(doc)).get(0);
    }

    @Override
    @Transactional
    public KnowledgeDocVO rechunk(Long docId) {
        KnowledgeDoc doc = getRequiredDoc(docId);
        rebuildChunks(doc, fileNameFromUrl(doc.getFileUrl()));
        doc.setParseStatus("parsed");
        doc.setIndexStatus("indexed");
        doc.setStatus("indexed");
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
        doc.setParseStatus("parsed");
        doc.setIndexStatus("indexed");
        doc.setStatus("indexed");
        updateById(doc);
        return buildDocVOs(List.of(doc)).get(0);
    }

    @Override
    @Transactional
    public List<KnowledgeDocVO> batchRechunk(List<Long> docIds) {
        return distinctDocIds(docIds).stream()
                .map(this::rechunk)
                .toList();
    }

    @Override
    @Transactional
    public List<KnowledgeDocVO> batchReindex(List<Long> docIds) {
        return distinctDocIds(docIds).stream()
                .map(this::reindex)
                .toList();
    }

    @Override
    public KnowledgeDocVO upload(Long userId, MultipartFile file, Long categoryId) {
        String originalFilename = file.getOriginalFilename();
        uploadPolicyService.validate(StorageDirectory.KNOWLEDGE, originalFilename, file.getContentType(), file.getSize());
        if (!documentParserService.isSupported(originalFilename)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "不支持的文件格式，仅支持: " + String.join(", ", documentParserService.supportedExtensions()));
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "文件读取失败: " + e.getMessage());
        }

        // Parse document into chunks
        List<String> chunks;
        try {
            chunks = documentParserService.parse(new ByteArrayInputStream(fileBytes), originalFilename);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "文件解析失败: " + e.getMessage());
        }
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档内容为空或无法解析");
        }

        StoredFile storedFile = fileStorageService.store(
                StorageDirectory.KNOWLEDGE,
                originalFilename,
                fileBytes,
                file.getContentType());

        // Create knowledge doc record
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setTitle(stripExtension(originalFilename));
        doc.setCategoryId(categoryId);
        doc.setUserId(userId);
        doc.setLibraryScope("personal");
        doc.setBusinessType("user_note");
        doc.setSourceType("user_upload");
        doc.setFileType(fileTypeFromName(originalFilename));
        doc.setFileUrl(storedFile.getStorageKey());
        doc.setSummary(chunks.get(0).length() > 200 ? chunks.get(0).substring(0, 200) + "..." : chunks.get(0));
        doc.setParseStatus("parsed");
        doc.setIndexStatus("pending");
        doc.setStatus("parsed");
        save(doc);

        // Insert chunks into DB
        List<KnowledgeChunk> insertedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkIndex(i + 1);
            chunk.setContent(chunks.get(i));
            chunk.setTokenCount(estimateTokenCount(chunks.get(i)));
            knowledgeChunkMapper.insert(chunk);
            insertedChunks.add(chunk);
        }

        // Async vectorization
        self.vectorizeChunksAsync(doc.getId(), insertedChunks, chunks);

        log.info("User {} uploaded document '{}', {} chunks; parse_status=parsed, index_status=pending",
                userId, originalFilename, chunks.size());
        return buildDocVOs(List.of(doc)).get(0);
    }

    private List<Long> distinctDocIds(List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "docIds is required");
        }
        return docIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public PageResult<KnowledgeDocVO> listUserDocs(Long userId, KnowledgeListQuery query) {
        Page<KnowledgeDoc> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<KnowledgeDoc> queryWrapper = new LambdaQueryWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getUserId, userId)
                .eq(StringUtils.hasText(query.getLibraryScope()), KnowledgeDoc::getLibraryScope, query.getLibraryScope())
                .eq(StringUtils.hasText(query.getBusinessType()), KnowledgeDoc::getBusinessType, query.getBusinessType())
                .eq(StringUtils.hasText(query.getFileType()), KnowledgeDoc::getFileType, query.getFileType())
                .eq(StringUtils.hasText(query.getParseStatus()), KnowledgeDoc::getParseStatus, query.getParseStatus())
                .eq(StringUtils.hasText(query.getIndexStatus()), KnowledgeDoc::getIndexStatus, query.getIndexStatus())
                .eq(StringUtils.hasText(query.getStatus()), KnowledgeDoc::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), keywordWrapper -> keywordWrapper
                        .like(KnowledgeDoc::getTitle, query.getKeyword())
                        .or()
                        .like(KnowledgeDoc::getSummary, query.getKeyword()))
                .orderByDesc(KnowledgeDoc::getUpdateTime);
        IPage<KnowledgeDoc> result = page(page, queryWrapper);
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
    @Transactional
    public void deleteUserDoc(Long userId, Long docId) {
        KnowledgeDoc doc = getRequiredDoc(docId);
        if (!userId.equals(doc.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能删除自己上传的文档");
        }
        knowledgeCardService.invalidateByDocId(docId, "来源文档已删除");
        // Remove chunks from vector store and DB
        vectorStoreService.removeByDocId(docId);
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocId, docId));
        removeById(docId);
        log.info("User {} deleted document {}, title='{}'", userId, docId, doc.getTitle());
    }

    @Async
    public void vectorizeChunksAsync(Long docId, List<KnowledgeChunk> chunks, List<String> texts) {
        vectorizeChunks(docId, chunks, texts);
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
        Map<Long, KnowledgeCardTask> deckMap = knowledgeCardTaskMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeCardTask>()
                                .in(KnowledgeCardTask::getDocId, docs.stream().map(KnowledgeDoc::getId).toList())
                                .eq(KnowledgeCardTask::getSourceType, "knowledge_doc")
                                .ne(KnowledgeCardTask::getStatus, "invalid")
                                .orderByDesc(KnowledgeCardTask::getUpdateTime))
                .stream()
                .collect(Collectors.toMap(KnowledgeCardTask::getDocId, Function.identity(), (left, right) -> left));
        Map<Long, List<KnowledgeCard>> cardsByTaskId = deckMap.isEmpty()
                ? Map.of()
                : knowledgeCardMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeCard>()
                                .in(KnowledgeCard::getTaskId, deckMap.values().stream().map(KnowledgeCardTask::getId).toList()))
                        .stream()
                        .collect(Collectors.groupingBy(KnowledgeCard::getTaskId));
        return docs.stream()
                .map(doc -> {
                    Category category = categoryMap.get(doc.getCategoryId());
                    KnowledgeCardTask deck = deckMap.get(doc.getId());
                    List<KnowledgeCard> cards = deck == null ? List.of() : cardsByTaskId.getOrDefault(deck.getId(), List.of());
                    String cardTypes = cards.stream()
                            .map(KnowledgeCard::getCardType)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .collect(Collectors.joining(","));
                    return KnowledgeDocVO.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .categoryId(doc.getCategoryId())
                            .categoryName(category == null ? null : category.getName())
                            .libraryScope(doc.getLibraryScope())
                            .businessType(doc.getBusinessType())
                            .sourceType(doc.getSourceType())
                            .fileType(doc.getFileType())
                            .fileUrl(doc.getFileUrl())
                            .parseStatus(doc.getParseStatus())
                            .indexStatus(doc.getIndexStatus())
                            .status(doc.getStatus())
                            .summary(doc.getSummary())
                            .chunkCount(chunkCountMap.getOrDefault(doc.getId(), 0L).intValue())
                            .cardDeckId(deck == null ? null : deck.getId())
                            .cardDeckTitle(deck == null ? null : deck.getDeckTitle())
                            .cardCount(cards.size())
                            .cardGeneratedAt(deck == null ? null : deck.getUpdateTime())
                            .cardTypes(StringUtils.hasText(cardTypes) ? cardTypes : null)
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
        vectorizeChunks(doc.getId(), insertedChunks, chunkTexts);
    }

    private void vectorizeChunks(Long docId, List<KnowledgeChunk> chunks, List<String> texts) {
        try {
            List<float[]> embeddings = embeddingGateway.embedBatch(texts, "knowledge.index");
            for (int i = 0; i < chunks.size(); i++) {
                float[] embedding = embeddings.get(i);
                if (embedding.length > 0) {
                    String key = vectorStoreService.store(chunks.get(i).getId(), chunks.get(i).getDocId(), texts.get(i), embedding);
                    chunks.get(i).setVectorId(key);
                    knowledgeChunkMapper.updateById(chunks.get(i));
                }
            }
            lambdaUpdate()
                    .eq(KnowledgeDoc::getId, docId)
                    .set(KnowledgeDoc::getParseStatus, "parsed")
                    .set(KnowledgeDoc::getIndexStatus, "indexed")
                    .set(KnowledgeDoc::getStatus, "indexed")
                    .update();
            log.info("Vectorized {} chunks for doc {}", chunks.size(), chunks.get(0).getDocId());
        } catch (Exception e) {
            lambdaUpdate()
                    .eq(KnowledgeDoc::getId, docId)
                    .set(KnowledgeDoc::getParseStatus, "parsed")
                    .set(KnowledgeDoc::getIndexStatus, "failed")
                    .set(KnowledgeDoc::getStatus, "parsed")
                    .update();
            log.warn("Vectorization failed for doc {}, chunks stored without vectors: {}",
                    chunks.isEmpty() ? docId : chunks.get(0).getDocId(), e.getMessage());
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

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private String fileTypeFromName(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
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
