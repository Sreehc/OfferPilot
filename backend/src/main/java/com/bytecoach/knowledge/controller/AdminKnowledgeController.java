package com.bytecoach.knowledge.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.knowledge.dto.KnowledgeImportRequest;
import com.bytecoach.knowledge.vo.KnowledgeDocVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库管理", description = "管理员操作：知识文档导入、切分、索引")
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final com.bytecoach.knowledge.service.KnowledgeService knowledgeService;

    @Operation(summary = "导入知识资料", description = "从内置种子数据导入知识文档")
    @PostMapping("/import")
    public Result<KnowledgeDocVO> importDoc(@Valid @RequestBody KnowledgeImportRequest request) {
        return Result.success(knowledgeService.importSeed(request));
    }

    @Operation(summary = "重新切分", description = "重新对文档进行 chunk 切分")
    @PostMapping("/rechunk/{docId}")
    public Result<KnowledgeDocVO> rechunk(@Parameter(description = "文档 ID") @PathVariable Long docId) {
        return Result.success(knowledgeService.rechunk(docId));
    }

    @Operation(summary = "重建索引", description = "重建文档的检索索引")
    @PostMapping("/reindex/{docId}")
    public Result<KnowledgeDocVO> reindex(@Parameter(description = "文档 ID") @PathVariable Long docId) {
        return Result.success(knowledgeService.reindex(docId));
    }
}
