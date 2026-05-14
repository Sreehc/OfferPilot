package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.cards.service.KnowledgeCardService;
import com.offerpilot.cards.vo.KnowledgeCardTaskVO;
import com.offerpilot.cards.vo.TodayCardsTaskVO;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.vo.ContextSourceVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.notification.service.NotificationService;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.mapper.VoiceRecordMapper;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.entity.VoiceRecord;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.resume.vo.ResumeProjectQuestionVO;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");
    private static final String INTERVIEW_DECK_TITLE = "面试诊断卡片";
    private static final String SOURCE_REF_INTERVIEW_RECORD = "interview_record";

    private final InterviewSessionMapper sessionMapper;
    private final InterviewRecordMapper recordMapper;
    private final VoiceRecordMapper voiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CategoryService categoryService;
    private final AiOrchestratorService aiOrchestratorService;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final KnowledgeCardService knowledgeCardService;
    private final OfferPilotProperties props;
    private final ObjectMapper objectMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;

    @Lazy
    @Autowired
    private InterviewServiceImpl self;

    @Override
    @Transactional
    public InterviewCurrentQuestionVO start(Long userId, InterviewStartRequest request) {
        InterviewContextSnapshot context = resolveInterviewContext(userId, request.getResumeId(), request.getProjectId());
        String effectiveTechStack = mergeTechStack(request.getTechStack(), context.techStack());
        boolean useResumeProjectContext = Boolean.TRUE.equals(request.getIncludeResumeProject()) || !"general".equals(context.type());
        List<Question> questions = pickQuestions(request.getDirection(), effectiveTechStack, request.getQuestionCount(), context);

        // If reanswerQuestionId is set, ensure it's the first question
        if (request.getReanswerQuestionId() != null) {
            Question reanswerQ = questionMapper.selectById(request.getReanswerQuestionId());
            if (reanswerQ != null) {
                questions.removeIf(q -> q.getId().equals(reanswerQ.getId()));
                questions.add(0, reanswerQ);
            }
        }

        if (questions.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "no questions found for direction: " + request.getDirection());
        }

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setDirection(request.getDirection());
        session.setJobRole(request.getJobRole());
        session.setExperienceLevel(request.getExperienceLevel());
        session.setTechStack(effectiveTechStack);
        session.setContextType(context.type());
        session.setResumeFileId(context.resumeId());
        session.setResumeProjectId(context.projectId());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setIncludeResumeProject(useResumeProjectContext ? 1 : 0);
        session.setStatus("in_progress");
        session.setQuestionCount(questions.size());
        session.setCurrentIndex(1);
        session.setStartTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // Insert all records upfront so we can track per-question state
        for (int i = 0; i < questions.size(); i++) {
            InterviewRecord record = new InterviewRecord();
            record.setSessionId(session.getId());
            record.setUserId(userId);
            record.setQuestionId(questions.get(i).getId());
            record.setIsWrong(0);
            recordMapper.insert(record);
        }

        Question first = questions.get(0);
        return InterviewCurrentQuestionVO.builder()
                .sessionId(session.getId())
                .currentIndex(1)
                .questionCount(questions.size())
                .questionId(first.getId())
                .questionTitle(resolveQuestionTitle(first, 1, questions.size(), context))
                .direction(session.getDirection())
                .jobRole(session.getJobRole())
                .experienceLevel(session.getExperienceLevel())
                .techStack(session.getTechStack())
                .durationMinutes(session.getDurationMinutes())
                .includeResumeProject(includeResumeProject(session))
                .contextType(context.type())
                .contextSource(context.source())
                .build();
    }

    @Override
    public InterviewCurrentQuestionVO current(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        List<InterviewRecord> records = getRecords(sessionId);
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "session has no records");
        }

        int idx = Math.min(session.getCurrentIndex() - 1, records.size() - 1);
        InterviewRecord record = records.get(idx);
        Question question = questionMapper.selectById(record.getQuestionId());
        InterviewContextSnapshot context = resolveInterviewContext(userId, session.getResumeFileId(), session.getResumeProjectId());
        String title = resolveQuestionTitle(question, session.getCurrentIndex(), session.getQuestionCount(), context);

        return InterviewCurrentQuestionVO.builder()
                .sessionId(sessionId)
                .currentIndex(session.getCurrentIndex())
                .questionCount(session.getQuestionCount())
                .questionId(record.getQuestionId())
                .questionTitle(title)
                .direction(session.getDirection())
                .jobRole(session.getJobRole())
                .experienceLevel(session.getExperienceLevel())
                .techStack(session.getTechStack())
                .durationMinutes(session.getDurationMinutes())
                .includeResumeProject(includeResumeProject(session))
                .contextType(context.type())
                .contextSource(context.source())
                .build();
    }

    @Override
    public InterviewAnswerVO answer(Long userId, InterviewAnswerRequest request) {
        // Phase 1: load context and enrich request (lightweight, no LLM)
        InterviewSession session = getOwnedSession(userId, request.getSessionId());
        if (!"in_progress".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "interview session is already finished");
        }

        List<InterviewRecord> records = getRecords(session.getId());
        InterviewContextSnapshot context = resolveInterviewContext(userId, session.getResumeFileId(), session.getResumeProjectId());
        InterviewRecord currentRecord = records.stream()
                .filter(r -> r.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not in this session"));

        Question question = questionMapper.selectById(request.getQuestionId());
        if (question != null || !"general".equals(context.type())) {
            request.setQuestionTitle(resolveQuestionTitle(question, session.getCurrentIndex(), session.getQuestionCount(), context));
        }
        if (question != null) {
            request.setStandardAnswer(question.getStandardAnswer());
            request.setScoreStandard(question.getScoreStandard());
        }
        request.setDirection(session.getDirection());
        request.setJobRole(session.getJobRole());
        request.setExperienceLevel(session.getExperienceLevel());
        request.setTechStack(session.getTechStack());
        request.setIncludeResumeProject(session.getIncludeResumeProject() != null && session.getIncludeResumeProject() == 1);
        request.setContextType(context.type());
        request.setContextSummary(context.source().getSummary());

        boolean hasNext = session.getCurrentIndex() < session.getQuestionCount();

        // Phase 2: call LLM outside any transaction
        InterviewAnswerVO aiResult = aiOrchestratorService.scoreInterviewAnswer(request);

        // Phase 3: persist result and advance session (in transaction)
        boolean addedToWrong = self.persistAnswerAndAdvance(userId, session, currentRecord, records, request, aiResult);

        return InterviewAnswerVO.builder()
                .score(aiResult.getScore())
                .comment(aiResult.getComment())
                .standardAnswer(aiResult.getStandardAnswer())
                .followUp(aiResult.getFollowUp())
                .scoreBreakdown(aiResult.getScoreBreakdown())
                .weakPointTags(aiResult.getWeakPointTags())
                .reviewSummary(aiResult.getReviewSummary())
                .addedToWrongBook(addedToWrong)
                .hasNextQuestion(hasNext)
                .build();
    }

    @Transactional
    public boolean persistAnswerAndAdvance(Long userId, InterviewSession session, InterviewRecord currentRecord,
                                           List<InterviewRecord> records, InterviewAnswerRequest request,
                                           InterviewAnswerVO aiResult) {
        currentRecord.setUserAnswer(request.getAnswer());
        currentRecord.setScore(aiResult.getScore());
        currentRecord.setComment(aiResult.getComment());
        currentRecord.setFollowUp(aiResult.getFollowUp());
        currentRecord.setScoreDimensionsJson(writeScoreBreakdown(aiResult.getScoreBreakdown()));
        currentRecord.setWeakPointTags(writeWeakPointTags(aiResult.getWeakPointTags()));
        currentRecord.setReviewSummary(aiResult.getReviewSummary());
        recordMapper.updateById(currentRecord);

        boolean addedToWrong = false;
        BigDecimal wrongThreshold = BigDecimal.valueOf(props.getInterview().getWrongThreshold());
        if (aiResult.getScore() != null && aiResult.getScore().compareTo(wrongThreshold) < 0) {
            addedToWrong = true;
            currentRecord.setIsWrong(1);
            recordMapper.updateById(currentRecord);
            addToWrongBook(userId, request, aiResult);
        }

        int currentIndex = session.getCurrentIndex();
        boolean hasNext = currentIndex < session.getQuestionCount();

        if (hasNext) {
            session.setCurrentIndex(currentIndex + 1);
            sessionMapper.updateById(session);
        } else {
            finishSession(session, records, currentRecord);
            dashboardService.evictCache(userId);
        }

        if (addedToWrong) {
            dashboardService.evictCache(userId);
        }

        return addedToWrong;
    }

    @Override
    public InterviewDetailVO detail(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        List<InterviewRecord> records = getRecords(sessionId);
        InterviewContextSnapshot context = resolveInterviewContext(userId, session.getResumeFileId(), session.getResumeProjectId());

        List<Long> questionIds = records.stream()
                .map(InterviewRecord::getQuestionId)
                .distinct()
                .toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        // Load voice records if this is a voice session
        Map<Long, VoiceRecord> voiceRecordMap;
        if ("voice".equals(session.getMode())) {
            List<VoiceRecord> voiceRecords = voiceRecordMapper.selectList(
                    new LambdaQueryWrapper<VoiceRecord>()
                            .eq(VoiceRecord::getSessionId, sessionId));
            voiceRecordMap = voiceRecords.stream()
                    .collect(Collectors.toMap(VoiceRecord::getRecordId, Function.identity(), (a, b) -> a));
        } else {
            voiceRecordMap = Map.of();
        }

        KnowledgeCardTaskVO interviewDeck = knowledgeCardService.getInterviewDeck(userId);
        InterviewDeckSnapshot deckSnapshot = buildInterviewDeckSnapshot(records, interviewDeck);

        List<InterviewDetailVO.InterviewRecordVO> recordVOs = records.stream()
                .map(record -> Map.entry(record, records.indexOf(record) + 1))
                .map(record -> {
                    InterviewRecord item = record.getKey();
                    int questionIndex = record.getValue();
                    Question q = questionMap.get(item.getQuestionId());
                    VoiceRecord vr = voiceRecordMap.get(item.getId());
                    String generatedCardId = deckSnapshot.generatedCardIdsByRecordId.get(item.getId());
                    return InterviewDetailVO.InterviewRecordVO.builder()
                            .questionId(item.getQuestionId())
                            .questionTitle(resolveQuestionTitle(q, questionIndex, session.getQuestionCount(), context))
                            .userAnswer(item.getUserAnswer())
                            .score(item.getScore())
                            .comment(item.getComment())
                            .standardAnswer(q != null ? q.getStandardAnswer() : null)
                            .followUp(item.getFollowUp())
                            .scoreBreakdown(parseScoreBreakdown(item.getScoreDimensionsJson()))
                            .weakPointTags(parseWeakPointTags(item.getWeakPointTags()))
                            .reviewSummary(item.getReviewSummary())
                            .isLowScore(Integer.valueOf(1).equals(item.getIsWrong()))
                            .recommendedCardFront(q != null ? q.getTitle() : "Unknown")
                            .recommendedCardBack(q != null ? q.getStandardAnswer() : null)
                            .recommendedCardExplanation(buildInterviewCardExplanation(item))
                            .recommendedCardFollowUp(item.getFollowUp())
                            .generatedCardId(generatedCardId)
                            .voiceTranscript(vr != null ? vr.getTranscript() : null)
                            .voiceConfidence(vr != null ? vr.getTranscriptConfidence() : null)
                            .build();
                })
                .toList();

        return InterviewDetailVO.builder()
                .sessionId(sessionId)
                .direction(session.getDirection())
                .jobRole(session.getJobRole())
                .experienceLevel(session.getExperienceLevel())
                .techStack(session.getTechStack())
                .durationMinutes(session.getDurationMinutes())
                .includeResumeProject(includeResumeProject(session))
                .contextType(context.type())
                .contextSource(context.source())
                .status(session.getStatus())
                .mode(session.getMode())
                .totalScore(session.getTotalScore())
                .questionCount(session.getQuestionCount())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .cardsGenerated(deckSnapshot.generatedCardCount > 0)
                .generatedCardCount(deckSnapshot.generatedCardCount)
                .interviewDeckId(deckSnapshot.deckId)
                .interviewDeckTitle(deckSnapshot.deckTitle)
                .records(recordVOs)
                .build();
    }

    @Override
    public PageResult<InterviewHistoryVO> history(Long userId, String direction, int pageNum, int pageSize) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getStatus, "finished")
                .eq(direction != null && !direction.isEmpty(), InterviewSession::getDirection, direction)
                .orderByDesc(InterviewSession::getCreateTime);

        Page<InterviewSession> page = sessionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        KnowledgeCardTaskVO interviewDeck = knowledgeCardService.getInterviewDeck(userId);
        Map<Long, InterviewDeckSnapshot> deckSnapshots = buildInterviewDeckSnapshots(userId, page.getRecords(), interviewDeck);

        List<InterviewHistoryVO> records = page.getRecords().stream()
                .map(s -> {
                    InterviewDeckSnapshot snapshot = deckSnapshots.getOrDefault(s.getId(), InterviewDeckSnapshot.empty());
                    return InterviewHistoryVO.builder()
                            .sessionId(s.getId())
                            .direction(s.getDirection())
                            .jobRole(s.getJobRole())
                            .experienceLevel(s.getExperienceLevel())
                            .techStack(s.getTechStack())
                            .durationMinutes(s.getDurationMinutes())
                            .includeResumeProject(includeResumeProject(s))
                            .contextType(resolveInterviewContext(userId, s.getResumeFileId(), s.getResumeProjectId()).type())
                            .contextSource(resolveInterviewContext(userId, s.getResumeFileId(), s.getResumeProjectId()).source())
                            .status(s.getStatus())
                            .mode(s.getMode())
                            .totalScore(s.getTotalScore())
                            .questionCount(s.getQuestionCount())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .cardsGenerated(snapshot.generatedCardCount > 0)
                            .generatedCardCount(snapshot.generatedCardCount)
                            .interviewDeckId(snapshot.deckId)
                            .build();
                })
                .toList();

        return PageResult.<InterviewHistoryVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();
    }

    @Override
    public List<InterviewHistoryVO> trendData(Long userId, int limit) {
        List<InterviewSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getStatus, "finished")
                .isNotNull(InterviewSession::getTotalScore)
                .orderByAsc(InterviewSession::getCreateTime)
                .last("LIMIT " + limit));

        return sessions.stream()
                .map(s -> InterviewHistoryVO.builder()
                        .sessionId(s.getId())
                        .direction(s.getDirection())
                        .jobRole(s.getJobRole())
                        .experienceLevel(s.getExperienceLevel())
                        .techStack(s.getTechStack())
                        .durationMinutes(s.getDurationMinutes())
                        .includeResumeProject(includeResumeProject(s))
                        .contextType(resolveInterviewContext(userId, s.getResumeFileId(), s.getResumeProjectId()).type())
                        .contextSource(resolveInterviewContext(userId, s.getResumeFileId(), s.getResumeProjectId()).source())
                        .status(s.getStatus())
                        .mode(s.getMode())
                        .totalScore(s.getTotalScore())
                        .questionCount(s.getQuestionCount())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public InterviewDetailVO generateCards(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        if (!"finished".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只有已完成的面试诊断才能生成复习卡片");
        }
        knowledgeCardService.syncInterviewDeckBySession(userId, sessionId);
        return detail(userId, sessionId);
    }

    @Override
    @Transactional
    public TodayCardsTaskVO activateCards(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        if (!"finished".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只有已完成的面试诊断才能加入今日卡片");
        }
        knowledgeCardService.syncInterviewDeckBySession(userId, sessionId);
        return knowledgeCardService.activateInterviewDeck(userId);
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    private List<Question> pickQuestions(String direction, String techStack, int count, InterviewContextSnapshot context) {
        // Try matching category by name — first look for 'interview' type, then 'question' type
        Category category = categoryService.lambdaQuery()
                .eq(Category::getName, direction)
                .eq(Category::getType, "interview")
                .one();
        if (category == null) {
            category = categoryService.lambdaQuery()
                    .eq(Category::getName, direction)
                    .eq(Category::getType, "question")
                    .one();
        }

        List<Question> pool;
        if (category != null) {
            pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .eq(Question::getCategoryId, category.getId()));
        } else {
            // Fallback: search by title keyword
            pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .like(Question::getTitle, direction));
        }

        List<String> techKeywords = new ArrayList<>();
        if (techStack != null && !techStack.isBlank()) {
            techKeywords.addAll(java.util.Arrays.stream(techStack.split("[,，/\\s]+"))
                    .map(String::trim)
                    .filter(keyword -> !keyword.isBlank())
                    .toList());
        }
        if (context.projectName() != null) {
            techKeywords.add(context.projectName());
        }
        if (!techKeywords.isEmpty()) {
            pool = pool.stream()
                    .sorted((left, right) -> Integer.compare(
                            techMatchScore(right, techKeywords),
                            techMatchScore(left, techKeywords)))
                    .toList();
        }

        if (pool.isEmpty()) {
            // Last resort: grab any questions
            pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .last("LIMIT 20"));
        }

        pool = new ArrayList<>(pool);
        if (techKeywords.isEmpty()) {
            Collections.shuffle(pool);
        } else {
            int headSize = Math.min(pool.size(), Math.max(count * 3, count));
            List<Question> head = new ArrayList<>(pool.subList(0, headSize));
            Collections.shuffle(head);
            List<Question> tail = headSize < pool.size() ? new ArrayList<>(pool.subList(headSize, pool.size())) : List.of();
            pool = new ArrayList<>(head);
            pool.addAll(tail);
        }
        List<Question> selected = new ArrayList<>(pool.stream().limit(count).toList());
        if (selected.size() >= count) {
            return selected;
        }

        // If the direction-specific pool is insufficient, backfill from the broader question set.
        List<Question> fallbackPool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .last("LIMIT 100"));
        Collections.shuffle(fallbackPool);

        Map<Long, Question> deduped = new LinkedHashMap<>();
        for (Question question : selected) {
            deduped.put(question.getId(), question);
        }
        for (Question question : fallbackPool) {
            deduped.putIfAbsent(question.getId(), question);
            if (deduped.size() >= count) {
                break;
            }
        }

        return new ArrayList<>(deduped.values()).stream().limit(count).toList();
    }

    private InterviewSession getOwnedSession(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "interview session not found");
        }
        return session;
    }

    private List<InterviewRecord> getRecords(Long sessionId) {
        return recordMapper.selectList(new LambdaQueryWrapper<InterviewRecord>()
                .eq(InterviewRecord::getSessionId, sessionId)
                .orderByAsc(InterviewRecord::getId));
    }

    private void addToWrongBook(Long userId, InterviewAnswerRequest request, InterviewAnswerVO aiResult) {
        // Check if already in wrong book
        WrongQuestion existing = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, request.getQuestionId()));
        if (existing != null) {
            // Already tracked — update review count
            existing.setReviewCount(existing.getReviewCount() + 1);
            existing.setLastReviewTime(LocalDateTime.now());
            existing.setUserAnswer(request.getAnswer());
            existing.setErrorReason(aiResult.getComment());
            wrongQuestionMapper.updateById(existing);
            knowledgeCardService.syncWrongDeck(userId);
            return;
        }

        WrongQuestion wrong = new WrongQuestion();
        wrong.setUserId(userId);
        wrong.setQuestionId(request.getQuestionId());
        wrong.setSourceType("interview");
        wrong.setUserAnswer(request.getAnswer());
        wrong.setStandardAnswer(aiResult.getStandardAnswer());
        wrong.setErrorReason(aiResult.getComment());
        wrong.setMasteryLevel("not_started");
        wrong.setReviewCount(0);
        wrong.setEaseFactor(DEFAULT_EASE_FACTOR);
        wrong.setIntervalDays(1);
        wrong.setNextReviewDate(LocalDate.now());
        wrong.setStreak(0);
        wrongQuestionMapper.insert(wrong);
        knowledgeCardService.syncWrongDeck(userId);
    }

    private void finishSession(InterviewSession session, List<InterviewRecord> records, InterviewRecord lastRecord) {
        // Collect all scores including the one we just saved
        List<BigDecimal> scores = new ArrayList<>();
        for (InterviewRecord r : records) {
            BigDecimal s = r.getId().equals(lastRecord.getId()) ? lastRecord.getScore() : r.getScore();
            if (s != null) {
                scores.add(s);
            }
        }

        BigDecimal avgScore = scores.isEmpty()
                ? BigDecimal.ZERO
                : scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        session.setStatus("finished");
        session.setTotalScore(avgScore);
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        knowledgeCardService.syncInterviewDeckBySession(session.getUserId(), session.getId());

        log.info("Interview session {} finished with avg score {}", session.getId(), avgScore);

        // Send interview completion notification
        try {
            String title = "面试完成通知";
            String content = String.format("你在「%s」方向的面试已完成，平均得分 %s 分。快来看看详细结果吧！",
                    session.getDirection(), avgScore.toPlainString());
            notificationService.send(session.getUserId(), "interview", title, content,
                    "/interview/detail/" + session.getId());
        } catch (Exception e) {
            log.warn("Failed to send interview completion notification: {}", e.getMessage());
        }
    }

    private InterviewDeckSnapshot buildInterviewDeckSnapshot(List<InterviewRecord> records, KnowledgeCardTaskVO interviewDeck) {
        if (interviewDeck == null || interviewDeck.getCards() == null || interviewDeck.getCards().isEmpty()) {
            return InterviewDeckSnapshot.empty();
        }
        Map<Long, String> generatedCardIdsByRecordId = interviewDeck.getCards().stream()
                .filter(card -> SOURCE_REF_INTERVIEW_RECORD.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .collect(Collectors.toMap(
                        card -> Long.valueOf(card.getSourceRefId()),
                        card -> card.getId(),
                        (a, b) -> a));

        int generatedCardCount = (int) records.stream()
                .filter(record -> Integer.valueOf(1).equals(record.getIsWrong()))
                .filter(record -> generatedCardIdsByRecordId.containsKey(record.getId()))
                .count();
        return new InterviewDeckSnapshot(
                interviewDeck.getId(),
                interviewDeck.getDeckTitle() == null ? INTERVIEW_DECK_TITLE : interviewDeck.getDeckTitle(),
                generatedCardCount,
                generatedCardIdsByRecordId);
    }

    private Map<Long, InterviewDeckSnapshot> buildInterviewDeckSnapshots(Long userId, List<InterviewSession> sessions, KnowledgeCardTaskVO interviewDeck) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = sessions.stream().map(InterviewSession::getId).toList();
        List<InterviewRecord> records = recordMapper.selectList(new LambdaQueryWrapper<InterviewRecord>()
                .eq(InterviewRecord::getUserId, userId)
                .in(InterviewRecord::getSessionId, sessionIds)
                .orderByAsc(InterviewRecord::getId));
        Map<Long, List<InterviewRecord>> recordsBySessionId = records.stream()
                .collect(Collectors.groupingBy(InterviewRecord::getSessionId));
        Map<Long, InterviewDeckSnapshot> snapshots = new LinkedHashMap<>();
        for (InterviewSession session : sessions) {
            snapshots.put(session.getId(), buildInterviewDeckSnapshot(recordsBySessionId.getOrDefault(session.getId(), List.of()), interviewDeck));
        }
        return snapshots;
    }

    private String buildInterviewCardExplanation(InterviewRecord record) {
        String comment = record.getComment() == null || record.getComment().isBlank()
                ? "这题需要回到标准答案，补齐关键知识点。"
                : record.getComment();
        String userAnswer = record.getUserAnswer() == null || record.getUserAnswer().isBlank()
                ? "未作答"
                : record.getUserAnswer();
        return comment + "\n\n回答问题点：\n" + userAnswer;
    }

    private int techMatchScore(Question question, List<String> techKeywords) {
        String haystack = String.join("\n",
                question.getTitle() == null ? "" : question.getTitle(),
                question.getTags() == null ? "" : question.getTags(),
                question.getJobDirection() == null ? "" : question.getJobDirection(),
                question.getApplicableScope() == null ? "" : question.getApplicableScope()).toLowerCase();
        int score = 0;
        for (String keyword : techKeywords) {
            if (haystack.contains(keyword.toLowerCase())) {
                score += 3;
            }
        }
        return score;
    }

    private boolean includeResumeProject(InterviewSession session) {
        return session.getIncludeResumeProject() != null && session.getIncludeResumeProject() == 1;
    }

    private String mergeTechStack(String requestTechStack, String contextTechStack) {
        if (!StringUtils.hasText(requestTechStack)) {
            return contextTechStack;
        }
        if (!StringUtils.hasText(contextTechStack)) {
            return requestTechStack;
        }
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        for (String item : (requestTechStack + "," + contextTechStack).split("[,，]")) {
            String normalized = item.trim();
            if (!normalized.isBlank()) {
                merged.putIfAbsent(normalized.toLowerCase(), normalized);
            }
        }
        return String.join(", ", merged.values());
    }

    private InterviewContextSnapshot resolveInterviewContext(Long userId, Long resumeId, Long projectId) {
        if (projectId != null) {
            ResumeProject project = resumeProjectMapper.selectById(projectId);
            if (project == null || !project.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume project not found");
            }
            ResumeFile resume = resumeFileMapper.selectById(project.getResumeFileId());
            if (resume == null || !resume.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume file not found");
            }
            ContextSourceVO source = ContextSourceVO.builder()
                    .type("project")
                    .label("项目上下文")
                    .summary(buildProjectContextSummary(project, resume))
                    .resumeId(resume.getId())
                    .resumeTitle(resume.getTitle())
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .build();
            return new InterviewContextSnapshot(
                    "project",
                    source,
                    resume.getId(),
                    project.getId(),
                    project.getProjectName(),
                    project.getTechStack(),
                    parseProjectFollowUpQuestions(project.getFollowUpQuestionsJson()));
        }
        if (resumeId != null) {
            ResumeFile resume = resumeFileMapper.selectById(resumeId);
            if (resume == null || !resume.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume file not found");
            }
            ContextSourceVO source = ContextSourceVO.builder()
                    .type("resume")
                    .label("简历上下文")
                    .summary(buildResumeContextSummary(resume))
                    .resumeId(resume.getId())
                    .resumeTitle(resume.getTitle())
                    .build();
            return new InterviewContextSnapshot("resume", source, resume.getId(), null, null, resume.getSkills(), List.of());
        }
        ContextSourceVO source = ContextSourceVO.builder()
                .type("general")
                .label("通用模拟")
                .summary("当前以通用方向题为主，不绑定特定简历或项目。")
                .build();
        return new InterviewContextSnapshot("general", source, null, null, null, null, List.of());
    }

    private String resolveQuestionTitle(Question question, int currentIndex, int questionCount, InterviewContextSnapshot context) {
        String baseTitle = question != null && StringUtils.hasText(question.getTitle()) ? question.getTitle() : "请继续回答当前问题";
        if ("project".equals(context.type())) {
            if (currentIndex <= context.projectQuestions().size()) {
                return context.projectQuestions().get(currentIndex - 1);
            }
            if (currentIndex == 1) {
                return "请你用 1 分钟介绍项目「" + context.projectName() + "」，重点讲背景、职责和结果。";
            }
            return "请结合项目「" + context.projectName() + "」回答：" + baseTitle;
        }
        if ("resume".equals(context.type())) {
            if (currentIndex == 1) {
                return "请先用 1 分钟介绍你的简历亮点，以及它和目标岗位的匹配度。";
            }
            if (currentIndex == questionCount) {
                return "如果面试官继续从你的简历经历追问，你会怎么把这段能力讲得更扎实？";
            }
            return "请结合你的简历经历回答：" + baseTitle;
        }
        return baseTitle;
    }

    private String buildResumeContextSummary(ResumeFile resume) {
        String summary = StringUtils.hasText(resume.getSummary()) ? resume.getSummary() : "可围绕这份简历里的项目经历、技术栈和结果来组织回答。";
        return "当前绑定简历《" + resume.getTitle() + "》。" + summary;
    }

    private String buildProjectContextSummary(ResumeProject project, ResumeFile resume) {
        String lead = "当前绑定项目「" + project.getProjectName() + "」";
        if (resume != null) {
            lead += "，来源简历《" + resume.getTitle() + "》";
        }
        String summary = StringUtils.hasText(project.getProjectSummary()) ? project.getProjectSummary() : "优先围绕项目背景、职责、方案选择和结果来组织表达。";
        String techStack = StringUtils.hasText(project.getTechStack()) ? "技术栈：" + project.getTechStack() + "。" : "";
        String responsibility = StringUtils.hasText(project.getResponsibility()) ? "职责：" + project.getResponsibility() + "。" : "";
        String achievement = StringUtils.hasText(project.getAchievement()) ? "结果：" + project.getAchievement() + "。" : "";
        return lead + "。" + summary + " " + techStack + responsibility + achievement;
    }

    private List<String> parseProjectFollowUpQuestions(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<ResumeProjectQuestionVO>>() {})
                    .stream()
                    .map(ResumeProjectQuestionVO::getQuestion)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse resume project follow-up questions: {}", e.getMessage());
            return List.of();
        }
    }

    private record InterviewContextSnapshot(
            String type,
            ContextSourceVO source,
            Long resumeId,
            Long projectId,
            String projectName,
            String techStack,
            List<String> projectQuestions) {
    }

    private String writeScoreBreakdown(List<InterviewAnswerVO.ScoreDimensionVO> scoreBreakdown) {
        return writeJson(scoreBreakdown);
    }

    private String writeWeakPointTags(List<String> weakPointTags) {
        if (weakPointTags == null || weakPointTags.isEmpty()) {
            return null;
        }
        return String.join(",", weakPointTags);
    }

    private List<InterviewAnswerVO.ScoreDimensionVO> parseScoreBreakdown(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<InterviewAnswerVO.ScoreDimensionVO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse score breakdown json: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> parseWeakPointTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize interview metadata: {}", e.getMessage());
            return null;
        }
    }

    private record InterviewDeckSnapshot(
            String deckId,
            String deckTitle,
            int generatedCardCount,
            Map<Long, String> generatedCardIdsByRecordId) {
        private static InterviewDeckSnapshot empty() {
            return new InterviewDeckSnapshot(null, null, 0, Map.of());
        }
    }
}
