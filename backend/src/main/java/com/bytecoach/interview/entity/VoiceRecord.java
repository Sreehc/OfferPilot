package com.bytecoach.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("voice_record")
@EqualsAndHashCode(callSuper = true)
public class VoiceRecord extends BaseEntity {
    private Long sessionId;
    private Long recordId;
    private Long userId;
    private String audioUrl;
    private String transcript;
    private BigDecimal transcriptConfidence;
    private Integer transcriptTimeMs;
}
