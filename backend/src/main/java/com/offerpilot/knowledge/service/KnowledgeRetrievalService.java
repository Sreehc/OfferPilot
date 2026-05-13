package com.offerpilot.knowledge.service;

import com.offerpilot.knowledge.vo.KnowledgeSearchVO;
import java.util.List;

public interface KnowledgeRetrievalService {
    List<KnowledgeSearchVO.Reference> searchReferences(String query, int limit);
}
