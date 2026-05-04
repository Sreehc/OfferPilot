package com.bytecoach.data.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionImportRow {
    @ExcelProperty("题目标题")
    private String title;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("标准答案")
    private String standardAnswer;
}
