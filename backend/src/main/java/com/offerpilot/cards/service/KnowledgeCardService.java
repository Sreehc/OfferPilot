package com.offerpilot.cards.service;

import com.offerpilot.cards.dto.CardRateRequest;
import com.offerpilot.cards.dto.CardGenerateRequest;
import com.offerpilot.cards.dto.CardTaskCreateRequest;
import com.offerpilot.cards.vo.CardDeckSummaryVO;
import com.offerpilot.cards.vo.CardStatsSummaryVO;
import com.offerpilot.cards.vo.KnowledgeCardTaskVO;
import com.offerpilot.cards.vo.TodayCardsTaskVO;
import java.util.List;

public interface KnowledgeCardService {
    KnowledgeCardTaskVO createTask(Long userId, CardTaskCreateRequest request);

    KnowledgeCardTaskVO generateDeck(Long userId, CardGenerateRequest request);

    KnowledgeCardTaskVO getActiveTask(Long userId);

    KnowledgeCardTaskVO getTask(Long userId, Long taskId);

    KnowledgeCardTaskVO startTask(Long userId, Long taskId);

    KnowledgeCardTaskVO rate(Long userId, Long taskId, CardRateRequest request);

    KnowledgeCardTaskVO restart(Long userId, Long taskId);

    TodayCardsTaskVO getTodayTask(Long userId);

    TodayCardsTaskVO reviewDeck(Long userId, Long deckId, CardRateRequest request);

    List<CardDeckSummaryVO> listDecks(Long userId);

    TodayCardsTaskVO activateDeck(Long userId, Long deckId);

    CardStatsSummaryVO getStats(Long userId);

    KnowledgeCardTaskVO getInterviewDeck(Long userId);

    KnowledgeCardTaskVO syncInterviewDeckBySession(Long userId, Long sessionId);

    TodayCardsTaskVO activateInterviewDeck(Long userId);

    void syncWrongDeck(Long userId);

    void invalidateByDocId(Long docId, String reason);
}
