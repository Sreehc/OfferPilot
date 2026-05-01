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

public interface KnowledgeService extends IService<KnowledgeDoc> {
    PageResult<KnowledgeDocVO> listDocs(KnowledgeListQuery query);

    KnowledgeSearchVO search(KnowledgeSearchRequest request);

    KnowledgeDocVO importSeed(KnowledgeImportRequest request);

    KnowledgeDocVO rechunk(Long docId);

    KnowledgeDocVO reindex(Long docId);
}
