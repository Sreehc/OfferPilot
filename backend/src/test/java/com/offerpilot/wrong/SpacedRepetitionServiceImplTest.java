package com.offerpilot.wrong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.dto.ReviewRateRequest;
import com.offerpilot.wrong.dto.ReviewTodayVO;
import com.offerpilot.wrong.entity.ReviewLog;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.offerpilot.wrong.service.impl.SpacedRepetitionServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "test"),
                ReviewLog.class);
    }

    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private ReviewLogMapper reviewLogMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private SpacedRepetitionServiceImpl service;

    @Test
    void getTodayReviews_returnsDueItemsSortedByOverdue() {
        WrongQuestion overdue = makeWrongQuestion(1L, 10L, LocalDate.now().minusDays(3));
        WrongQuestion today = makeWrongQuestion(2L, 20L, LocalDate.now());
        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(today, overdue));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, "Q1"), makeQuestion(20L, "Q2")));
        when(reviewLogMapper.selectCount(any())).thenReturn(0L);
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());

        ReviewTodayVO result = service.getTodayReviews(1L, "wrong_card");

        assertEquals(2, result.getItems().size());
        assertEquals(3L, result.getItems().get(0).getOverdueDays());
        assertEquals(0L, result.getItems().get(1).getOverdueDays());
    }

    @Test
    void getTodayReviews_emptyWhenNoneDue() {
        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of());
        when(reviewLogMapper.selectCount(any())).thenReturn(0L);
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());

        ReviewTodayVO result = service.getTodayReviews(1L, "wrong_card");

        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotalPending());
    }

    @Test
    void rate_rating1_again_resetsIntervalToOne() {
        WrongQuestion wrongQuestion = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wrongQuestion);
        when(wrongQuestionMapper.updateById(any(WrongQuestion.class))).thenReturn(1);
        when(reviewLogMapper.insert(any(ReviewLog.class))).thenReturn(1);
        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(wrongQuestion));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, "Q1")));
        when(reviewLogMapper.selectCount(any())).thenReturn(0L);
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());

        ReviewRateRequest request = new ReviewRateRequest();
        request.setRating(1);
        request.setContentType("wrong_card");
        service.rate(1L, 1L, request);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();
        assertEquals(1, updated.getIntervalDays());
        assertEquals(0, updated.getStreak());
        assertTrue(updated.getEaseFactor().compareTo(new BigDecimal("2.30")) >= 0);
    }

    @Test
    void rate_rating3_good_thirdReview_intervalByEF() {
        WrongQuestion wrongQuestion = makeWrongQuestionWithEF(1L, 2.50, 6, 2);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wrongQuestion);
        when(wrongQuestionMapper.updateById(any(WrongQuestion.class))).thenReturn(1);
        when(reviewLogMapper.insert(any(ReviewLog.class))).thenReturn(1);
        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(wrongQuestion));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, "Q1")));
        when(reviewLogMapper.selectCount(any())).thenReturn(0L);
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());

        ReviewRateRequest request = new ReviewRateRequest();
        request.setRating(3);
        request.setContentType("wrong_card");
        service.rate(1L, 1L, request);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();
        assertEquals(15, updated.getIntervalDays());
        assertEquals(3, updated.getStreak());
    }

    @Test
    void rate_wrongQuestion_notOwned_throws() {
        WrongQuestion wrongQuestion = makeWrongQuestion(1L, 10L, LocalDate.now());
        wrongQuestion.setUserId(99L);
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wrongQuestion);

        ReviewRateRequest request = new ReviewRateRequest();
        request.setRating(3);

        assertThrows(BusinessException.class, () -> service.rate(1L, 1L, request));
    }

    @Test
    void computeMasteryLevel_usesSchedulingRules() {
        assertEquals("mastered", service.computeMasteryLevel(new BigDecimal("2.30"), 3));
        assertEquals("reviewing", service.computeMasteryLevel(new BigDecimal("1.80"), 0));
        assertEquals("not_started", service.computeMasteryLevel(new BigDecimal("1.50"), 0));
    }

    private WrongQuestion makeWrongQuestion(Long id, Long questionId, LocalDate nextReview) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setId(id);
        wrongQuestion.setUserId(1L);
        wrongQuestion.setQuestionId(questionId);
        wrongQuestion.setNextReviewDate(nextReview);
        wrongQuestion.setEaseFactor(new BigDecimal("2.50"));
        wrongQuestion.setIntervalDays(1);
        wrongQuestion.setStreak(0);
        wrongQuestion.setReviewCount(0);
        return wrongQuestion;
    }

    private WrongQuestion makeWrongQuestionWithEF(Long id, double ef, int interval, int streak) {
        WrongQuestion wrongQuestion = makeWrongQuestion(id, 10L, LocalDate.now());
        wrongQuestion.setEaseFactor(BigDecimal.valueOf(ef));
        wrongQuestion.setIntervalDays(interval);
        wrongQuestion.setStreak(streak);
        return wrongQuestion;
    }

    private Question makeQuestion(Long id, String title) {
        Question question = new Question();
        question.setId(id);
        question.setTitle(title);
        return question;
    }
}
