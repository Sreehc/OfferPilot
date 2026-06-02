package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("recording_transcript_segment")
@EqualsAndHashCode(callSuper = true)
public class RecordingTranscriptSegment extends BaseEntity {
    private Long sessionId;
    private Long userId;
    private Integer segmentIndex;
    private String transcriptText;
    private Integer startOffsetMs;
    private Integer endOffsetMs;
    private String signalType;
}
