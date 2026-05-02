package com.bytecoach.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.knowledge.dto.KnowledgeImportRequest;
import com.bytecoach.knowledge.dto.KnowledgeListQuery;
import com.bytecoach.knowledge.dto.KnowledgeSearchRequest;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.vo.KnowledgeDocVO;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService extends IService<KnowledgeDoc> {
    PageResult<KnowledgeDocVO> listDocs(KnowledgeListQuery query);

    KnowledgeSearchVO search(KnowledgeSearchRequest request);

    KnowledgeDocVO importSeed(KnowledgeImportRequest request);

    KnowledgeDocVO rechunk(Long docId);

    KnowledgeDocVO reindex(Long docId);

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
