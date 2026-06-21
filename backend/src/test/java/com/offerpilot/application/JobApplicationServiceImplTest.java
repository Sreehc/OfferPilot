package com.offerpilot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.entity.JobApplicationEvent;
import com.offerpilot.application.mapper.JobApplicationEventMapper;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.application.service.impl.JobApplicationServiceImpl;
import com.offerpilot.application.vo.JobApplicationVO;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private JobApplicationEventMapper jobApplicationEventMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;
    @Mock
    private TrainingSignalService trainingSignalService;

    @InjectMocks
    private JobApplicationServiceImpl service;

    @Test
    void board_returnsStableKanbanFieldsAndLatestEvent() {
        JobApplication application = new JobApplication();
        application.setId(101L);
        application.setUserId(1L);
        application.setCompany("字节跳动");
        application.setJobTitle("Java 后端工程师");
        application.setStatus("interview");
        application.setMatchScore(new BigDecimal("88.50"));
        application.setReviewSuggestion("系统设计案例还需要补量化指标。");
        application.setNextStepSuggestion("准备系统设计二面复盘。");

        JobApplicationEvent latest = new JobApplicationEvent();
        latest.setId(201L);
        latest.setApplicationId(101L);
        latest.setUserId(1L);
        latest.setEventType("interview");
        latest.setTitle("二面已约");
        latest.setContent("下周二下午系统设计面。");
        latest.setEventTime(LocalDateTime.of(2026, 6, 21, 10, 0));

        when(jobApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(jobApplicationEventMapper.selectList(any())).thenReturn(List.of(latest));

        List<JobApplicationVO> board = service.board(1L);

        assertEquals(1, board.size());
        JobApplicationVO item = board.get(0);
        assertEquals("INTERVIEW", item.getKanbanStatus());
        assertEquals("面试中", item.getStatusLabel());
        assertEquals("准备系统设计二面复盘。", item.getNextAction());
        assertEquals("88.5", item.getMatchScoreDisplay());
        assertNotNull(item.getLatestEvent());
        assertEquals("二面已约", item.getLatestEvent().getTitle());
    }
}
