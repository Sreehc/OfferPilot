package com.offerpilot.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerpilot.admin.dto.AdminInterviewGovernanceSummaryVO;
import com.offerpilot.admin.dto.AdminInterviewGovernanceVO;
import com.offerpilot.admin.service.AdminInterviewGovernanceService;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminInterviewGovernanceServiceImpl implements AdminInterviewGovernanceService {

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewRecordMapper interviewRecordMapper;

    @Override
    public PageResult<AdminInterviewGovernanceVO> list(String status, String mode, String keyword, int pageNum, int pageSize) {
        Page<InterviewSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InterviewSession> queryWrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(StringUtils.hasText(status), InterviewSession::getStatus, status)
                .eq(StringUtils.hasText(mode), InterviewSession::getMode, mode)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(InterviewSession::getDirection, keyword)
                        .or()
                        .like(InterviewSession::getJobRole, keyword)
                        .or()
                        .like(InterviewSession::getTechStack, keyword))
                .orderByDesc(InterviewSession::getUpdateTime);
        IPage<InterviewSession> result = interviewSessionMapper.selectPage(page, queryWrapper);
        List<Long> sessionIds = result.getRecords().stream().map(InterviewSession::getId).toList();
        Map<Long, Integer> lowScoreCountMap = sessionIds.isEmpty()
                ? Map.of()
                : interviewRecordMapper.selectList(new LambdaQueryWrapper<InterviewRecord>()
                                .in(InterviewRecord::getSessionId, sessionIds)
                                .isNotNull(InterviewRecord::getScore)
                                .lt(InterviewRecord::getScore, new BigDecimal("60")))
                        .stream()
                        .collect(Collectors.groupingBy(InterviewRecord::getSessionId, Collectors.summingInt(item -> 1)));

        List<AdminInterviewGovernanceVO> records = result.getRecords().stream()
                .map(session -> AdminInterviewGovernanceVO.builder()
                        .sessionId(session.getId())
                        .userId(session.getUserId())
                        .direction(session.getDirection())
                        .jobRole(session.getJobRole())
                        .experienceLevel(session.getExperienceLevel())
                        .techStack(session.getTechStack())
                        .mode(session.getMode())
                        .status(session.getStatus())
                        .totalScore(session.getTotalScore())
                        .questionCount(session.getQuestionCount())
                        .durationMinutes(session.getDurationMinutes())
                        .includeResumeProject(session.getIncludeResumeProject() != null && session.getIncludeResumeProject() == 1)
                        .lowScoreCount(lowScoreCountMap.getOrDefault(session.getId(), 0))
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .build())
                .toList();
        return PageResult.<AdminInterviewGovernanceVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) result.getPages())
                .build();
    }

    @Override
    public AdminInterviewGovernanceSummaryVO summary() {
        long total = interviewSessionMapper.selectCount(null);
        long finished = interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getStatus, "finished"));
        long voice = interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getMode, "voice"));
        long resumeProject = interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getIncludeResumeProject, 1));
        List<InterviewSession> scoredSessions = interviewSessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                .isNotNull(InterviewSession::getTotalScore)
                .orderByDesc(InterviewSession::getUpdateTime)
                .last("LIMIT 200"));
        BigDecimal averageScore = scoredSessions.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(scoredSessions.stream()
                                .map(InterviewSession::getTotalScore)
                                .filter(java.util.Objects::nonNull)
                                .mapToDouble(BigDecimal::doubleValue)
                                .average()
                                .orElse(0))
                        .setScale(2, RoundingMode.HALF_UP);
        return AdminInterviewGovernanceSummaryVO.builder()
                .totalSessions(total)
                .finishedSessions(finished)
                .voiceSessions(voice)
                .resumeProjectSessions(resumeProject)
                .averageScore(averageScore)
                .build();
    }
}
