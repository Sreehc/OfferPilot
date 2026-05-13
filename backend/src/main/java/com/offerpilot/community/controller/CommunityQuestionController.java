package com.offerpilot.community.controller;

import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.api.Result;
import com.offerpilot.community.dto.CommunityQuestionUpsertRequest;
import com.offerpilot.community.service.CommunityService;
import com.offerpilot.community.service.UserStatsService;
import com.offerpilot.community.vo.CommunityQuestionVO;
import com.offerpilot.community.vo.LeaderboardEntryVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityQuestionController {

    private final CommunityService communityService;
    private final UserStatsService userStatsService;

    @PostMapping("/questions")
    public Result<Long> createQuestion(@Valid @RequestBody CommunityQuestionUpsertRequest request) {
        return Result.success(communityService.createQuestion(request));
    }

    @PutMapping("/questions")
    public Result<Void> updateQuestion(@Valid @RequestBody CommunityQuestionUpsertRequest request) {
        communityService.updateQuestion(request);
        return Result.success();
    }

    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        communityService.deleteQuestion(id);
        return Result.success();
    }

    @GetMapping("/questions/{id}")
    public Result<CommunityQuestionVO> getQuestion(@PathVariable Long id) {
        return Result.success(communityService.getQuestionDetail(id));
    }

    @GetMapping("/questions")
    public Result<PageResult<CommunityQuestionVO>> listQuestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(communityService.listQuestions(page, size, sort, categoryId, keyword));
    }

    @PostMapping("/vote")
    public Result<Void> vote(@RequestBody @Valid com.offerpilot.community.dto.CommunityVoteRequest request) {
        communityService.vote(request);
        return Result.success();
    }

    @GetMapping("/leaderboard")
    public Result<List<LeaderboardEntryVO>> getLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(communityService.getLeaderboard(limit));
    }
}
