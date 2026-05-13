package com.offerpilot.data.dto;

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

    @ExcelProperty("题目类型")
    private String type;

    @ExcelProperty("高频度")
    private Integer frequency;

    @ExcelProperty("岗位方向")
    private String jobDirection;

    @ExcelProperty("适用范围")
    private String applicableScope;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("标准答案")
    private String standardAnswer;

    @ExcelProperty("面试版回答")
    private String interviewAnswer;

    @ExcelProperty("追问建议")
    private String followUpSuggestions;

    @ExcelProperty("常见错误回答")
    private String commonMistakes;

    @ExcelProperty("创建时间")
    private String createTime;
}
