package com.offerpilot.knowledge.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.knowledge.dto.KnowledgeListQuery;
import com.offerpilot.knowledge.dto.KnowledgeSearchRequest;
import com.offerpilot.knowledge.service.KnowledgeService;
import com.offerpilot.knowledge.vo.KnowledgeDocVO;
import com.offerpilot.knowledge.vo.KnowledgeSearchVO;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "知识库", description = "知识文档管理与检索")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

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

    @Operation(summary = "上传文档", description = "上传 Markdown/TXT 文档到个人知识库，自动切分并入库")
    @PostMapping("/upload")
    public Result<KnowledgeDocVO> upload(
            @Parameter(description = "文件（支持 .md / .txt）") @RequestParam("file") MultipartFile file,
            @Parameter(description = "分类 ID（可选）") @RequestParam(value = "categoryId", required = false) Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(knowledgeService.upload(userId, file, categoryId));
    }

    @Operation(summary = "我的文档", description = "查询当前用户上传的文档列表")
    @GetMapping("/my")
    public Result<PageResult<KnowledgeDocVO>> myDocs(@ModelAttribute KnowledgeListQuery query) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(knowledgeService.listUserDocs(userId, query));
    }

    @Operation(summary = "删除文档", description = "删除用户自己上传的文档（级联删除 chunk 和向量）")
    @DeleteMapping("/{docId}")
    public Result<Void> delete(
            @Parameter(description = "文档 ID") @PathVariable Long docId) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeService.deleteUserDoc(userId, docId);
        return Result.success(null);
    }
}
