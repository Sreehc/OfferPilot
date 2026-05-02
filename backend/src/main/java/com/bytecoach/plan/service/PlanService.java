package com.bytecoach.plan.service;

import com.bytecoach.plan.dto.GeneratePlanRequest;
import com.bytecoach.plan.dto.PlanTaskStatusRequest;
import com.bytecoach.plan.vo.StudyPlanTaskVO;
import com.bytecoach.plan.vo.StudyPlanVO;
import java.util.List;

public interface PlanService {

    StudyPlanVO generate(Long userId, GeneratePlanRequest request);

    StudyPlanVO current(Long userId);

    List<StudyPlanTaskVO> tasks(Long userId, Long planId);

    void updateTaskStatus(Long userId, Long taskId, PlanTaskStatusRequest request);

    /**
     * Get all plan versions for the user (newest first).
     */
    List<StudyPlanVO> history(Long userId);
}
