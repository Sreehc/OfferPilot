package com.offerpilot.resume.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.resume.service.ResumeService;
import com.offerpilot.resume.vo.ResumeFileVO;
import com.offerpilot.resume.vo.ResumeProjectVO;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "简历助手", description = "简历上传、解析、项目追问与面试简历输出")
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传简历")
    @PostMapping("/upload")
    public Result<ResumeFileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(resumeService.upload(currentUserId(), file));
    }

    @Operation(summary = "简历列表")
    @GetMapping("/list")
    public Result<List<ResumeFileVO>> list() {
        return Result.success(resumeService.list(currentUserId()));
    }

    @Operation(summary = "最新简历")
    @GetMapping("/latest")
    public Result<ResumeFileVO> latest() {
        return Result.success(resumeService.latest(currentUserId()));
    }

    @Operation(summary = "简历详情")
    @GetMapping("/{resumeId}")
    public Result<ResumeFileVO> detail(@Parameter(description = "简历 ID") @PathVariable Long resumeId) {
        return Result.success(resumeService.detail(currentUserId(), resumeId));
    }

    @Operation(summary = "项目追问")
    @GetMapping("/{resumeId}/project-questions")
    public Result<List<ResumeProjectVO>> projectQuestions(@Parameter(description = "简历 ID") @PathVariable Long resumeId) {
        return Result.success(resumeService.projectQuestions(currentUserId(), resumeId));
    }

    @Operation(summary = "自我介绍")
    @GetMapping("/{resumeId}/intro")
    public Result<Map<String, String>> intro(@Parameter(description = "简历 ID") @PathVariable Long resumeId) {
        return Result.success(Map.of("content", resumeService.selfIntro(currentUserId(), resumeId)));
    }

    @Operation(summary = "面试简历输出")
    @GetMapping("/{resumeId}/interview-resume")
    public Result<Map<String, String>> interviewResume(@Parameter(description = "简历 ID") @PathVariable Long resumeId) {
        return Result.success(Map.of("content", resumeService.interviewResume(currentUserId(), resumeId)));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
