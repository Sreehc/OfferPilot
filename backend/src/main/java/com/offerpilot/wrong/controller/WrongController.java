package com.offerpilot.wrong.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.wrong.dto.WrongMasteryUpdateRequest;
import com.offerpilot.wrong.service.WrongService;
import com.offerpilot.wrong.vo.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "错题本", description = "错题管理与掌握状态追踪")
@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongController {

    private final WrongService wrongService;

    @Operation(summary = "错题列表", description = "分页查询当前用户的错题")
    @GetMapping("/list")
    public Result<PageResult<WrongQuestionVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(wrongService.list(currentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "错题详情")
    @GetMapping("/{id}")
    public Result<WrongQuestionVO> detail(@Parameter(description = "错题 ID") @PathVariable Long id) {
        return Result.success(wrongService.detail(currentUserId(), id));
    }

    @Operation(summary = "更新掌握状态", description = "切换错题的掌握状态（未开始/复习中/已掌握）")
    @PutMapping("/mastery/{id}")
    public Result<Void> updateMastery(@Parameter(description = "错题 ID") @PathVariable Long id,
                                      @Valid @RequestBody WrongMasteryUpdateRequest request) {
        wrongService.updateMastery(currentUserId(), id, request);
        return Result.success();
    }

    @Operation(summary = "删除错题")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "错题 ID") @PathVariable Long id) {
        wrongService.delete(currentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "导出错题为 Markdown")
    @GetMapping("/export")
    public void exportMarkdown(HttpServletResponse response) throws Exception {
        Long userId = currentUserId();
        List<WrongQuestionVO> allQuestions = wrongService.listAll(userId);

        response.setContentType("text/markdown");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"wrong-questions.md\"");

        PrintWriter writer = response.getWriter();
        writer.println("# OfferPilot 错题本");
        writer.println();
        writer.println("> 导出时间: " + java.time.LocalDateTime.now().toLocalDate());
        writer.println("> 共 " + allQuestions.size() + " 道错题");
        writer.println();

        String lastLevel = "";
        for (WrongQuestionVO q : allQuestions) {
            String level = q.getMasteryLevel();
            String levelLabel = switch (level) {
                case "not_started" -> "未开始";
                case "reviewing" -> "复习中";
                case "mastered" -> "已掌握";
                default -> level;
            };
            if (!level.equals(lastLevel)) {
                writer.println("## " + levelLabel);
                writer.println();
                lastLevel = level;
            }
            writer.println("### " + q.getTitle());
            writer.println();
            if (q.getStandardAnswer() != null && !q.getStandardAnswer().isBlank()) {
                writer.println("**标准答案：**");
                writer.println();
                writer.println(q.getStandardAnswer());
                writer.println();
            }
            if (q.getErrorReason() != null && !q.getErrorReason().isBlank()) {
                writer.println("**错误原因：** " + q.getErrorReason());
                writer.println();
            }
            writer.println("---");
            writer.println();
        }
        writer.flush();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
