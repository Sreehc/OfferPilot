package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.entity.VoiceRecord;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.mapper.VoiceRecordMapper;
import com.offerpilot.interview.service.impl.InterviewServiceImpl;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.notification.service.NotificationService;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewRecordMapper recordMapper;
    @Mock
    private VoiceRecordMapper voiceRecordMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AiOrchestratorService aiOrchestratorService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TrainingSignalService trainingSignalService;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;

    private final OfferPilotProperties props = new OfferPilotProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @BeforeEach
    void setUp() throws Exception {
        props.getInterview().setWrongThreshold(60);
        var chain = org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        lenient().when(categoryService.lambdaQuery()).thenReturn(chain);
        lenient().when(chain.eq(any(), any())).thenReturn(chain);
        lenient().when(chain.one()).thenReturn(null);

        Field propsField = InterviewServiceImpl.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(interviewService, props);

        Field objectMapperField = InterviewServiceImpl.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(interviewService, objectMapper);

        Field selfField = InterviewServiceImpl.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(interviewService, interviewService);
    }

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
        when(questionMapper.selectList(any())).thenReturn(List.of(q1, q2));
        when(sessionMapper.insert(any(InterviewSession.class))).thenAnswer(invocation -> {
            InterviewSession session = invocation.getArgument(0);
            session.setId(1L);
            return 1;
        });
        when(recordMapper.insert(any(InterviewRecord.class))).thenReturn(1);

        InterviewCurrentQuestionVO result = interviewService.start(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getSessionId());
        assertEquals(1, result.getCurrentIndex());
        assertEquals(2, result.getQuestionCount());
        assertTrue(List.of(10L, 20L).contains(result.getQuestionId()));
        verify(recordMapper, org.mockito.Mockito.times(2)).insert(any(InterviewRecord.class));
    }

    @Test
    void start_noQuestions_throws() {
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDirection("Unknown");
        request.setQuestionCount(3);

        when(questionMapper.selectList(any())).thenReturn(List.of(), List.of());

        assertThrows(BusinessException.class, () -> interviewService.start(1L, request));
    }

    @Test
    void answer_belowThreshold_addsToWrongBook() {
        InterviewSession session = makeSession("in_progress", 2, 1);
        when(sessionMapper.selectById(1L)).thenReturn(session);
        InterviewRecord record = makeRecord(100L, 1L, 10L);
        when(recordMapper.selectList(any())).thenReturn(List.of(record));

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
        when(wrongQuestionMapper.selectOne(any())).thenReturn(null);
        when(wrongQuestionMapper.insert(any(WrongQuestion.class))).thenReturn(1);
        when(recordMapper.updateById(any(InterviewRecord.class))).thenReturn(1);
        when(sessionMapper.updateById(any(InterviewSession.class))).thenReturn(1);

        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setSessionId(1L);
        request.setQuestionId(10L);
        request.setAnswer("I don't know");

        InterviewAnswerVO result = interviewService.answer(1L, request);

        assertTrue(result.getAddedToWrongBook());
        assertTrue(result.getHasNextQuestion());
        assertEquals(new BigDecimal("40"), result.getScore());
        verify(wrongQuestionMapper).insert(any(WrongQuestion.class));
    }

    @Test
    void answer_aboveThreshold_finishesSessionWithoutWrongBook() {
        InterviewSession session = makeSession("in_progress", 1, 1);
        when(sessionMapper.selectById(1L)).thenReturn(session);
        InterviewRecord record = makeRecord(100L, 1L, 10L);
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(questionMapper.selectById(10L)).thenReturn(new Question());

        InterviewAnswerVO aiResult = InterviewAnswerVO.builder()
                .score(new BigDecimal("85"))
                .comment("Good answer")
                .standardAnswer("...")
                .followUp("...")
                .build();
        when(aiOrchestratorService.scoreInterviewAnswer(any())).thenReturn(aiResult);
        when(recordMapper.updateById(any(InterviewRecord.class))).thenReturn(1);
        when(sessionMapper.updateById(any(InterviewSession.class))).thenReturn(1);

        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setSessionId(1L);
        request.setQuestionId(10L);
        request.setAnswer("Great answer about IOC");

        InterviewAnswerVO result = interviewService.answer(1L, request);

        assertFalse(result.getAddedToWrongBook());
        assertFalse(result.getHasNextQuestion());
        verify(wrongQuestionMapper, never()).insert(any(WrongQuestion.class));
        verify(notificationService).send(any(), any(), any(), any(), any());
        verify(trainingSignalService).handleEvidenceUpdate(1L);
    }

    @Test
    void detail_returnsSessionWithRecords() {
        InterviewSession session = makeSession("finished", 1, 1);
        session.setDirection("Java Backend");
        session.setTotalScore(new BigDecimal("75"));
        when(sessionMapper.selectById(1L)).thenReturn(session);

        InterviewRecord record = makeRecord(100L, 1L, 10L);
        record.setScore(new BigDecimal("75"));
        record.setComment("Good");
        when(recordMapper.selectList(any())).thenReturn(List.of(record));

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

    private InterviewSession makeSession(String status, int questionCount, int currentIndex) {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setStatus(status);
        session.setQuestionCount(questionCount);
        session.setCurrentIndex(currentIndex);
        session.setDirection("Java Backend");
        return session;
    }

    private InterviewRecord makeRecord(Long id, Long sessionId, Long questionId) {
        InterviewRecord record = new InterviewRecord();
        record.setId(id);
        record.setSessionId(sessionId);
        record.setQuestionId(questionId);
        return record;
    }
}
