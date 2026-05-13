package com.offerpilot.question.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.question.dto.QuestionQuery;
import com.offerpilot.question.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题库", description = "题目查询")
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final com.offerpilot.question.service.QuestionService questionService;

    @Operation(summary = "题目列表", description = "分页查询题库")
    @GetMapping("/list")
    public Result<PageResult<QuestionVO>> list(@ModelAttribute QuestionQuery query) {
        return Result.success(questionService.listQuestions(query));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@Parameter(description = "题目 ID") @PathVariable Long id) {
        return Result.success(questionService.getQuestionDetail(id));
    }
}
