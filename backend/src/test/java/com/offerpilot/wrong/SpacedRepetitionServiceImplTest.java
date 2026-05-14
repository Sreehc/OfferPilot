package com.offerpilot.wrong;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.dto.ReviewRateRequest;
import com.offerpilot.wrong.dto.ReviewTodayVO;
import com.offerpilot.wrong.entity.ReviewLog;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.offerpilot.wrong.service.impl.SpacedRepetitionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceImplTest {

    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private ReviewLogMapper reviewLogMapper;
    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private SpacedRepetitionServiceImpl service;

    // ── getTodayReviews ──────────────────────────────

    @Test
    void getTodayReviews_returnsDueItemsSortedByOverdue() {
        WrongQuestion wq1 = makeWrongQuestion(1L, 10L, LocalDate.now().minusDays(3));
        WrongQuestion wq2 = makeWrongQuestion(2L, 20L, LocalDate.now());
        when(wrongQuestionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(wq2, wq1)); // DB returns in some order

        Question q1 = new Question(); q1.setId(10L); q1.setTitle("Q1");
        Question q2 = new Question(); q2.setId(20L); q2.setTitle("Q2");
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q1, q2));

        List<ReviewTodayVO> result = service.getTodayReviews(1L);

        assertEquals(2, result.size());
        // Most overdue first (3 days overdue > 0 days)
        assertEquals(3L, result.get(0).getOverdueDays());
        assertEquals(0L, result.get(1).getOverdueDays());
    }

    @Test
    void getTodayReviews_emptyWhenNoneDue() {
        when(wrongQuestionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<ReviewTodayVO> result = service.getTodayReviews(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTodayReviewCount_returnsCorrectCount() {
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(5L);

        int count = service.getTodayReviewCount(1L);
        assertEquals(5, count);
    }

    // ── rate: SM-2 algorithm ─────────────────────────

    @Test
    void rate_rating1_again_resetsIntervalToOne() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(1);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        assertEquals(1, updated.getIntervalDays());
        assertEquals(0, updated.getStreak());
        // EF should decrease by 0.20
        assertTrue(updated.getEaseFactor().compareTo(new BigDecimal("2.30")) >= 0);
    }

    @Test
    void rate_rating2_hard_resetsIntervalToOne() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(2);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        assertEquals(1, updated.getIntervalDays());
        assertEquals(0, updated.getStreak());
    }

    @Test
    void rate_rating3_good_firstReview_intervalOne() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 0, 0);
        wq.setStreak(0);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(3);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        assertEquals(1, updated.getIntervalDays());
        assertEquals(1, updated.getStreak());
    }

    @Test
    void rate_rating3_good_secondReview_intervalSix() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 1, 1);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(3);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        assertEquals(6, updated.getIntervalDays());
        assertEquals(2, updated.getStreak());
    }

    @Test
    void rate_rating3_good_thirdReview_intervalByEF() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(3);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        // interval = round(6 * 2.50) = 15
        assertEquals(15, updated.getIntervalDays());
        assertEquals(3, updated.getStreak());
    }

    @Test
    void rate_rating4_easy_increasesEF() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(4);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        // EF should increase for Easy (quality=5)
        assertTrue(updated.getEaseFactor().compareTo(new BigDecimal("2.50")) > 0);
        assertEquals(3, updated.getStreak());
    }

    @Test
    void rate_efNeverGoesBelowMin() {
        WrongQuestion wq = makeWrongQuestionWithEF(1L, 1.35, 1, 1);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);
        when(wrongQuestionMapper.updateById(any())).thenReturn(1);
        when(reviewLogMapper.insert(any())).thenReturn(1);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(1);
        service.rate(1L, 1L, req);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();

        // EF should not go below 1.30
        assertTrue(updated.getEaseFactor().compareTo(new BigDecimal("1.30")) >= 0);
    }

    @Test
    void rate_wrongQuestion_notOwned_throws() {
        WrongQuestion wq = makeWrongQuestion(1L, 10L, LocalDate.now());
        wq.setUserId(99L); // different user
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);

        ReviewRateRequest req = new ReviewRateRequest();
        req.setRating(3);

        assertThrows(BusinessException.class, () -> service.rate(1L, 1L, req));
    }

    // ── Mastery level computation ────────────────────

    @Test
    void computeMasteryLevel_mastered() {
        assertEquals("mastered", SpacedRepetitionServiceImpl.computeMasteryLevel(
                new BigDecimal("2.30"), 3));
        assertEquals("mastered", SpacedRepetitionServiceImpl.computeMasteryLevel(
                new BigDecimal("2.50"), 5));
    }

    @Test
    void computeMasteryLevel_reviewing() {
        assertEquals("reviewing", SpacedRepetitionServiceImpl.computeMasteryLevel(
                new BigDecimal("1.80"), 0));
        assertEquals("reviewing", SpacedRepetitionServiceImpl.computeMasteryLevel(
                new BigDecimal("2.50"), 1));
    }

    @Test
    void computeMasteryLevel_notStarted() {
        assertEquals("not_started", SpacedRepetitionServiceImpl.computeMasteryLevel(
                new BigDecimal("1.50"), 0));
    }

    @Test
    void computeMasteryLevel_nullValues() {
        // null easeFactor defaults to 2.50, null streak defaults to 0
        assertEquals("reviewing", SpacedRepetitionServiceImpl.computeMasteryLevel(null, null));
    }

    // ── getTotalReviewCount & getReviewStreak ────────

    @Test
    void getTotalReviewCount_returnsCount() {
        when(reviewLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(42L);
        assertEquals(42, service.getTotalReviewCount(1L));
    }

    @Test
    void getReviewStreak_noReviews_returnsZero() {
        when(reviewLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertEquals(0, service.getReviewStreak(1L));
    }

    // ── Helpers ──────────────────────────────────────

    private WrongQuestion makeWrongQuestion(Long id, Long questionId, LocalDate nextReview) {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(id);
        wq.setUserId(1L);
        wq.setQuestionId(questionId);
        wq.setNextReviewDate(nextReview);
        wq.setEaseFactor(new BigDecimal("2.50"));
        wq.setIntervalDays(1);
        wq.setStreak(0);
        wq.setReviewCount(0);
        return wq;
    }

    private WrongQuestion makeWrongQuestionWithEF(Long id, double ef, int interval, int streak) {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(id);
        wq.setUserId(1L);
        wq.setQuestionId(10L);
        wq.setNextReviewDate(LocalDate.now());
        wq.setEaseFactor(BigDecimal.valueOf(ef));
        wq.setIntervalDays(interval);
        wq.setStreak(streak);
        wq.setReviewCount(0);
        return wq;
    }
}
