package com.offerpilot.favorite.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.favorite.dto.FavoriteBatchDeleteRequest;
import com.offerpilot.favorite.dto.FavoriteTagUpsertRequest;
import com.offerpilot.favorite.dto.FavoriteUpsertRequest;
import com.offerpilot.favorite.service.FavoriteService;
import com.offerpilot.favorite.vo.FavoriteStatsVO;
import com.offerpilot.favorite.vo.FavoriteTagVO;
import com.offerpilot.favorite.vo.FavoriteVO;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏", description = "收藏管理与分组")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏列表")
    @GetMapping
    public Result<PageResult<FavoriteVO>> list(
            @Parameter(description = "目标类型") @RequestParam(required = false) String targetType,
            @Parameter(description = "分组 ID") @RequestParam(required = false) Long tagId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(favoriteService.list(currentUserId(), targetType, tagId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "收藏统计")
    @GetMapping("/stats")
    public Result<FavoriteStatsVO> stats() {
        return Result.success(favoriteService.stats(currentUserId()));
    }

    @Operation(summary = "添加收藏")
    @PostMapping
    public Result<FavoriteVO> add(@Valid @RequestBody FavoriteUpsertRequest request) {
        return Result.success(favoriteService.add(currentUserId(), request));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@Parameter(description = "收藏 ID") @PathVariable Long id) {
        favoriteService.remove(currentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "批量取消收藏")
    @PostMapping("/batch-delete")
    public Result<Void> batchRemove(@Valid @RequestBody FavoriteBatchDeleteRequest request) {
        favoriteService.batchRemove(currentUserId(), request.getIds());
        return Result.success();
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check")
    public Result<Boolean> check(
            @Parameter(description = "目标类型") @RequestParam String targetType,
            @Parameter(description = "目标 ID") @RequestParam Long targetId) {
        return Result.success(favoriteService.isFavorited(currentUserId(), targetType, targetId));
    }

    @Operation(summary = "收藏分组列表")
    @GetMapping("/tags")
    public Result<List<FavoriteTagVO>> listTags() {
        return Result.success(favoriteService.listTags(currentUserId()));
    }

    @Operation(summary = "创建收藏分组")
    @PostMapping("/tags")
    public Result<FavoriteTagVO> createTag(@Valid @RequestBody FavoriteTagUpsertRequest request) {
        return Result.success(favoriteService.createTag(currentUserId(), request));
    }

    @Operation(summary = "删除收藏分组")
    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(@Parameter(description = "分组 ID") @PathVariable Long tagId) {
        favoriteService.deleteTag(currentUserId(), tagId);
        return Result.success();
    }

    @Operation(summary = "修改收藏分组")
    @PutMapping("/{id}/tag")
    public Result<Void> updateFavoriteTag(
            @Parameter(description = "收藏 ID") @PathVariable Long id,
            @Parameter(description = "分组 ID") @RequestParam(required = false) Long tagId) {
        favoriteService.updateFavoriteTag(currentUserId(), id, tagId);
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
