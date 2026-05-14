package com.offerpilot.interview;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.service.impl.InterviewServiceImpl;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewRecordMapper recordMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AiOrchestratorService aiOrchestratorService;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Test
    void start_picksQuestionsAndCreatesSession() {
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDirection("Java Backend");
        request.setQuestionCount(2);

        Question q1 = new Question();
        q1.setId(10L);
        q1.setTitle("What is Spring IOC?");
        Question q2 = new Question();
        q2.setId(20L);
        q2.setTitle("Explain AOP");

        // Mock CategoryService lambdaQuery chain - return null (no category match)
        when(categoryService.lambdaQuery()).thenReturn(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                .eq(Category::getName, "Java Backend").eq(Category::getType, "interview"));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(q1, q2));
        when(sessionMapper.insert(any(InterviewSession.class))).thenAnswer(inv -> {
            InterviewSession s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });
        when(recordMapper.insert(any(InterviewRecord.class))).thenReturn(1);

        InterviewCurrentQuestionVO result = interviewService.start(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getSessionId());
        assertEquals(1, result.getCurrentIndex());
        assertEquals(2, result.getQuestionCount());
        assertNotNull(result.getQuestionId());
        verify(sessionMapper).insert(any(InterviewSession.class));
        verify(recordMapper, times(2)).insert(any(InterviewRecord.class));
    }

    @Test
    void start_noQuestions_throws() {
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDirection("Unknown");
        request.setQuestionCount(3);

        when(categoryService.lambdaQuery()).thenReturn(new LambdaQueryWrapper<Category>());
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> interviewService.start(1L, request));
    }

    @Test
    void answer_belowThreshold_addsToWrongBook() {
        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setSessionId(1L);
        request.setQuestionId(10L);
        request.setAnswer("I don't know");

        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setStatus("in_progress");
        session.setQuestionCount(2);
        session.setCurrentIndex(1);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        InterviewRecord record = new InterviewRecord();
        record.setId(100L);
        record.setSessionId(1L);
        record.setQuestionId(10L);
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question question = new Question();
        question.setId(10L);
        question.setTitle("What is Spring IOC?");
        question.setStandardAnswer("Spring IOC is...");
        when(questionMapper.selectById(10L)).thenReturn(question);

        InterviewAnswerVO aiResult = InterviewAnswerVO.builder()
                .score(new BigDecimal("40"))
                .comment("Poor answer")
                .standardAnswer("Spring IOC is...")
                .followUp("Can you explain DI?")
                .build();
        when(aiOrchestratorService.scoreInterviewAnswer(any())).thenReturn(aiResult);
        when(wrongQuestionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(wrongQuestionMapper.insert(any(WrongQuestion.class))).thenReturn(1);

        InterviewAnswerVO result = interviewService.answer(1L, request);

        assertTrue(result.getAddedToWrongBook());
        assertTrue(result.getHasNextQuestion());
        assertEquals(new BigDecimal("40"), result.getScore());
        verify(wrongQuestionMapper).insert(any(WrongQuestion.class));
    }

    @Test
    void answer_aboveThreshold_noWrongBook() {
        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setSessionId(1L);
        request.setQuestionId(10L);
        request.setAnswer("Great answer about IOC");

        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setStatus("in_progress");
        session.setQuestionCount(1);
        session.setCurrentIndex(1);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        InterviewRecord record = new InterviewRecord();
        record.setId(100L);
        record.setSessionId(1L);
        record.setQuestionId(10L);
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(questionMapper.selectById(10L)).thenReturn(new Question());

        InterviewAnswerVO aiResult = InterviewAnswerVO.builder()
                .score(new BigDecimal("85"))
                .comment("Good answer")
                .standardAnswer("...")
                .followUp("...")
                .build();
        when(aiOrchestratorService.scoreInterviewAnswer(any())).thenReturn(aiResult);

        InterviewAnswerVO result = interviewService.answer(1L, request);

        assertFalse(result.getAddedToWrongBook());
        assertFalse(result.getHasNextQuestion());
        verify(wrongQuestionMapper, never()).insert(any());
    }

    @Test
    void detail_returnsSessionWithRecords() {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setDirection("Java Backend");
        session.setStatus("finished");
        session.setTotalScore(new BigDecimal("75"));
        session.setQuestionCount(1);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        InterviewRecord record = new InterviewRecord();
        record.setId(100L);
        record.setSessionId(1L);
        record.setQuestionId(10L);
        record.setScore(new BigDecimal("75"));
        record.setComment("Good");
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question question = new Question();
        question.setId(10L);
        question.setTitle("What is Spring IOC?");
        question.setStandardAnswer("IOC explanation");
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(question));

        InterviewDetailVO result = interviewService.detail(1L, 1L);

        assertEquals(1L, result.getSessionId());
        assertEquals("Java Backend", result.getDirection());
        assertEquals(new BigDecimal("75"), result.getTotalScore());
        assertEquals(1, result.getRecords().size());
        assertEquals("What is Spring IOC?", result.getRecords().get(0).getQuestionTitle());
    }
}
