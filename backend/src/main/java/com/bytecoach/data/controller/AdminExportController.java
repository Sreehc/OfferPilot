package com.bytecoach.data.controller;

import com.bytecoach.data.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据导出", description = "管理员导出题库和用户列表")
@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final DataExportService dataExportService;

    @Operation(summary = "导出题库为 Excel")
    @GetMapping("/questions")
    public void exportQuestions(HttpServletResponse response) {
        dataExportService.exportQuestions(response);
    }

    @Operation(summary = "导出用户列表为 Excel")
    @GetMapping("/users")
    public void exportUsers(HttpServletResponse response) {
        dataExportService.exportUsers(response);
    }
}
