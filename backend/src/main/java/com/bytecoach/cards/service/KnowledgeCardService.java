package com.bytecoach.cards.service;

import com.bytecoach.cards.dto.CardRateRequest;
import com.bytecoach.cards.dto.CardGenerateRequest;
import com.bytecoach.cards.dto.CardTaskCreateRequest;
import com.bytecoach.cards.vo.CardDeckSummaryVO;
import com.bytecoach.cards.vo.CardStatsSummaryVO;
import com.bytecoach.cards.vo.KnowledgeCardTaskVO;
import com.bytecoach.cards.vo.TodayCardsTaskVO;
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

    void syncWrongDeck(Long userId);

    void invalidateByDocId(Long docId, String reason);
}
