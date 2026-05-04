package com.bytecoach.adaptive;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.adaptive.service.impl.AdaptiveServiceImpl;
import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.adaptive.vo.CategoryAbilityVO;
import com.bytecoach.adaptive.vo.RecommendInterviewVO;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdaptiveServiceImpl service;

    // ── getAbilityProfile: empty sessions ──────────────

    @Test
    void getAbilityProfile_noSessions_returnsDefaults() {
        mockCacheMiss();
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(0.0, profile.getOverallAbility());
        assertEquals("easy", profile.getRecommendedDifficulty());
        assertTrue(profile.getCategoryAbilities().isEmpty());
        assertTrue(profile.getWeakCategories().isEmpty());
        assertNull(profile.getSuggestedFocus());
    }

    // ── getAbilityProfile: with data ───────────────────

    @Test
    void getAbilityProfile_withSessions_computesWeightedAbility() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"), makeCategory(200L, "MySQL"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record1 = makeRecord(1L, 10L, new BigDecimal("80"));
        InterviewRecord record2 = makeRecord(1L, 20L, new BigDecimal("60"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record1, record2));

        Question q1 = makeQuestion(10L, 100L);
        Question q2 = makeQuestion(20L, 200L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q1, q2));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(2, profile.getCategoryAbilities().size());
        assertTrue(profile.getOverallAbility() > 0);
        assertTrue(profile.getWeakCategories().isEmpty());
    }

    @Test
    void getAbilityProfile_lowScores_identifiesWeakCategories() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "JVM"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("30"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(1, profile.getWeakCategories().size());
        assertEquals("JVM", profile.getWeakCategories().get(0));
        assertEquals("JVM", profile.getSuggestedFocus());
    }

    // ── Recency weighting ──────────────────────────────

    @Test
    void getAbilityProfile_recentSessions_haveHigherWeight() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"));

        InterviewSession oldSession = makeSession(1L, LocalDateTime.now().minusDays(30));
        InterviewSession recentSession = makeSession(2L, LocalDateTime.now());
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(recentSession, oldSession));

        InterviewRecord oldRecord = makeRecord(1L, 10L, new BigDecimal("40"));
        InterviewRecord recentRecord = makeRecord(2L, 10L, new BigDecimal("60"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(oldRecord, recentRecord));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        // Simple average = 50, but recent session (~3x weight) should pull it above 50
        CategoryAbilityVO springAbility = profile.getCategoryAbilities().get(0);
        assertTrue(springAbility.getAbilityScore() > 50,
                "Recent session should pull weighted average above 50, got: " + springAbility.getAbilityScore());
    }

    // ── Difficulty mapping (ZPD) ───────────────────────

    @Test
    void getAbilityProfile_lowAbility_recommendsEasy() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Redis"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("20"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("easy", profile.getRecommendedDifficulty());
    }

    @Test
    void getAbilityProfile_mediumAbility_recommendsMedium() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "MySQL"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("50"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("medium", profile.getRecommendedDifficulty());
    }

    @Test
    void getAbilityProfile_highAbility_recommendsHard() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "Spring"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("80"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals("hard", profile.getRecommendedDifficulty());
    }

    // ── getRecommendInterview ──────────────────────────

    @Test
    void getRecommendInterview_withWeakCategories_suggestsWeakest() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "JVM"), makeCategory(200L, "Spring"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord r1 = makeRecord(1L, 10L, new BigDecimal("30"));
        InterviewRecord r2 = makeRecord(1L, 20L, new BigDecimal("80"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2));

        Question q1 = makeQuestion(10L, 100L);
        Question q2 = makeQuestion(20L, 200L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q1, q2));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        RecommendInterviewVO result = service.getRecommendInterview(1L);

        assertEquals("JVM", result.getDirection());
        assertEquals(5, result.getQuestionCount());
        assertNotNull(result.getDifficulty());
    }

    @Test
    void getRecommendInterview_noSessions_suggestsCategory() {
        mockCacheMiss();

        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Category cat = makeCategory(100L, "Java基础");
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<Category> catWrapper = mock(LambdaQueryWrapper.class);
        when(categoryService.lambdaQuery()).thenReturn(catWrapper);
        when(catWrapper.eq(any(), any())).thenReturn(catWrapper);
        when(catWrapper.list()).thenReturn(List.of(cat));

        RecommendInterviewVO result = service.getRecommendInterview(1L);

        assertNotNull(result.getDirection());
        assertEquals(5, result.getQuestionCount());
    }

    // ── Caching ────────────────────────────────────────

    @Test
    void getAbilityProfile_cacheHit_returnsCachedValue() throws Exception {
        AbilityProfileVO cached = AbilityProfileVO.builder()
                .overallAbility(75.0)
                .recommendedDifficulty("hard")
                .categoryAbilities(List.of())
                .weakCategories(List.of())
                .suggestedFocus(null)
                .build();

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("adaptive:profile:1")).thenReturn(objectMapper.writeValueAsString(cached));

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(75.0, profile.getOverallAbility());
        verifyNoInteractions(sessionMapper);
    }

    // ── refreshAbilityProfile ──────────────────────────

    @Test
    void refreshAbilityProfile_evictsCacheAndRecomputes() {
        when(redisTemplate.delete("adaptive:profile:1")).thenReturn(true);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.refreshAbilityProfile(1L);

        verify(redisTemplate).delete("adaptive:profile:1");
        verify(sessionMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ── Score rounding ─────────────────────────────────

    @Test
    void getAbilityProfile_scoresRoundedToTwoDecimals() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "并发"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("66.666"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        CategoryAbilityVO ability = profile.getCategoryAbilities().get(0);
        String scoreStr = String.valueOf(ability.getAbilityScore());
        int decimalPart = scoreStr.contains(".") ? scoreStr.split("\\.")[1].length() : 0;
        assertTrue(decimalPart <= 2, "Score should be rounded to 2 decimals: " + scoreStr);
    }

    // ── Category sorting ───────────────────────────────

    @Test
    void getAbilityProfile_categoriesSortedByAbilityAscending() {
        mockCacheMiss();
        mockCategories(
                makeCategory(100L, "Spring"),
                makeCategory(200L, "JVM"),
                makeCategory(300L, "MySQL"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord r1 = makeRecord(1L, 10L, new BigDecimal("90")); // Spring
        InterviewRecord r2 = makeRecord(1L, 20L, new BigDecimal("30")); // JVM
        InterviewRecord r3 = makeRecord(1L, 30L, new BigDecimal("60")); // MySQL
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2, r3));

        Question q1 = makeQuestion(10L, 100L);
        Question q2 = makeQuestion(20L, 200L);
        Question q3 = makeQuestion(30L, 300L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q1, q2, q3));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        List<CategoryAbilityVO> cats = profile.getCategoryAbilities();
        assertEquals(3, cats.size());
        assertTrue(cats.get(0).getAbilityScore() <= cats.get(1).getAbilityScore());
        assertTrue(cats.get(1).getAbilityScore() <= cats.get(2).getAbilityScore());
        assertEquals("JVM", cats.get(0).getCategoryName());
        assertEquals("JVM", profile.getSuggestedFocus());
    }

    // ── Category boundary thresholds ───────────────────

    @Test
    void getAbilityProfile_scoreExactly50_isNotWeak() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "MySQL"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("50"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        // Score exactly 50 — threshold is < 50, so NOT weak
        assertTrue(profile.getWeakCategories().isEmpty());
    }

    @Test
    void getAbilityProfile_score49_isWeak() {
        mockCacheMiss();
        mockCategories(makeCategory(100L, "MySQL"));

        InterviewSession session = makeSession(1L, LocalDateTime.now().minusDays(1));
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

        InterviewRecord record = makeRecord(1L, 10L, new BigDecimal("49"));
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        Question q = makeQuestion(10L, 100L);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q));
        when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0);

        AbilityProfileVO profile = service.getAbilityProfile(1L);

        assertEquals(1, profile.getWeakCategories().size());
        assertEquals("MySQL", profile.getWeakCategories().get(0));
    }

    // ── Helpers ────────────────────────────────────────

    private void mockCacheMiss() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
    }

    /**
     * Mock the categoryService.lambdaQuery().eq(...).list() fluent chain.
     */
    @SuppressWarnings("unchecked")
    private void mockCategories(Category... categories) {
        LambdaQueryWrapper<Category> wrapper = mock(LambdaQueryWrapper.class);
        when(categoryService.lambdaQuery()).thenReturn(wrapper);
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(wrapper.list()).thenReturn(List.of(categories));
    }

    private InterviewSession makeSession(Long id, LocalDateTime createTime) {
        InterviewSession s = new InterviewSession();
        s.setId(id);
        s.setUserId(1L);
        s.setStatus("finished");
        s.setCreateTime(createTime);
        return s;
    }

    private InterviewRecord makeRecord(Long sessionId, Long questionId, BigDecimal score) {
        InterviewRecord r = new InterviewRecord();
        r.setSessionId(sessionId);
        r.setUserId(1L);
        r.setQuestionId(questionId);
        r.setScore(score);
        return r;
    }

    private Question makeQuestion(Long id, Long categoryId) {
        Question q = new Question();
        q.setId(id);
        q.setCategoryId(categoryId);
        q.setTitle("Q" + id);
        return q;
    }

    private Category makeCategory(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setStatus(1);
        return c;
    }
}
