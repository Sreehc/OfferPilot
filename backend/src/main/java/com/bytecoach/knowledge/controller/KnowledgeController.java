package com.bytecoach.knowledge.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.knowledge.dto.KnowledgeListQuery;
import com.bytecoach.knowledge.dto.KnowledgeSearchRequest;
import com.bytecoach.knowledge.vo.KnowledgeDocVO;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库", description = "知识文档管理与检索")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final com.bytecoach.knowledge.service.KnowledgeService knowledgeService;

    @Operation(summary = "文档列表", description = "分页查询知识文档")
    @GetMapping("/list")
    public Result<PageResult<KnowledgeDocVO>> list(@ModelAttribute KnowledgeListQuery query) {
        return Result.success(knowledgeService.listDocs(query));
    }

    @Operation(summary = "知识检索", description = "根据关键词检索知识库片段")
    @PostMapping("/search")
    public Result<KnowledgeSearchVO> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.success(knowledgeService.search(request));
    }
}
