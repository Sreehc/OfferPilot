package com.offerpilot.category.controller;

import com.offerpilot.category.dto.CategoryUpsertRequest;
import com.offerpilot.category.vo.CategoryVO;
import com.offerpilot.common.api.Result;
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

@Tag(name = "分类管理", description = "管理员操作：分类 CRUD")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final com.offerpilot.category.service.CategoryService categoryService;

    @Operation(summary = "新增分类")
    @PostMapping("/add")
    public Result<CategoryVO> add(@Valid @RequestBody CategoryUpsertRequest request) {
        return Result.success(categoryService.createCategory(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/update")
    public Result<CategoryVO> update(@Valid @RequestBody CategoryUpsertRequest request) {
        return Result.success(categoryService.updateCategory(request));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
