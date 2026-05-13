package com.offerpilot.question.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.question.dto.QuestionUpsertRequest;
import com.offerpilot.question.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题库管理", description = "管理员操作：题目 CRUD")
@RestController
@RequestMapping("/api/admin/question")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final com.offerpilot.question.service.QuestionService questionService;

    @Operation(summary = "新增题目")
    @PostMapping("/add")
    public Result<QuestionVO> add(@Valid @RequestBody QuestionUpsertRequest request) {
        return Result.success(questionService.createQuestion(request));
    }

    @Operation(summary = "更新题目")
    @PutMapping("/update")
    public Result<QuestionVO> update(@Valid @RequestBody QuestionUpsertRequest request) {
        return Result.success(questionService.updateQuestion(request));
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "题目 ID") @PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }
}
