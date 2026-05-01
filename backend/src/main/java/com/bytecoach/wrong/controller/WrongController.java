package com.bytecoach.wrong.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import com.bytecoach.wrong.dto.WrongMasteryUpdateRequest;
import com.bytecoach.wrong.service.WrongService;
import com.bytecoach.wrong.vo.WrongQuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongController {

    private final WrongService wrongService;

    @GetMapping("/list")
    public Result<PageResult<WrongQuestionVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(wrongService.list(currentUserId(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<WrongQuestionVO> detail(@PathVariable Long id) {
        return Result.success(wrongService.detail(currentUserId(), id));
    }

    @PutMapping("/mastery/{id}")
    public Result<Void> updateMastery(@PathVariable Long id,
                                      @Valid @RequestBody WrongMasteryUpdateRequest request) {
        wrongService.updateMastery(currentUserId(), id, request);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        wrongService.delete(currentUserId(), id);
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
