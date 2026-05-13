package com.offerpilot.category.controller;

import com.offerpilot.category.dto.CategoryQuery;
import com.offerpilot.category.vo.CategoryVO;
import com.offerpilot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "分类查询", description = "公开分类列表查询")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final com.offerpilot.category.service.CategoryService categoryService;

    @Operation(summary = "分类列表", description = "按类型查询分类列表")
    @GetMapping("/list")
    public Result<List<CategoryVO>> list(@ModelAttribute CategoryQuery query) {
        return Result.success(categoryService.listCategories(query));
    }
}
