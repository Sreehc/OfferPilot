package com.bytecoach.adaptive.controller;

import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.adaptive.vo.RecommendInterviewVO;
import com.bytecoach.adaptive.vo.RecommendQuestionsVO;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "智能推荐", description = "基于用户能力画像的自适应推荐")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final AdaptiveService adaptiveService;

    @Operation(summary = "能力画像", description = "获取用户的能力评估、薄弱分类、推荐难度")
    @GetMapping("/profile")
    public Result<AbilityProfileVO> profile() {
        return Result.success(adaptiveService.getAbilityProfile(currentUserId()));
    }

    @Operation(summary = "推荐题目", description = "返回个性化推荐题目列表（薄弱分类优先 + 适配难度 + 未做过）")
    @GetMapping("/questions")
    public Result<List<RecommendQuestionsVO>> questions(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(adaptiveService.getRecommendQuestions(currentUserId(), limit));
    }

    @Operation(summary = "推荐面试", description = "返回推荐面试方向和题目数量")
    @GetMapping("/interview")
    public Result<RecommendInterviewVO> interview() {
        return Result.success(adaptiveService.getRecommendInterview(currentUserId()));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
