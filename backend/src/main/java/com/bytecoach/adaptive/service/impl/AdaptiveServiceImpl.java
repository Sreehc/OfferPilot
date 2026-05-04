package com.bytecoach.adaptive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.adaptive.vo.CategoryAbilityVO;
import com.bytecoach.adaptive.vo.RecommendInterviewVO;
import com.bytecoach.adaptive.vo.RecommendQuestionsVO;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.common.config.ByteCoachProperties;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveServiceImpl implements AdaptiveService {

    private static final String CACHE_PREFIX = "adaptive:profile:";

    private final InterviewSessionMapper sessionMapper;
    private final InterviewRecordMapper recordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CategoryService categoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ByteCoachProperties props;

    @Override
    public AbilityProfileVO getAbilityProfile(Long userId) {
        // Try cache first
        String cacheKey = CACHE_PREFIX + userId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, AbilityProfileVO.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read adaptive cache: {}", e.getMessage());
        }

        AbilityProfileVO profile = computeAbilityProfile(userId);

        // Cache the result
        try {
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(cacheKey, json, props.getAdaptive().getCacheTtlHours(), TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to write adaptive cache: {}", e.getMessage());
        }

        return profile;
    }

    @Override
    public List<CategoryAbilityVO> getCategoryAbilities(Long userId) {
        return getAbilityProfile(userId).getCategoryAbilities();
    }

    @Override
    public List<String> getWeakCategories(Long userId) {
        return getAbilityProfile(userId).getWeakCategories();
    }

    @Override
    public String getRecommendedDifficulty(Long userId) {
        return getAbilityProfile(userId).getRecommendedDifficulty();
    }

    @Override
    public RecommendInterviewVO getRecommendInterview(Long userId) {
        AbilityProfileVO profile = getAbilityProfile(userId);

        // Pick the weakest category as recommended focus
        String focusCategory = profile.getSuggestedFocus();
        if (focusCategory == null || focusCategory.isEmpty()) {
            // No weak points — suggest a general interview
            List<Category> categories = categoryService.lambdaQuery()
                    .eq(Category::getStatus, 1)
                    .list();
            if (!categories.isEmpty()) {
                Collections.shuffle(categories);
                focusCategory = categories.get(0).getName();
            } else {
                focusCategory = "Java基础";
            }
        }

        return RecommendInterviewVO.builder()
                .direction(focusCategory)
                .questionCount(5)
                .reason(profile.getWeakCategories().contains(focusCategory)
                        ? "该分类是你的薄弱点，建议重点练习"
                        : "综合练习，巩固已学知识")
                .difficulty(profile.getRecommendedDifficulty())
                .build();
    }

    @Override
    public List<RecommendQuestionsVO> getRecommendQuestions(Long userId, int limit) {
        AbilityProfileVO profile = getAbilityProfile(userId);

        // Get questions the user has already answered
        List<Long> answeredQuestionIds = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getUserId, userId)
                        .select(InterviewRecord::getQuestionId))
                .stream()
                .map(InterviewRecord::getQuestionId)
                .distinct()
                .toList();

        List<RecommendQuestionsVO> recommendations = new ArrayList<>();

        // First, recommend from weak categories
        List<CategoryAbilityVO> sorted = new ArrayList<>(profile.getCategoryAbilities());
        sorted.sort(Comparator.comparingDouble(CategoryAbilityVO::getAbilityScore));

        for (CategoryAbilityVO cat : sorted) {
            if (recommendations.size() >= limit) break;

            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                    .eq(Question::getCategoryId, cat.getCategoryId())
                    .eq(Question::getStatus, 1);

            if (!answeredQuestionIds.isEmpty()) {
                wrapper.notIn(Question::getId, answeredQuestionIds);
            }

            // Match difficulty to recommended level
            String recDifficulty = cat.getRecommendedDifficulty();
            if (recDifficulty != null) {
                wrapper.eq(Question::getDifficulty, recDifficulty);
            }

            wrapper.last("LIMIT " + (limit - recommendations.size()));

            List<Question> questions = questionMapper.selectList(wrapper);
            for (Question q : questions) {
                recommendations.add(RecommendQuestionsVO.builder()
                        .questionId(q.getId())
                        .title(q.getTitle())
                        .categoryId(q.getCategoryId())
                        .categoryName(cat.getCategoryName())
                        .difficulty(q.getDifficulty())
                        .reason(cat.getIsWeak() ? "薄弱分类强化" : "能力匹配推荐")
                        .build());
            }
        }

        // If still not enough, fill with any unanswered questions
        if (recommendations.size() < limit) {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                    .eq(Question::getStatus, 1);
            if (!answeredQuestionIds.isEmpty()) {
                wrapper.notIn(Question::getId, answeredQuestionIds);
            }
            if (!recommendations.isEmpty()) {
                wrapper.notIn(Question::getId, recommendations.stream()
                        .map(RecommendQuestionsVO::getQuestionId).toList());
            }
            wrapper.last("LIMIT " + (limit - recommendations.size()));

            List<Question> filler = questionMapper.selectList(wrapper);
            for (Question q : filler) {
                recommendations.add(RecommendQuestionsVO.builder()
                        .questionId(q.getId())
                        .title(q.getTitle())
                        .categoryId(q.getCategoryId())
                        .difficulty(q.getDifficulty())
                        .reason("拓展练习")
                        .build());
            }
        }

        return recommendations;
    }

    @Override
    public void refreshAbilityProfile(Long userId) {
        String cacheKey = CACHE_PREFIX + userId;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to evict adaptive cache: {}", e.getMessage());
        }
        // Re-compute and cache
        getAbilityProfile(userId);
    }

    // ──────────────────────────────────────────────
    // Core computation
    // ──────────────────────────────────────────────

    private AbilityProfileVO computeAbilityProfile(Long userId) {
        // Load all finished sessions with records
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .orderByDesc(InterviewSession::getCreateTime));

        if (sessions.isEmpty()) {
            return AbilityProfileVO.builder()
                    .overallAbility(0.0)
                    .recommendedDifficulty("easy")
                    .categoryAbilities(List.of())
                    .weakCategories(List.of())
                    .suggestedFocus(null)
                    .build();
        }

        // Load all records for these sessions
        List<Long> sessionIds = sessions.stream().map(InterviewSession::getId).toList();
        List<InterviewRecord> allRecords = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .in(InterviewRecord::getSessionId, sessionIds)
                        .isNotNull(InterviewRecord::getScore));

        // Build session creation time map for recency weighting
        Map<Long, LocalDateTime> sessionTimeMap = sessions.stream()
                .collect(Collectors.toMap(InterviewSession::getId, InterviewSession::getCreateTime, (a, b) -> a));

        // Load all questions to get category mapping
        List<Long> questionIds = allRecords.stream()
                .map(InterviewRecord::getQuestionId)
                .distinct()
                .toList();

        Map<Long, Question> questionMap = new HashMap<>();
        if (!questionIds.isEmpty()) {
            questionMapper.selectBatchIds(questionIds)
                    .forEach(q -> questionMap.put(q.getId(), q));
        }

        // Group records by category and compute weighted scores
        Map<Long, List<ScoreEntry>> categoryScores = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (InterviewRecord record : allRecords) {
            Question q = questionMap.get(record.getQuestionId());
            if (q == null || q.getCategoryId() == null) continue;

            LocalDateTime sessionTime = sessionTimeMap.get(record.getSessionId());
            double recencyWeight = computeRecencyWeight(sessionTime, now);

            categoryScores.computeIfAbsent(q.getCategoryId(), k -> new ArrayList<>())
                    .add(new ScoreEntry(record.getScore().doubleValue(), recencyWeight));
        }

        // Compute ability per category
        List<CategoryAbilityVO> categoryAbilities = new ArrayList<>();
        List<String> weakCategories = new ArrayList<>();
        double totalWeightedAbility = 0;
        double totalWeight = 0;

        // Get all categories
        List<Category> allCategories = categoryService.lambdaQuery()
                .eq(Category::getStatus, 1)
                .list();

        Map<Long, String> categoryNameMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        for (Map.Entry<Long, List<ScoreEntry>> entry : categoryScores.entrySet()) {
            Long categoryId = entry.getKey();
            List<ScoreEntry> scores = entry.getValue();

            double weightedSum = 0;
            double weightSum = 0;
            for (ScoreEntry se : scores) {
                weightedSum += se.score * se.weight;
                weightSum += se.weight;
            }
            double ability = weightSum > 0 ? weightedSum / weightSum : 0;

            // Count wrong questions for this category
            int wrongCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<com.bytecoach.wrong.entity.WrongQuestion>()
                            .eq(com.bytecoach.wrong.entity.WrongQuestion::getUserId, userId)
                            .inSql(com.bytecoach.wrong.entity.WrongQuestion::getQuestionId,
                                    "SELECT id FROM question WHERE category_id = " + categoryId));

            boolean isWeak = ability < props.getAdaptive().getWeakThreshold();
            if (isWeak) {
                weakCategories.add(categoryNameMap.getOrDefault(categoryId, "未知"));
            }

            String recDifficulty = computeDifficulty(ability);

            categoryAbilities.add(CategoryAbilityVO.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryNameMap.getOrDefault(categoryId, "未知"))
                    .abilityScore(Math.round(ability * 100.0) / 100.0)
                    .interviewCount(scores.size())
                    .wrongCount(wrongCount)
                    .isWeak(isWeak)
                    .recommendedDifficulty(recDifficulty)
                    .build());

            totalWeightedAbility += ability * weightSum;
            totalWeight += weightSum;
        }

        // Sort by ability score ascending (weakest first)
        categoryAbilities.sort(Comparator.comparingDouble(CategoryAbilityVO::getAbilityScore));

        double overallAbility = totalWeight > 0 ? totalWeightedAbility / totalWeight : 0;
        String recommendedDifficulty = computeDifficulty(overallAbility);

        // Suggested focus: the weakest category
        String suggestedFocus = categoryAbilities.isEmpty() ? null
                : categoryAbilities.get(0).getCategoryName();

        return AbilityProfileVO.builder()
                .overallAbility(Math.round(overallAbility * 100.0) / 100.0)
                .recommendedDifficulty(recommendedDifficulty)
                .categoryAbilities(categoryAbilities)
                .weakCategories(weakCategories)
                .suggestedFocus(suggestedFocus)
                .build();
    }

    /**
     * Compute recency weight using exponential decay.
     * More recent sessions get higher weight (up to 3x).
     * Sessions older than 30 days get weight ~1x.
     */
    private double computeRecencyWeight(LocalDateTime sessionTime, LocalDateTime now) {
        if (sessionTime == null) return 1.0;
        long daysBetween = ChronoUnit.DAYS.between(sessionTime, now);
        // Exponential decay: weight = 1 + 2 * exp(-days/14)
        // At day 0: weight ~3, at day 14: weight ~1.74, at day 30: weight ~1.25
        return 1.0 + 2.0 * Math.exp(-daysBetween / 14.0);
    }

    /**
     * Map ability score (0-100) to difficulty level using ZPD theory.
     * Recommended difficulty = current ability + one step up.
     */
    private String computeDifficulty(double ability) {
        if (ability < 30) return "easy";
        if (ability < 60) return "medium";
        return "hard";
    }

    private static class ScoreEntry {
        final double score;
        final double weight;

        ScoreEntry(double score, double weight) {
            this.score = score;
            this.weight = weight;
        }
    }
}
