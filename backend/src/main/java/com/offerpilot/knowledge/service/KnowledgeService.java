package com.offerpilot.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.knowledge.dto.KnowledgeImportRequest;
import com.offerpilot.knowledge.dto.KnowledgeListQuery;
import com.offerpilot.knowledge.dto.KnowledgeSearchRequest;
import com.offerpilot.knowledge.entity.KnowledgeDoc;
import com.offerpilot.knowledge.vo.KnowledgeDocVO;
import com.offerpilot.knowledge.vo.KnowledgeSearchVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService extends IService<KnowledgeDoc> {
    PageResult<KnowledgeDocVO> listDocs(KnowledgeListQuery query);

    KnowledgeSearchVO search(KnowledgeSearchRequest request);

    KnowledgeDocVO importSeed(KnowledgeImportRequest request);

    KnowledgeDocVO rechunk(Long docId);

    KnowledgeDocVO reindex(Long docId);

    List<KnowledgeDocVO> batchRechunk(List<Long> docIds);

    List<KnowledgeDocVO> batchReindex(List<Long> docIds);

    /**
     * Upload a user document: parse, chunk, and vectorize.
     */
    KnowledgeDocVO upload(Long userId, MultipartFile file, Long categoryId);

    /**
     * List documents uploaded by a specific user.
     */
    PageResult<KnowledgeDocVO> listUserDocs(Long userId, KnowledgeListQuery query);

    /**
     * Delete a user-uploaded document (only owner can delete).
     */
    void deleteUserDoc(Long userId, Long docId);
}
