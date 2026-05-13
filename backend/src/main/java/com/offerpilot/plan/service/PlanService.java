package com.offerpilot.plan.service;

import com.offerpilot.plan.dto.StudyPlanGenerateRequest;
import com.offerpilot.plan.dto.StudyPlanTaskStatusRequest;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;

public interface PlanService {
    StudyPlanCurrentVO generate(Long userId, StudyPlanGenerateRequest request);

    StudyPlanCurrentVO current(Long userId);

    StudyPlanCurrentVO updateTaskStatus(Long userId, Long taskId, StudyPlanTaskStatusRequest request);

    StudyPlanCurrentVO refresh(Long userId, Long planId);
}
