package com.offerpilot.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.impl.AdaptiveServiceImpl;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.adaptive.vo.RecommendInterviewVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AdaptiveServiceImplTest {

    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewRecordMapper recordMapper;
    @Mock
    private RecordingReviewSessionMapper recordingReviewSessionMapper;
    @Mock
    private JobPrepSessionMapper jobPrepSessionMapper;
    @Mock
    private CopilotPrepSessionMapper copilotPrepSessionMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    private final OfferPilotProperties props = new OfferPilotProperties();

    @InjectMocks
    private AdaptiveServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        props.getAdaptive().setCacheTtlHours(24);
        props.getAdaptive().setWeakThreshold(50.0);

        Field objectMapperField = AdaptiveServiceImpl.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(service, objectMapper);

        Field propsField = AdaptiveServiceImpl.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(service, props);

        lenient().doReturn("{}").when(objectMapper).writeValueAsString(any());
        lenient().when(resumeFileMapper.selectList(any())).thenReturn(List.of());
        lenient().when(resumeProjectMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void getAbilityProfile_noSessions_returnsDefaults() {
        mockCacheMiss();
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(0.0, profile.getOverallAbility());
        assertEquals("easy", profile.getRecommendedDifficulty());
        assertEquals(0, profile.getRecordingReviewCount());
        assertTrue(profile.getCategoryAbilities().isEmpty());
        assertTrue(profile.getWeakCategories().isEmpty());
        assertNull(profile.getSuggestedFocus());
        assertEquals("insufficient", profile.getEvidenceStatus());
        assertTrue(profile.getEvidenceSummary().contains("足够训练证据"));
    }

    @Test
    void getAbilityProfile_withSessions_computesWeightedAbility() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "MySQL"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any())).thenReturn(List.of(session));

        InterviewRecord record1 = makeRecord(1L, 10L, new BigDecimal("80"));
        InterviewRecord record2 = makeRecord(1L, 20L, new BigDecimal("60"));
        when(recordMapper.selectList(any())).thenReturn(List.of(record1, record2));

        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(
                makeQuestion(10L, 100L),
                makeQuestion(20L, 200L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(2, profile.getCategoryAbilities().size());
        assertTrue(profile.getOverallAbility() > 0);
        assertTrue(profile.getWeakCategories().isEmpty());
        assertEquals("forming", profile.getEvidenceStatus());
        assertTrue(profile.getEvidenceSummary().contains("画像还在形成中"));
    }

    @Test
    void getAbilityProfile_lowScores_identifiesWeakCategories() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "JVM"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("30"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(3L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(List.of("JVM"), profile.getWeakCategories());
        assertEquals("JVM", profile.getSuggestedFocus());
    }

    @Test
    void getAbilityProfile_recentSessions_haveHigherWeight() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        when(sessionMapper.selectList(any())).thenReturn(List.of(
                makeSession(2L, LocalDateTime.now()),
                makeSession(1L, LocalDateTime.now().minusDays(30))));
        when(recordMapper.selectList(any())).thenReturn(List.of(
                makeRecord(1L, 10L, new BigDecimal("40")),
                makeRecord(2L, 10L, new BigDecimal("60"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        CategoryAbilityVO springAbility = profile.getCategoryAbilities().get(0);
        assertTrue(springAbility.getAbilityScore() > 50.0);
    }

    @Test
    void getAbilityProfile_recommendsDifficultyByAbility() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("20"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        assertEquals("easy", service.getAbilityProfile(1L).getRecommendedDifficulty());

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("50"))));
        assertEquals("medium", service.getAbilityProfile(2L).getRecommendedDifficulty());

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("80"))));
        assertEquals("hard", service.getAbilityProfile(3L).getRecommendedDifficulty());
    }

    @Test
    void getRecommendInterview_withWeakCategories_suggestsWeakest() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "JVM"), makeCategory(200L, "Spring"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(recordMapper.selectList(any())).thenReturn(List.of(
                makeRecord(1L, 10L, new BigDecimal("30")),
                makeRecord(1L, 20L, new BigDecimal("80"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(
                makeQuestion(10L, 100L),
                makeQuestion(20L, 200L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        RecommendInterviewVO result = service.getRecommendInterview(1L);

        assertEquals("JVM", result.getDirection());
        assertEquals(5, result.getQuestionCount());
        assertNotNull(result.getDifficulty());
    }

    @Test
    void getRecommendInterview_noSessions_usesAvailableCategory() {
        mockCacheMiss();
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        mockCategories(makeCategory(100L, "Java基础"));

        RecommendInterviewVO result = service.getRecommendInterview(1L);

        assertEquals("Java基础", result.getDirection());
        assertEquals(5, result.getQuestionCount());
    }

    @Test
    void getAbilityProfile_cacheHit_returnsCachedValue() throws Exception {
        AbilityProfileVO cached = AbilityProfileVO.builder()
                .overallAbility(75.0)
                .recommendedDifficulty("hard")
                .categoryAbilities(List.of())
                .weakCategories(List.of())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("adaptive:profile:1")).thenReturn("{cached}");
        when(objectMapper.readValue("{cached}", AbilityProfileVO.class)).thenReturn(cached);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(75.0, profile.getOverallAbility());
        verifyNoInteractions(sessionMapper);
    }

    @Test
    void refreshAbilityProfile_evictsCacheAndRecomputes() {
        mockCacheMiss();
        when(redisTemplate.delete("adaptive:profile:1")).thenReturn(true);
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        service.refreshAbilityProfile(1L);

        verify(redisTemplate).delete("adaptive:profile:1");
        verify(sessionMapper).selectList(any());
    }

    @Test
    void getAbilityProfile_scoresRoundedAndSorted() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "JVM"), makeCategory(300L, "MySQL"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(recordMapper.selectList(any())).thenReturn(List.of(
                makeRecord(1L, 10L, new BigDecimal("90")),
                makeRecord(1L, 20L, new BigDecimal("30")),
                makeRecord(1L, 30L, new BigDecimal("66.666"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(
                makeQuestion(10L, 100L),
                makeQuestion(20L, 200L),
                makeQuestion(30L, 300L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        List<CategoryAbilityVO> categories = profile.getCategoryAbilities();
        assertEquals("JVM", categories.get(0).getCategoryName());
        assertTrue(categories.get(0).getAbilityScore() <= categories.get(1).getAbilityScore());
        assertTrue(categories.get(1).getAbilityScore() <= categories.get(2).getAbilityScore());
        assertEquals("JVM", profile.getSuggestedFocus());
        assertTrue(String.valueOf(categories.get(2).getAbilityScore()).split("\\.").length <= 2
                || String.valueOf(categories.get(2).getAbilityScore()).split("\\.")[1].length() <= 2);
    }

    @Test
    void getAbilityProfile_scoreThresholdBoundary() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "MySQL"));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("50.01"))));
        assertTrue(service.getAbilityProfile(1L).getWeakCategories().isEmpty());

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("49"))));
        assertEquals(List.of("MySQL"), service.getAbilityProfile(2L).getWeakCategories());
    }

    @Test
    void getAbilityProfile_withRecordingReviewEvidence_updatesCountsAndFocus() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "Redis"));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of(
                makeRecordingReview(10L, "Java后端", "Spring工程师", "Spring 事务和 Bean 生命周期表达一般", new BigDecimal("42"))));
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());

        when(wrongQuestionMapper.selectCount(any())).thenReturn(1L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(1, profile.getRecordingReviewCount());
        assertEquals(1, profile.getCategoryAbilities().size());
        assertEquals("Spring", profile.getSuggestedFocus());
        assertEquals(List.of("Spring"), profile.getWeakCategories());

        CategoryAbilityVO springAbility = profile.getCategoryAbilities().stream()
                .filter(item -> "Spring".equals(item.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, springAbility.getInterviewCount());
        assertEquals(1, springAbility.getRecordingReviewCount());
        assertEquals(42.0, springAbility.getAbilityScore());
        assertEquals("forming", profile.getEvidenceStatus());
        assertTrue(profile.getEvidenceSummary().contains("画像还在形成中"));
    }

    @Test
    void getAbilityProfile_withEnoughEvidence_marksProfileReady() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"));
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(recordMapper.selectList(any())).thenReturn(List.of(
                makeRecord(1L, 10L, new BigDecimal("70")),
                makeRecord(1L, 10L, new BigDecimal("75"))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of(
                makeRecordingReview(10L, "Redis", "后端开发", "Redis 缓存一致性表达较稳", new BigDecimal("78"))));
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("ready", profile.getEvidenceStatus());
        assertTrue(profile.getEvidenceSummary().contains("长期画像已形成"));
    }

    @Test
    void getAbilityProfile_withPrepEvidence_only_marksProfileFormingAndCountsPrepSources() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"), makeCategory(200L, "Spring"));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of(
                makeJobPrep(21L, "Redis 后端工程师", "负责 Redis 缓存设计", "Redis 高并发缓存", new BigDecimal("76"))));
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of(
                makeCopilotPrep(31L, "Redis 实时面试", "Redis 缓存一致性追问", "Redis 追问风险与 live cue")));
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("forming", profile.getEvidenceStatus());
        assertEquals(1, profile.getCategoryAbilities().size());
        CategoryAbilityVO redisAbility = profile.getCategoryAbilities().get(0);
        assertEquals("Redis", redisAbility.getCategoryName());
        assertEquals(1, redisAbility.getJobPrepCount());
        assertEquals(1, redisAbility.getCopilotPrepCount());
        assertEquals(0, redisAbility.getInterviewCount());
        assertTrue(redisAbility.getAbilityScore() > 0);
    }

    @Test
    void getAbilityProfile_withApplicationFeedbackEvidence_countsApplicationSignals() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"), makeCategory(200L, "Kafka"));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of(
                makeApplication(41L, "后端开发", "Redis,Kafka", "Redis", "继续补 Redis 一致性")));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("forming", profile.getEvidenceStatus());
        assertEquals(2, profile.getCategoryAbilities().size());
        CategoryAbilityVO redisAbility = profile.getCategoryAbilities().stream()
                .filter(item -> "Redis".equals(item.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, redisAbility.getApplicationFeedbackCount());
        assertTrue(redisAbility.getAbilityScore() > 0);
    }

    @Test
    void getAbilityProfile_withResumeEvidence_countsResumeSignals() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"), makeCategory(200L, "Spring"));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(recordingReviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(copilotPrepSessionMapper.selectList(any())).thenReturn(List.of());
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(resumeFileMapper.selectList(any())).thenReturn(List.of(
                makeResume(51L, "Java 后端简历", "Redis,Spring", "Redis 高并发缓存项目", "Redis 一致性案例")));
        when(resumeProjectMapper.selectList(any())).thenReturn(List.of(
                makeResumeProject(51L, "Redis 缓存平台", "后端开发", "Redis,Spring Boot")));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("forming", profile.getEvidenceStatus());
        assertEquals(2, profile.getCategoryAbilities().size());
        CategoryAbilityVO redisAbility = profile.getCategoryAbilities().stream()
                .filter(item -> "Redis".equals(item.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, redisAbility.getResumeEvidenceCount());
        assertEquals(0, redisAbility.getInterviewCount());
        assertTrue(redisAbility.getAbilityScore() >= 40.0);
        assertTrue(profile.getEvidenceSummary().contains("画像还在形成中"));
    }

    private void mockCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @SuppressWarnings("unchecked")
    private void mockCategories(Category... categories) {
        var chain = mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(categoryService.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(List.of(categories));
    }

    private InterviewSession makeSession(Long id, LocalDateTime createTime) {
        InterviewSession session = new InterviewSession();
        session.setId(id);
        session.setUserId(1L);
        session.setStatus("finished");
        session.setCreateTime(createTime);
        return session;
    }

    private InterviewRecord makeRecord(Long sessionId, Long questionId, BigDecimal score) {
        InterviewRecord record = new InterviewRecord();
        record.setSessionId(sessionId);
        record.setUserId(1L);
        record.setQuestionId(questionId);
        record.setScore(score);
        return record;
    }

    private Question makeQuestion(Long id, Long categoryId) {
        Question question = new Question();
        question.setId(id);
        question.setCategoryId(categoryId);
        question.setTitle("Q" + id);
        return question;
    }

    private Category makeCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(1);
        return category;
    }

    private RecordingReviewSession makeRecordingReview(Long id, String direction, String jobRole, String transcript, BigDecimal score) {
        RecordingReviewSession review = new RecordingReviewSession();
        review.setId(id);
        review.setUserId(1L);
        review.setStatus("ready");
        review.setDirection(direction);
        review.setJobRole(jobRole);
        review.setTranscript(transcript);
        review.setSummary(transcript);
        review.setOverallScore(score);
        review.setCreateTime(LocalDateTime.now().minusDays(1));
        review.setUpdateTime(LocalDateTime.now());
        return review;
    }

    private JobPrepSession makeJobPrep(Long id, String jobTitle, String jdText, String summary, BigDecimal matchScore) {
        JobPrepSession session = new JobPrepSession();
        session.setId(id);
        session.setUserId(1L);
        session.setStatus("ready");
        session.setJobTitle(jobTitle);
        session.setJdText(jdText);
        session.setSummary(summary);
        session.setMatchedKeywordsJson("[\"Redis\"]");
        session.setFocusAreasJson("[\"Redis 缓存一致性\"]");
        session.setResumeTalkingPointsJson("[\"Redis 高并发缓存案例\"]");
        session.setMockQuestionsJson("[\"Redis 一致性怎么处理\"]");
        session.setNextActionsJson("[\"补 Redis 项目口径\"]");
        session.setMatchScore(matchScore);
        session.setCreateTime(LocalDateTime.now().minusDays(1));
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    private CopilotPrepSession makeCopilotPrep(Long id, String jobTitle, String jdText, String summary) {
        CopilotPrepSession session = new CopilotPrepSession();
        session.setId(id);
        session.setUserId(1L);
        session.setStatus("ready");
        session.setJobTitle(jobTitle);
        session.setJdText(jdText);
        session.setSummary(summary);
        session.setOpeningBriefJson("[\"Redis 项目开场\"]");
        session.setKeyRisksJson("[\"Redis 一致性追问\"]");
        session.setLiveCuesJson("[\"先讲结论再讲缓存取舍\"]");
        session.setFollowUpQuestionsJson("[\"Redis 雪崩如何处理\"]");
        session.setNextActionsJson("[\"会前复述 Redis 案例\"]");
        session.setCreateTime(LocalDateTime.now().minusDays(1));
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    private JobApplication makeApplication(Long id, String jobTitle, String jdKeywords, String missingKeywords, String reviewSuggestion) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setUserId(1L);
        application.setCompany("字节跳动");
        application.setJobTitle(jobTitle);
        application.setStatus("interview");
        application.setMatchScore(new BigDecimal("78"));
        application.setJdKeywords(jdKeywords);
        application.setMissingKeywords(missingKeywords);
        application.setReviewSuggestion(reviewSuggestion);
        application.setAnalysisSummary("Redis 和 Kafka 都会被重点追问");
        application.setNextStepSuggestion("下轮重点补 Redis");
        application.setCreateTime(LocalDateTime.now().minusDays(1));
        application.setUpdateTime(LocalDateTime.now());
        return application;
    }

    private ResumeFile makeResume(Long id, String title, String skills, String summary, String interviewResumeText) {
        ResumeFile resume = new ResumeFile();
        resume.setId(id);
        resume.setUserId(1L);
        resume.setTitle(title);
        resume.setParseStatus("parsed");
        resume.setSkills(skills);
        resume.setSummary(summary);
        resume.setSelfIntro("我负责 Redis 与 Spring 服务治理");
        resume.setInterviewResumeText(interviewResumeText);
        resume.setCreateTime(LocalDateTime.now().minusDays(2));
        resume.setUpdateTime(LocalDateTime.now().minusDays(1));
        return resume;
    }

    private ResumeProject makeResumeProject(Long resumeFileId, String projectName, String roleName, String techStack) {
        ResumeProject project = new ResumeProject();
        project.setResumeFileId(resumeFileId);
        project.setUserId(1L);
        project.setProjectName(projectName);
        project.setRoleName(roleName);
        project.setTechStack(techStack);
        project.setResponsibility("负责 Redis 缓存一致性与 Spring 服务接入");
        project.setAchievement("完成高并发缓存链路优化");
        project.setProjectSummary("聚焦 Redis 缓存一致性和 Spring 工程治理");
        project.setRiskHints("需要补 Redis 雪崩追问");
        project.setFollowUpQuestionsJson("[\"Redis 一致性怎么做\"]");
        return project;
    }
}
