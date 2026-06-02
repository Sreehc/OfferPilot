package com.offerpilot.interview.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.dto.CopilotPrepSessionCreateRequest;
import com.offerpilot.interview.dto.JobPrepSessionCreateRequest;
import com.offerpilot.interview.service.InterviewCopilotPrepService;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.dto.VoiceStartRequest;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.service.InterviewVoiceService;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import com.offerpilot.interview.vo.VoiceSubmitVO;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "模拟面试", description = "AI 驱动的模拟面试与评分")
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewCopilotPrepService interviewCopilotPrepService;
    private final InterviewJobPrepService interviewJobPrepService;
    private final InterviewRecordingReviewService interviewRecordingReviewService;
    private final InterviewVoiceService interviewVoiceService;
    private final UploadPolicyService uploadPolicyService;

    @Operation(summary = "开始面试", description = "根据方向抽取题目并创建面试会话，支持绑定简历或项目上下文")
    @PostMapping("/start")
    public Result<InterviewCurrentQuestionVO> start(@Valid @RequestBody InterviewStartRequest request) {
        return Result.success(interviewService.start(currentUserId(), request));
    }

    @Operation(summary = "当前题目", description = "获取面试会话的当前题目")
    @GetMapping("/current/{sessionId}")
    public Result<InterviewCurrentQuestionVO> current(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewService.current(currentUserId(), sessionId));
    }

    @Operation(summary = "提交答案", description = "提交答案并获取 AI 评分")
    @PostMapping("/answer")
    public Result<InterviewAnswerVO> answer(@Valid @RequestBody InterviewAnswerRequest request) {
        return Result.success(interviewService.answer(currentUserId(), request));
    }

    @Operation(summary = "面试详情", description = "查看面试会话的所有题目和评分")
    @GetMapping("/detail/{sessionId}")
    public Result<InterviewDetailVO> detail(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewService.detail(currentUserId(), sessionId));
    }

    @Operation(summary = "面试历史", description = "分页查看面试历史记录，可按方向筛选")
    @GetMapping("/history")
    public Result<PageResult<InterviewHistoryVO>> history(
            @Parameter(description = "方向筛选") @RequestParam(required = false) String direction,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(interviewService.history(currentUserId(), direction, pageNum, pageSize));
    }

    @Operation(summary = "面试趋势", description = "获取最近面试成绩趋势数据（用于折线图）")
    @GetMapping("/trend")
    public Result<List<InterviewHistoryVO>> trend(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(interviewService.trendData(currentUserId(), limit));
    }

    @Operation(summary = "创建 JD 备面会话", description = "根据 JD、简历和投递信息生成一份定向备面结果")
    @PostMapping("/job-prep/sessions")
    public Result<JobPrepSessionVO> createJobPrepSession(@Valid @RequestBody JobPrepSessionCreateRequest request) {
        return Result.success(interviewJobPrepService.createSession(currentUserId(), request));
    }

    @Operation(summary = "JD 备面详情", description = "查看已生成的 JD 备面会话内容")
    @GetMapping("/job-prep/sessions/{sessionId}")
    public Result<JobPrepSessionVO> jobPrepSession(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewJobPrepService.detail(currentUserId(), sessionId));
    }

    @Operation(summary = "创建 Copilot Prep 会话", description = "根据 JD、简历、JD 备面和 provider readiness 生成会前 Prep 结果")
    @PostMapping("/copilot/prep-sessions")
    public Result<CopilotPrepSessionVO> createCopilotPrepSession(@Valid @RequestBody CopilotPrepSessionCreateRequest request) {
        return Result.success(interviewCopilotPrepService.createSession(currentUserId(), request));
    }

    @Operation(summary = "Copilot Prep 详情", description = "查看实时 Copilot 会前准备结果")
    @GetMapping("/copilot/prep-sessions/{sessionId}")
    public Result<CopilotPrepSessionVO> copilotPrepSession(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewCopilotPrepService.detail(currentUserId(), sessionId));
    }

    @Operation(summary = "创建录音复盘", description = "上传真实面试录音并生成转写与结构化复盘结果")
    @PostMapping("/recording-reviews")
    public Result<RecordingReviewSessionVO> createRecordingReview(
            @Parameter(description = "面试方向") @RequestParam(required = false) String direction,
            @Parameter(description = "目标岗位") @RequestParam(required = false) String jobRole,
            @Parameter(description = "场景备注") @RequestParam(required = false) String notes,
            @Parameter(description = "录音文件") @RequestParam("audio") MultipartFile audioFile) {
        try {
            uploadPolicyService.validate(
                    StorageDirectory.INTERVIEW_AUDIO,
                    audioFile.getOriginalFilename(),
                    audioFile.getContentType(),
                    audioFile.getSize());
            String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";
            return Result.success(interviewRecordingReviewService.createReview(
                    currentUserId(),
                    direction,
                    jobRole,
                    notes,
                    audioFile.getBytes(),
                    mimeType,
                    audioFile.getOriginalFilename()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "录音复盘处理失败: " + e.getMessage());
        }
    }

    @Operation(summary = "录音复盘详情", description = "查看录音复盘的转写、片段与建议动作")
    @GetMapping("/recording-reviews/{sessionId}")
    public Result<RecordingReviewSessionVO> recordingReview(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewRecordingReviewService.detail(currentUserId(), sessionId));
    }

    // ── Voice Interview Endpoints ──────────────────────────

    @Operation(summary = "语音面试状态", description = "检查语音面试功能是否可用")
    @GetMapping("/voice/status")
    public Result<Map<String, Object>> voiceStatus() {
        return Result.success(Map.of("available", interviewVoiceService.isVoiceAvailable()));
    }

    @Operation(summary = "开始语音面试", description = "创建语音面试会话，返回第一题，支持绑定简历或项目上下文")
    @PostMapping("/voice/start")
    public Result<InterviewCurrentQuestionVO> voiceStart(@Valid @RequestBody VoiceStartRequest request) {
        return Result.success(interviewVoiceService.startVoice(currentUserId(), request));
    }

    @Operation(summary = "提交语音答案", description = "上传音频文件，自动转录并评分")
    @PostMapping("/voice/submit")
    public Result<VoiceSubmitVO> voiceSubmit(
            @Parameter(description = "面试会话 ID") @RequestParam Long sessionId,
            @Parameter(description = "题目 ID") @RequestParam Long questionId,
            @Parameter(description = "音频文件") @RequestParam("audio") MultipartFile audioFile) {
        try {
            uploadPolicyService.validate(
                    StorageDirectory.INTERVIEW_AUDIO,
                    audioFile.getOriginalFilename(),
                    audioFile.getContentType(),
                    audioFile.getSize());
            String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";
            return Result.success(interviewVoiceService.submitVoice(
                    currentUserId(), sessionId, questionId,
                    audioFile.getBytes(), mimeType, audioFile.getOriginalFilename()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(),
                    "音频处理失败: " + e.getMessage());
        }
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
