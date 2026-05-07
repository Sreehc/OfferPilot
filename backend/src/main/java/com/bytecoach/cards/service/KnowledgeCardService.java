package com.bytecoach.cards.service;

import com.bytecoach.cards.dto.CardRateRequest;
import com.bytecoach.cards.dto.CardTaskCreateRequest;
import com.bytecoach.cards.vo.KnowledgeCardTaskVO;

public interface KnowledgeCardService {
    KnowledgeCardTaskVO createTask(Long userId, CardTaskCreateRequest request);

    KnowledgeCardTaskVO getActiveTask(Long userId);

    KnowledgeCardTaskVO getTask(Long userId, Long taskId);

    KnowledgeCardTaskVO startTask(Long userId, Long taskId);

    KnowledgeCardTaskVO rate(Long userId, Long taskId, CardRateRequest request);

    KnowledgeCardTaskVO restart(Long userId, Long taskId);

    void invalidateByDocId(Long docId, String reason);
}
