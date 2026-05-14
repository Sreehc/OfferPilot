package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.dto.VoiceStartRequest;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.entity.VoiceRecord;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.interview.mapper.VoiceRecordMapper;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.service.InterviewVoiceService;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.VoiceSubmitVO;
import com.offerpilot.interview.voice.SttGateway;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewVoiceServiceImpl implements InterviewVoiceService {

    private final SttGateway sttGateway;
    private final InterviewService interviewService;
    private final InterviewServiceImpl interviewServiceImpl;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewRecordMapper recordMapper;
    private final VoiceRecordMapper voiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final FileStorageService fileStorageService;

    @Override
    public InterviewCurrentQuestionVO startVoice(Long userId, VoiceStartRequest request) {
        if (!isVoiceAvailable()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "语音面试功能未启用，请配置 STT 服务（offerpilot.stt.base-url 和 offerpilot.stt.api-key）");
        }

        // Reuse the existing start logic, then set mode to 'voice'
        InterviewStartRequest startRequest = new InterviewStartRequest();
        startRequest.setDirection(request.getDirection());
        startRequest.setJobRole(request.getJobRole());
        startRequest.setExperienceLevel(request.getExperienceLevel());
        startRequest.setTechStack(request.getTechStack());
        startRequest.setResumeId(request.getResumeId());
        startRequest.setProjectId(request.getProjectId());
        startRequest.setDurationMinutes(request.getDurationMinutes());
        startRequest.setIncludeResumeProject(request.getIncludeResumeProject());
        startRequest.setQuestionCount(request.getQuestionCount());
        startRequest.setReanswerQuestionId(request.getReanswerQuestionId());

        InterviewCurrentQuestionVO result = interviewService.start(userId, startRequest);

        // Update session mode to 'voice'
        InterviewSession session = sessionMapper.selectById(result.getSessionId());
        if (session != null) {
            session.setMode("voice");
            sessionMapper.updateById(session);
        }

        return result;
    }

    @Override
    @Transactional
    public VoiceSubmitVO submitVoice(Long userId, Long sessionId, Long questionId,
                                     byte[] audioData, String mimeType, String originalFilename) {
        // Validate session ownership
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "interview session not found");
        }
        if (!"in_progress".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "interview session is already finished");
        }
        if (!"voice".equals(session.getMode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "this is not a voice interview session");
        }

        // Find the current record
        InterviewRecord currentRecord = recordMapper.selectOne(
                new LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getSessionId, sessionId)
                        .eq(InterviewRecord::getQuestionId, questionId)
                        .last("LIMIT 1"));
        if (currentRecord == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not found in this session");
        }

        // Step 1: Transcribe audio via STT
        SttGateway.SttResult sttResult;
        try {
            sttResult = sttGateway.transcribe(audioData, mimeType);
        } catch (Exception e) {
            log.error("STT transcription failed for session {}: {}", sessionId, e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(),
                    "语音转录失败，请重试或切换到文字模式: " + e.getMessage());
        }

        String transcript = sttResult.text();
        if (transcript == null || transcript.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "语音转录结果为空，请重新录制或切换到文字模式");
        }

        log.info("Voice STT completed for session {}: text='{}', confidence={}, time={}ms",
                sessionId, transcript.length() > 50 ? transcript.substring(0, 50) + "..." : transcript,
                sttResult.confidence(), sttResult.processingTimeMs());

        StoredFile storedFile = fileStorageService.store(
                StorageDirectory.INTERVIEW_AUDIO,
                originalFilename,
                audioData,
                mimeType);

        // Step 2: Persist voice record
        VoiceRecord voiceRecord = new VoiceRecord();
        voiceRecord.setSessionId(sessionId);
        voiceRecord.setRecordId(currentRecord.getId());
        voiceRecord.setUserId(userId);
        voiceRecord.setAudioUrl(storedFile.getStorageKey());
        voiceRecord.setTranscript(transcript);
        voiceRecord.setTranscriptConfidence(sttResult.confidence());
        voiceRecord.setTranscriptTimeMs(sttResult.processingTimeMs());
        voiceRecordMapper.insert(voiceRecord);

        // Step 3: Use transcript as the answer and score via AI
        Question question = questionMapper.selectById(questionId);
        InterviewCurrentQuestionVO currentQuestion = interviewService.current(userId, sessionId);
        InterviewAnswerRequest answerRequest = new InterviewAnswerRequest();
        answerRequest.setSessionId(sessionId);
        answerRequest.setQuestionId(questionId);
        answerRequest.setAnswer(transcript);
        if (currentQuestion != null) {
            answerRequest.setQuestionTitle(currentQuestion.getQuestionTitle());
            answerRequest.setContextType(currentQuestion.getContextType());
            answerRequest.setContextSummary(currentQuestion.getContextSource() != null ? currentQuestion.getContextSource().getSummary() : null);
        } else if (question != null) {
            answerRequest.setQuestionTitle(question.getTitle());
        }
        if (question != null) {
            answerRequest.setStandardAnswer(question.getStandardAnswer());
            answerRequest.setScoreStandard(question.getScoreStandard());
        }
        answerRequest.setDirection(session.getDirection());
        answerRequest.setJobRole(session.getJobRole());
        answerRequest.setExperienceLevel(session.getExperienceLevel());
        answerRequest.setTechStack(session.getTechStack());
        answerRequest.setIncludeResumeProject(session.getIncludeResumeProject() != null && session.getIncludeResumeProject() == 1);

        boolean hasNext = session.getCurrentIndex() < session.getQuestionCount();

        // Call AI orchestrator (outside transaction — LLM call)
        InterviewAnswerVO aiResult = aiOrchestratorService.scoreInterviewAnswer(answerRequest);

        // Persist answer and advance session (reuse existing logic)
        var records = recordMapper.selectList(new LambdaQueryWrapper<InterviewRecord>()
                .eq(InterviewRecord::getSessionId, sessionId)
                .orderByAsc(InterviewRecord::getId));
        boolean addedToWrong = interviewServiceImpl.persistAnswerAndAdvance(
                userId, session, currentRecord, records, answerRequest, aiResult);

        return VoiceSubmitVO.builder()
                .transcript(transcript)
                .transcriptConfidence(sttResult.confidence())
                .transcriptTimeMs(sttResult.processingTimeMs())
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

    @Override
    public boolean isVoiceAvailable() {
        return sttGateway.isAvailable();
    }
}
