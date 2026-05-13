package com.offerpilot.community.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.community.dto.CommunityAnswerUpsertRequest;
import com.offerpilot.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/answers")
@RequiredArgsConstructor
public class CommunityAnswerController {

    private final CommunityService communityService;

    @PostMapping
    public Result<Long> submitAnswer(@Valid @RequestBody CommunityAnswerUpsertRequest request) {
        return Result.success(communityService.submitAnswer(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAnswer(@PathVariable Long id) {
        communityService.deleteAnswer(id);
        return Result.success();
    }

    @PostMapping("/{answerId}/accept")
    public Result<Void> acceptAnswer(
            @RequestParam Long questionId,
            @PathVariable Long answerId) {
        communityService.acceptAnswer(questionId, answerId);
        return Result.success();
    }
}
