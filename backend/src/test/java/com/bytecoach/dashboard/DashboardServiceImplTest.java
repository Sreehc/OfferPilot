package com.bytecoach.dashboard;

import com.bytecoach.dashboard.dto.DashboardOverviewVO;
import com.bytecoach.dashboard.dto.RecentInterviewVO;
import com.bytecoach.dashboard.dto.WeakPointVO;
import com.bytecoach.dashboard.mapper.DashboardMetricsMapper;
import com.bytecoach.dashboard.service.impl.DashboardServiceImpl;
import com.bytecoach.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void overview_emptyData_firstVisit() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.planCompletionRate(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(null);

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(0, result.getLearningCount());
            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getWrongCount());
            assertEquals(0, result.getPlanCompletionRate());
            assertTrue(result.getFirstVisit());
            assertTrue(result.getRecentInterviews().isEmpty());
            assertTrue(result.getWeakPoints().isEmpty());
        }
    }

    @Test
    void overview_hasData_notFirstVisit() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(5L);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(3L);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(new BigDecimal("78.50"));
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(10L);
            when(dashboardMetricsMapper.planCompletionRate(1L)).thenReturn(40);

            RecentInterviewVO recent = RecentInterviewVO.builder()
                    .sessionId(1L).direction("Java Backend").totalScore(new BigDecimal("80")).status("finished").build();
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(List.of(recent));

            WeakPointVO weak = WeakPointVO.builder()
                    .categoryName("JVM").wrongCount(5L).score(new BigDecimal("60")).build();
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(List.of(weak));

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(8, result.getLearningCount());
            assertEquals(new BigDecimal("78.50"), result.getAverageScore());
            assertEquals(10, result.getWrongCount());
            assertEquals(40, result.getPlanCompletionRate());
            assertFalse(result.getFirstVisit());
            assertEquals(1, result.getRecentInterviews().size());
            assertEquals(1, result.getWeakPoints().size());
        }
    }

    @Test
    void overview_noUserId_throws() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            assertThrows(Exception.class, () -> dashboardService.overview());
        }
    }
}
