package com.bytecoach.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.ai.service.AiOrchestratorService;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.config.ByteCoachProperties;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.dashboard.service.DashboardService;
import com.bytecoach.interview.dto.InterviewAnswerRequest;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.interview.dto.InterviewStartRequest;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.interview.mapper.VoiceRecordMapper;
import com.bytecoach.interview.service.InterviewService;
import com.bytecoach.interview.entity.VoiceRecord;
import com.bytecoach.interview.vo.InterviewAnswerVO;
import com.bytecoach.interview.vo.InterviewCurrentQuestionVO;
import com.bytecoach.interview.vo.InterviewDetailVO;
import com.bytecoach.interview.vo.InterviewHistoryVO;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewRecordMapper recordMapper;
    private final VoiceRecordMapper voiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CategoryService categoryService;
    private final AiOrchestratorService aiOrchestratorService;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final ByteCoachProperties props;

    @Lazy
    @Autowired
    private InterviewServiceImpl self;

    @Override
    @Transactional
    public InterviewCurrentQuestionVO start(Long userId, InterviewStartRequest request) {
        List<Question> questions = pickQuestions(request.getDirection(), request.getQuestionCount());

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
                .questionTitle(first.getTitle())
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
        String title = question != null ? question.getTitle() : "Unknown";

        return InterviewCurrentQuestionVO.builder()
                .sessionId(sessionId)
                .currentIndex(session.getCurrentIndex())
                .questionCount(session.getQuestionCount())
                .questionId(record.getQuestionId())
                .questionTitle(title)
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
        InterviewRecord currentRecord = records.stream()
                .filter(r -> r.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not in this session"));

        Question question = questionMapper.selectById(request.getQuestionId());
        if (question != null) {
            request.setQuestionTitle(question.getTitle());
            request.setStandardAnswer(question.getStandardAnswer());
            request.setScoreStandard(question.getScoreStandard());
        }

        // Phase 2: call LLM outside any transaction
        InterviewAnswerVO aiResult = aiOrchestratorService.scoreInterviewAnswer(request);

        // Phase 3: persist result and advance session (in transaction)
        boolean addedToWrong = self.persistAnswerAndAdvance(userId, session, currentRecord, records, request, aiResult);

        int currentIndex = session.getCurrentIndex();
        boolean hasNext = currentIndex < session.getQuestionCount();

        return InterviewAnswerVO.builder()
                .score(aiResult.getScore())
                .comment(aiResult.getComment())
                .standardAnswer(aiResult.getStandardAnswer())
                .followUp(aiResult.getFollowUp())
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

        List<Long> questionIds = records.stream()
                .map(InterviewRecord::getQuestionId)
                .distinct()
                .toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        // Load voice records if this is a voice session
        Map<Long, VoiceRecord> voiceRecordMap = Map.of();
        if ("voice".equals(session.getMode())) {
            List<VoiceRecord> voiceRecords = voiceRecordMapper.selectList(
                    new LambdaQueryWrapper<VoiceRecord>()
                            .eq(VoiceRecord::getSessionId, sessionId));
            voiceRecordMap = voiceRecords.stream()
                    .collect(Collectors.toMap(VoiceRecord::getRecordId, Function.identity(), (a, b) -> a));
        }

        List<InterviewDetailVO.InterviewRecordVO> recordVOs = records.stream()
                .map(record -> {
                    Question q = questionMap.get(record.getQuestionId());
                    VoiceRecord vr = voiceRecordMap.get(record.getId());
                    return InterviewDetailVO.InterviewRecordVO.builder()
                            .questionId(record.getQuestionId())
                            .questionTitle(q != null ? q.getTitle() : "Unknown")
                            .userAnswer(record.getUserAnswer())
                            .score(record.getScore())
                            .comment(record.getComment())
                            .standardAnswer(q != null ? q.getStandardAnswer() : null)
                            .followUp(record.getFollowUp())
                            .voiceTranscript(vr != null ? vr.getTranscript() : null)
                            .voiceConfidence(vr != null ? vr.getTranscriptConfidence() : null)
                            .build();
                })
                .toList();

        return InterviewDetailVO.builder()
                .sessionId(sessionId)
                .direction(session.getDirection())
                .status(session.getStatus())
                .mode(session.getMode())
                .totalScore(session.getTotalScore())
                .questionCount(session.getQuestionCount())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
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

        List<InterviewHistoryVO> records = page.getRecords().stream()
                .map(s -> InterviewHistoryVO.builder()
                        .sessionId(s.getId())
                        .direction(s.getDirection())
                        .status(s.getStatus())
                        .mode(s.getMode())
                        .totalScore(s.getTotalScore())
                        .questionCount(s.getQuestionCount())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
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
                        .status(s.getStatus())
                        .mode(s.getMode())
                        .totalScore(s.getTotalScore())
                        .questionCount(s.getQuestionCount())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    private List<Question> pickQuestions(String direction, int count) {
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

        if (pool.isEmpty()) {
            // Last resort: grab any questions
            pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .last("LIMIT 20"));
        }

        Collections.shuffle(pool);
        return pool.stream().limit(count).toList();
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
        wrongQuestionMapper.insert(wrong);
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
}
