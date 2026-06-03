package com.offerpilot.plan.service;

import com.offerpilot.plan.dto.StudyPlanGenerateRequest;
import com.offerpilot.plan.dto.StudyPlanTaskStatusRequest;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;

public interface PlanService {
    StudyPlanCurrentVO generate(Long userId, StudyPlanGenerateRequest request);

    StudyPlanCurrentVO current(Long userId);

    StudyPlanCurrentVO updateTaskStatus(Long userId, Long taskId, StudyPlanTaskStatusRequest request);

    StudyPlanCurrentVO refresh(Long userId, Long planId);

    StudyPlanCurrentVO saveInterviewReviewAction(Long userId, Long interviewSessionId, Long copilotRealtimeSessionId,
                                                 String focusDirection, String targetRole, String techStack,
                                                 String taskTitle, String taskDescription, String actionPath);

    StudyPlanCurrentVO saveRecordingReviewAction(Long userId, Long recordingReviewSessionId,
                                                 String focusDirection, String targetRole, String techStack,
                                                 String taskTitle, String taskDescription, String actionPath);

    StudyPlanCurrentVO saveTopicRetrospectiveAction(Long userId, Long categoryId,
                                                    String focusDirection, String targetRole, String techStack,
                                                    String taskTitle, String taskDescription, String actionPath);
}
