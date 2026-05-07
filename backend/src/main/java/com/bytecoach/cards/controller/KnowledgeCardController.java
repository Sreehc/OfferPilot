package com.bytecoach.cards.controller;

import com.bytecoach.cards.dto.CardRateRequest;
import com.bytecoach.cards.dto.CardTaskCreateRequest;
import com.bytecoach.cards.service.KnowledgeCardService;
import com.bytecoach.cards.vo.KnowledgeCardTaskVO;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识卡片", description = "从知识文档生成卡片并按 SM-2 方式推进记忆任务")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class KnowledgeCardController {

    private final KnowledgeCardService knowledgeCardService;

    @Operation(summary = "创建知识卡片任务")
    @PostMapping("/task")
    public Result<KnowledgeCardTaskVO> createTask(@Valid @RequestBody CardTaskCreateRequest request) {
        return Result.success(knowledgeCardService.createTask(currentUserId(), request));
    }

    @Operation(summary = "获取当前活跃任务")
    @GetMapping("/active")
    public Result<KnowledgeCardTaskVO> activeTask() {
        return Result.success(knowledgeCardService.getActiveTask(currentUserId()));
    }

    @Operation(summary = "获取知识卡片任务详情")
    @GetMapping("/task/{id}")
    public Result<KnowledgeCardTaskVO> task(@Parameter(description = "任务 ID") @PathVariable Long id) {
        return Result.success(knowledgeCardService.getTask(currentUserId(), id));
    }

    @Operation(summary = "开始知识卡片任务")
    @PostMapping("/task/{id}/start")
    public Result<KnowledgeCardTaskVO> start(@Parameter(description = "任务 ID") @PathVariable Long id) {
        return Result.success(knowledgeCardService.startTask(currentUserId(), id));
    }

    @Operation(summary = "提交知识卡片评分")
    @PostMapping("/task/{id}/rate")
    public Result<KnowledgeCardTaskVO> rate(
            @Parameter(description = "任务 ID") @PathVariable Long id,
            @Valid @RequestBody CardRateRequest request) {
        return Result.success(knowledgeCardService.rate(currentUserId(), id, request));
    }

    @Operation(summary = "重新开始知识卡片任务")
    @PostMapping("/task/{id}/restart")
    public Result<KnowledgeCardTaskVO> restart(@Parameter(description = "任务 ID") @PathVariable Long id) {
        return Result.success(knowledgeCardService.restart(currentUserId(), id));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
