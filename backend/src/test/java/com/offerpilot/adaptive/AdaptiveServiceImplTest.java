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
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.lang.reflect.Field;
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
    }

    @Test
    void getAbilityProfile_noSessions_returnsDefaults() {
        mockCacheMiss();
        when(sessionMapper.selectList(any())).thenReturn(List.of());

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(0.0, profile.getOverallAbility());
        assertEquals("easy", profile.getRecommendedDifficulty());
        assertTrue(profile.getCategoryAbilities().isEmpty());
        assertTrue(profile.getWeakCategories().isEmpty());
        assertNull(profile.getSuggestedFocus());
    }

    @Test
    void getAbilityProfile_withSessions_computesWeightedAbility() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "MySQL"));

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
    }

    @Test
    void getAbilityProfile_lowScores_identifiesWeakCategories() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "JVM"));

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

        service.refreshAbilityProfile(1L);

        verify(redisTemplate).delete("adaptive:profile:1");
        verify(sessionMapper).selectList(any());
    }

    @Test
    void getAbilityProfile_scoresRoundedAndSorted() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "JVM"), makeCategory(300L, "MySQL"));
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
        when(sessionMapper.selectList(any())).thenReturn(List.of(makeSession(1L, LocalDateTime.now().minusDays(1))));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(makeQuestion(10L, 100L)));
        when(wrongQuestionMapper.selectCount(any())).thenReturn(0L);

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("50.01"))));
        assertTrue(service.getAbilityProfile(1L).getWeakCategories().isEmpty());

        when(recordMapper.selectList(any())).thenReturn(List.of(makeRecord(1L, 10L, new BigDecimal("49"))));
        assertEquals(List.of("MySQL"), service.getAbilityProfile(2L).getWeakCategories());
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
}
