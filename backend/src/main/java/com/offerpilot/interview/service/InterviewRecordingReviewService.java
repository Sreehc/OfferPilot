package com.offerpilot.interview.service;

import com.offerpilot.interview.vo.RecordingReviewSessionVO;

public interface InterviewRecordingReviewService {

    RecordingReviewSessionVO createReview(Long userId, String direction, String jobRole, String notes,
                                          String transcriptText, byte[] audioData, String mimeType,
                                          String originalFilename);

    RecordingReviewSessionVO detail(Long userId, Long sessionId);

    RecordingReviewSessionVO latest(Long userId);
}
