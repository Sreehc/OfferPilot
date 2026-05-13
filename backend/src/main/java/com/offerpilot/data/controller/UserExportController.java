package com.offerpilot.data.controller;

import com.offerpilot.data.service.DataExportService;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人数据导出", description = "用户导出自己的学习数据")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class UserExportController {

    private final DataExportService dataExportService;

    @Operation(summary = "导出个人数据（面试记录、错题本、复习记录）")
    @GetMapping("/my-data")
    public void exportMyData(HttpServletResponse response) {
        Long userId = SecurityUtils.getCurrentUserId();
        dataExportService.exportMyData(userId, response);
    }
}
