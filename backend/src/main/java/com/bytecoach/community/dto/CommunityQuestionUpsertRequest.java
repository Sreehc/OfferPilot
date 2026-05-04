package com.bytecoach.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommunityQuestionUpsertRequest {
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容最多10000字")
    private String content;

    private Long categoryId;
}
