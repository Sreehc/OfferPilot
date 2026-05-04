package com.bytecoach.data.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExportRow {
    @ExcelProperty("ID")
    private Long id;

    @ExcelProperty("题目标题")
    private String title;

    @ExcelProperty("分类ID")
    private Long categoryId;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("标准答案")
    private String standardAnswer;

    @ExcelProperty("创建时间")
    private String createTime;
}
