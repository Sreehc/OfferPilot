package com.bytecoach.interview.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.cards.vo.TodayCardsTaskVO;
import com.bytecoach.interview.dto.InterviewAnswerRequest;
import com.bytecoach.interview.dto.InterviewStartRequest;
import com.bytecoach.interview.dto.VoiceStartRequest;
import com.bytecoach.interview.service.InterviewService;
import com.bytecoach.interview.service.InterviewVoiceService;
import com.bytecoach.interview.vo.InterviewAnswerVO;
import com.bytecoach.interview.vo.InterviewCurrentQuestionVO;
import com.bytecoach.interview.vo.InterviewDetailVO;
import com.bytecoach.interview.vo.InterviewHistoryVO;
import com.bytecoach.interview.vo.VoiceSubmitVO;
import com.bytecoach.security.util.SecurityUtils;
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
    private final InterviewVoiceService interviewVoiceService;

    @Operation(summary = "开始面试", description = "根据方向抽取题目并创建面试会话")
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

    @Operation(summary = "补生成面试复习卡片", description = "为指定已完成面试会话补生成面试诊断卡片")
    @PostMapping("/{sessionId}/cards/generate")
    public Result<InterviewDetailVO> generateCards(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewService.generateCards(currentUserId(), sessionId));
    }

    @Operation(summary = "加入今日卡片", description = "将面试诊断卡片 deck 设为当前 deck")
    @PostMapping("/{sessionId}/cards/activate")
    public Result<TodayCardsTaskVO> activateCards(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(interviewService.activateCards(currentUserId(), sessionId));
    }

    // ── Voice Interview Endpoints ──────────────────────────

    @Operation(summary = "语音面试状态", description = "检查语音面试功能是否可用")
    @GetMapping("/voice/status")
    public Result<Map<String, Object>> voiceStatus() {
        return Result.success(Map.of("available", interviewVoiceService.isVoiceAvailable()));
    }

    @Operation(summary = "开始语音面试", description = "创建语音面试会话，返回第一题")
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
            String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";
            return Result.success(interviewVoiceService.submitVoice(
                    currentUserId(), sessionId, questionId,
                    audioFile.getBytes(), mimeType));
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
