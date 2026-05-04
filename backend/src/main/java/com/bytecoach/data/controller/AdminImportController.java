package com.bytecoach.data.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.data.dto.ImportResultVO;
import com.bytecoach.data.service.DataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "数据导入", description = "管理员批量导入数据")
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class AdminImportController {

    private final DataImportService dataImportService;

    @Operation(summary = "Excel 批量导入题目")
    @PostMapping("/questions")
    public Result<ImportResultVO> importQuestions(@RequestParam("file") MultipartFile file) {
        return Result.success(dataImportService.importQuestions(file));
    }
}
