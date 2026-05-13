package com.offerpilot.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommunityAnswerUpsertRequest {
    @NotNull(message = "问题ID不能为空")
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 10000, message = "内容最多10000字")
    private String content;
}
