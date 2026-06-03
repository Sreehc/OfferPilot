package com.offerpilot.adaptive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.adaptive.vo.RecommendInterviewVO;
import com.offerpilot.adaptive.vo.RecommendQuestionsVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
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
    private final RecordingReviewSessionMapper recordingReviewSessionMapper;
    private final JobPrepSessionMapper jobPrepSessionMapper;
    private final CopilotPrepSessionMapper copilotPrepSessionMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CategoryService categoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OfferPilotProperties props;

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
                    .eq(Question::getCategoryId, cat.getCategoryId());

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
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
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
        List<RecordingReviewSession> recordingReviews = recordingReviewSessionMapper.selectList(
                new LambdaQueryWrapper<RecordingReviewSession>()
                        .eq(RecordingReviewSession::getUserId, userId)
                        .eq(RecordingReviewSession::getStatus, "ready")
                        .orderByDesc(RecordingReviewSession::getUpdateTime));
        List<JobPrepSession> jobPrepSessions = jobPrepSessionMapper.selectList(
                new LambdaQueryWrapper<JobPrepSession>()
                        .eq(JobPrepSession::getUserId, userId)
                        .eq(JobPrepSession::getStatus, "ready")
                        .orderByDesc(JobPrepSession::getUpdateTime));
        List<CopilotPrepSession> copilotPrepSessions = copilotPrepSessionMapper.selectList(
                new LambdaQueryWrapper<CopilotPrepSession>()
                        .eq(CopilotPrepSession::getUserId, userId)
                        .eq(CopilotPrepSession::getStatus, "ready")
                        .orderByDesc(CopilotPrepSession::getUpdateTime));
        List<JobApplication> applications = jobApplicationMapper.selectList(
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getUserId, userId)
                        .orderByDesc(JobApplication::getUpdateTime));
        List<ResumeFile> resumes = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getUserId, userId)
                        .orderByDesc(ResumeFile::getUpdateTime));

        // Load all finished sessions with records
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .orderByDesc(InterviewSession::getCreateTime));

        if (sessions.isEmpty() && recordingReviews.isEmpty() && jobPrepSessions.isEmpty()
                && copilotPrepSessions.isEmpty() && applications.isEmpty() && resumes.isEmpty()) {
            return AbilityProfileVO.builder()
                    .overallAbility(0.0)
                    .recommendedDifficulty("easy")
                    .recordingReviewCount(0)
                    .categoryAbilities(List.of())
                    .weakCategories(List.of())
                    .suggestedFocus(null)
                    .evidenceStatus("insufficient")
                    .evidenceSummary("还没有足够训练证据形成长期画像。先完成几轮题库、模拟面试或录音复盘。")
                    .build();
        }

        // Load all records for these sessions
        List<Long> sessionIds = sessions.stream().map(InterviewSession::getId).toList();
        List<InterviewRecord> allRecords = sessionIds.isEmpty()
                ? List.of()
                : recordMapper.selectList(
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
        Map<Long, Integer> interviewCountByCategory = new HashMap<>();
        Map<Long, Integer> recordingReviewCountByCategory = new HashMap<>();
        Map<Long, Integer> jobPrepCountByCategory = new HashMap<>();
        Map<Long, Integer> copilotPrepCountByCategory = new HashMap<>();
        Map<Long, Integer> applicationFeedbackCountByCategory = new HashMap<>();
        Map<Long, Integer> resumeEvidenceCountByCategory = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (InterviewRecord record : allRecords) {
            Question q = questionMap.get(record.getQuestionId());
            if (q == null || q.getCategoryId() == null) continue;

            LocalDateTime sessionTime = sessionTimeMap.get(record.getSessionId());
            double recencyWeight = computeRecencyWeight(sessionTime, now);

            categoryScores.computeIfAbsent(q.getCategoryId(), k -> new ArrayList<>())
                    .add(new ScoreEntry(record.getScore().doubleValue(), recencyWeight));
            interviewCountByCategory.merge(q.getCategoryId(), 1, Integer::sum);
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
        List<Long> resumeIds = resumes.stream()
                .map(ResumeFile::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, List<ResumeProject>> resumeProjectsMap = new HashMap<>();
        if (!resumeIds.isEmpty()) {
            resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                            .in(ResumeProject::getResumeFileId, resumeIds)
                            .orderByAsc(ResumeProject::getSortOrder)
                            .orderByAsc(ResumeProject::getId))
                    .forEach(project -> resumeProjectsMap.computeIfAbsent(project.getResumeFileId(), key -> new ArrayList<>()).add(project));
        }

        for (RecordingReviewSession review : recordingReviews) {
            for (Long categoryId : resolveRecordingReviewCategories(review, allCategories)) {
                double reviewScore = review.getOverallScore() == null ? 55.0 : review.getOverallScore().doubleValue();
                double recencyWeight = computeRecencyWeight(
                        review.getUpdateTime() == null ? review.getCreateTime() : review.getUpdateTime(),
                        now) * 0.85;
                categoryScores.computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(new ScoreEntry(reviewScore, recencyWeight));
                recordingReviewCountByCategory.merge(categoryId, 1, Integer::sum);
            }
        }

        for (JobPrepSession session : jobPrepSessions) {
            for (Long categoryId : resolveJobPrepCategories(session, allCategories)) {
                double prepScore = session.getMatchScore() == null ? 58.0 : session.getMatchScore().doubleValue();
                double recencyWeight = computeRecencyWeight(
                        session.getUpdateTime() == null ? session.getCreateTime() : session.getUpdateTime(),
                        now) * 0.60;
                categoryScores.computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(new ScoreEntry(prepScore, recencyWeight));
                jobPrepCountByCategory.merge(categoryId, 1, Integer::sum);
            }
        }

        for (CopilotPrepSession session : copilotPrepSessions) {
            for (Long categoryId : resolveCopilotPrepCategories(session, allCategories)) {
                double prepScore = resolveCopilotPrepScore(session);
                double recencyWeight = computeRecencyWeight(
                        session.getUpdateTime() == null ? session.getCreateTime() : session.getUpdateTime(),
                        now) * 0.55;
                categoryScores.computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(new ScoreEntry(prepScore, recencyWeight));
                copilotPrepCountByCategory.merge(categoryId, 1, Integer::sum);
            }
        }

        for (JobApplication application : applications) {
            for (Long categoryId : resolveApplicationCategories(application, allCategories)) {
                double feedbackScore = resolveApplicationFeedbackScore(application);
                double recencyWeight = computeRecencyWeight(
                        application.getUpdateTime() == null ? application.getCreateTime() : application.getUpdateTime(),
                        now) * 0.50;
                categoryScores.computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(new ScoreEntry(feedbackScore, recencyWeight));
                applicationFeedbackCountByCategory.merge(categoryId, 1, Integer::sum);
            }
        }

        for (ResumeFile resume : resumes) {
            List<ResumeProject> projects = resume.getId() == null
                    ? List.of()
                    : resumeProjectsMap.getOrDefault(resume.getId(), List.of());
            for (Long categoryId : resolveResumeCategories(resume, projects, allCategories)) {
                double resumeScore = resolveResumeEvidenceScore(resume, projects);
                double recencyWeight = computeRecencyWeight(
                        resume.getUpdateTime() == null ? resume.getCreateTime() : resume.getUpdateTime(),
                        now) * 0.45;
                categoryScores.computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(new ScoreEntry(resumeScore, recencyWeight));
                resumeEvidenceCountByCategory.merge(categoryId, 1, Integer::sum);
            }
        }

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
            long wrongCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<com.offerpilot.wrong.entity.WrongQuestion>()
                            .eq(com.offerpilot.wrong.entity.WrongQuestion::getUserId, userId)
                            .inSql(com.offerpilot.wrong.entity.WrongQuestion::getQuestionId,
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
                    .interviewCount(interviewCountByCategory.getOrDefault(categoryId, 0))
                    .recordingReviewCount(recordingReviewCountByCategory.getOrDefault(categoryId, 0))
                    .jobPrepCount(jobPrepCountByCategory.getOrDefault(categoryId, 0))
                    .copilotPrepCount(copilotPrepCountByCategory.getOrDefault(categoryId, 0))
                    .applicationFeedbackCount(applicationFeedbackCountByCategory.getOrDefault(categoryId, 0))
                    .resumeEvidenceCount(resumeEvidenceCountByCategory.getOrDefault(categoryId, 0))
                    .wrongCount((int) wrongCount)
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
        int totalEvidenceCount = allRecords.size() + recordingReviews.size()
                + jobPrepSessions.size() + copilotPrepSessions.size() + applications.size() + resumes.size();
        String evidenceStatus = resolveEvidenceStatus(categoryAbilities, totalEvidenceCount);

        return AbilityProfileVO.builder()
                .overallAbility(Math.round(overallAbility * 100.0) / 100.0)
                .recommendedDifficulty(recommendedDifficulty)
                .recordingReviewCount(recordingReviews.size())
                .categoryAbilities(categoryAbilities)
                .weakCategories(weakCategories)
                .suggestedFocus(suggestedFocus)
                .evidenceStatus(evidenceStatus)
                .evidenceSummary(buildEvidenceSummary(evidenceStatus, totalEvidenceCount, categoryAbilities.size(), weakCategories, suggestedFocus))
                .build();
    }

    private String resolveEvidenceStatus(List<CategoryAbilityVO> categoryAbilities, int totalEvidenceCount) {
        if (categoryAbilities.isEmpty() || totalEvidenceCount == 0) {
            return "insufficient";
        }
        if (totalEvidenceCount < 3) {
            return "forming";
        }
        return "ready";
    }

    private String buildEvidenceSummary(String evidenceStatus, int totalEvidenceCount, int categoryCount,
                                        List<String> weakCategories, String suggestedFocus) {
        return switch (evidenceStatus) {
            case "insufficient" -> "还没有足够训练证据形成长期画像。先完成几轮题库、模拟面试或录音复盘。";
            case "forming" -> "当前画像还在形成中，已沉淀 " + totalEvidenceCount + " 条训练证据，覆盖 "
                    + categoryCount + " 个主题。建议继续补 1-2 轮训练，再看长期趋势。";
            default -> weakCategories.isEmpty()
                    ? "长期画像已形成，当前没有明显单一薄弱主题，可以继续按主线任务推进。"
                    : "长期画像已形成，当前建议优先收紧「" + (suggestedFocus == null ? weakCategories.get(0) : suggestedFocus)
                    + "」等薄弱主题。";
        };
    }

    private List<Long> resolveRecordingReviewCategories(RecordingReviewSession review, List<Category> categories) {
        String evidence = String.join(" ",
                nullSafe(review.getDirection()),
                nullSafe(review.getJobRole()),
                nullSafe(review.getSummary()),
                nullSafe(review.getTranscript()),
                nullSafe(review.getWeakPointsJson()),
                nullSafe(review.getSuggestedActionsJson()));
        if (evidence.isBlank()) {
            return List.of();
        }
        String normalizedEvidence = evidence.toLowerCase();
        List<Long> matched = new ArrayList<>();
        for (Category category : categories) {
            if (category.getId() == null || !org.springframework.util.StringUtils.hasText(category.getName())) {
                continue;
            }
            String categoryName = category.getName().trim().toLowerCase();
            if (normalizedEvidence.contains(categoryName)) {
                matched.add(category.getId());
            }
        }
        return matched.stream().distinct().toList();
    }

    private List<Long> resolveJobPrepCategories(JobPrepSession session, List<Category> categories) {
        String evidence = String.join(" ",
                nullSafe(session.getCompany()),
                nullSafe(session.getJobTitle()),
                nullSafe(session.getJdText()),
                nullSafe(session.getSummary()),
                nullSafe(session.getMatchedKeywordsJson()),
                nullSafe(session.getMissingKeywordsJson()),
                nullSafe(session.getFocusAreasJson()),
                nullSafe(session.getResumeTalkingPointsJson()),
                nullSafe(session.getMockQuestionsJson()),
                nullSafe(session.getNextActionsJson()));
        return matchCategoryIds(evidence, categories);
    }

    private List<Long> resolveCopilotPrepCategories(CopilotPrepSession session, List<Category> categories) {
        String evidence = String.join(" ",
                nullSafe(session.getCompany()),
                nullSafe(session.getJobTitle()),
                nullSafe(session.getJdText()),
                nullSafe(session.getNotes()),
                nullSafe(session.getSummary()),
                nullSafe(session.getOpeningBriefJson()),
                nullSafe(session.getKeyRisksJson()),
                nullSafe(session.getLiveCuesJson()),
                nullSafe(session.getFollowUpQuestionsJson()),
                nullSafe(session.getNextActionsJson()));
        return matchCategoryIds(evidence, categories);
    }

    private List<Long> resolveApplicationCategories(JobApplication application, List<Category> categories) {
        String evidence = String.join(" ",
                nullSafe(application.getCompany()),
                nullSafe(application.getJobTitle()),
                nullSafe(application.getCity()),
                nullSafe(application.getSource()),
                nullSafe(application.getJdText()),
                nullSafe(application.getJdKeywords()),
                nullSafe(application.getMissingKeywords()),
                nullSafe(application.getAnalysisSummary()),
                nullSafe(application.getReviewSuggestion()),
                nullSafe(application.getNextStepSuggestion()));
        return matchCategoryIds(evidence, categories);
    }

    private List<Long> resolveResumeCategories(ResumeFile resume, List<ResumeProject> projects, List<Category> categories) {
        List<String> projectEvidence = projects.stream()
                .flatMap(project -> java.util.stream.Stream.of(
                        nullSafe(project.getProjectName()),
                        nullSafe(project.getRoleName()),
                        nullSafe(project.getTechStack()),
                        nullSafe(project.getResponsibility()),
                        nullSafe(project.getAchievement()),
                        nullSafe(project.getProjectSummary()),
                        nullSafe(project.getRiskHints()),
                        nullSafe(project.getFollowUpQuestionsJson())))
                .toList();
        String evidence = String.join(" ",
                nullSafe(resume.getTitle()),
                nullSafe(resume.getSummary()),
                nullSafe(resume.getSkills()),
                nullSafe(resume.getEducation()),
                nullSafe(resume.getSelfIntro()),
                nullSafe(resume.getInterviewResumeText()),
                String.join(" ", projectEvidence));
        return matchCategoryIds(evidence, categories);
    }

    private List<Long> matchCategoryIds(String evidence, List<Category> categories) {
        if (evidence.isBlank()) {
            return List.of();
        }
        String normalizedEvidence = evidence.toLowerCase();
        List<Long> matched = new ArrayList<>();
        for (Category category : categories) {
            if (category.getId() == null || !org.springframework.util.StringUtils.hasText(category.getName())) {
                continue;
            }
            String categoryName = category.getName().trim().toLowerCase();
            if (normalizedEvidence.contains(categoryName)) {
                matched.add(category.getId());
            }
        }
        return matched.stream().distinct().toList();
    }

    private double resolveCopilotPrepScore(CopilotPrepSession session) {
        String evidence = String.join(" ",
                nullSafe(session.getOpeningBriefJson()),
                nullSafe(session.getKeyRisksJson()),
                nullSafe(session.getLiveCuesJson()),
                nullSafe(session.getFollowUpQuestionsJson()),
                nullSafe(session.getNextActionsJson()));
        int signalCount = 0;
        if (org.springframework.util.StringUtils.hasText(session.getOpeningBriefJson())) {
            signalCount++;
        }
        if (org.springframework.util.StringUtils.hasText(session.getKeyRisksJson())) {
            signalCount++;
        }
        if (org.springframework.util.StringUtils.hasText(session.getLiveCuesJson())) {
            signalCount++;
        }
        if (org.springframework.util.StringUtils.hasText(session.getFollowUpQuestionsJson())) {
            signalCount++;
        }
        if (org.springframework.util.StringUtils.hasText(session.getNextActionsJson())) {
            signalCount++;
        }
        if (org.springframework.util.StringUtils.hasText(session.getSummary())) {
            signalCount++;
        }
        double base = 52.0 + signalCount * 4.0;
        if (evidence.toLowerCase().contains("未完全就绪") || evidence.toLowerCase().contains("missing")) {
            base -= 6.0;
        }
        return Math.max(45.0, Math.min(82.0, base));
    }

    private double resolveApplicationFeedbackScore(JobApplication application) {
        double base = application.getMatchScore() == null ? 56.0 : application.getMatchScore().doubleValue();
        String status = normalize(application.getStatus());
        if ("offer".equals(status)) {
            base += 8.0;
        } else if ("interview".equals(status)) {
            base += 4.0;
        } else if ("written".equals(status)) {
            base += 2.0;
        } else if ("rejected".equals(status)) {
            base -= 8.0;
        }
        int gapCount = countDelimitedItems(application.getMissingKeywords());
        if (gapCount > 0) {
            base -= Math.min(12.0, gapCount * 2.0);
        }
        if (org.springframework.util.StringUtils.hasText(application.getReviewSuggestion())) {
            base -= 2.0;
        }
        return Math.max(35.0, Math.min(88.0, base));
    }

    private double resolveResumeEvidenceScore(ResumeFile resume, List<ResumeProject> projects) {
        double base = 54.0;
        if ("parsed".equals(normalize(resume.getParseStatus()))) {
            base += 5.0;
        }
        if (org.springframework.util.StringUtils.hasText(resume.getSummary())) {
            base += 4.0;
        }
        if (org.springframework.util.StringUtils.hasText(resume.getSelfIntro())) {
            base += 3.0;
        }
        if (org.springframework.util.StringUtils.hasText(resume.getInterviewResumeText())) {
            base += 4.0;
        }
        if (org.springframework.util.StringUtils.hasText(resume.getSkills())) {
            base += 3.0;
        }
        if (!projects.isEmpty()) {
            base += Math.min(8.0, projects.size() * 2.0);
        }
        return Math.max(40.0, Math.min(82.0, base));
    }

    private int countDelimitedItems(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return 0;
        }
        return (int) java.util.Arrays.stream(value.split("[,，]"))
                .map(String::trim)
                .filter(org.springframework.util.StringUtils::hasText)
                .count();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
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
