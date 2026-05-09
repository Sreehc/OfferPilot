package com.bytecoach.dashboard;

import com.bytecoach.cards.entity.KnowledgeCardTask;
import com.bytecoach.cards.mapper.KnowledgeCardTaskMapper;
import com.bytecoach.dashboard.dto.DashboardOverviewVO;
import com.bytecoach.dashboard.dto.RecentInterviewVO;
import com.bytecoach.dashboard.dto.WeakPointVO;
import com.bytecoach.dashboard.mapper.DashboardMetricsMapper;
import com.bytecoach.dashboard.service.impl.DashboardServiceImpl;
import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.common.config.ByteCoachProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bytecoach.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AdaptiveService adaptiveService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private ByteCoachProperties props;
    @Mock
    private KnowledgeCardTaskMapper knowledgeCardTaskMapper;

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
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectLatestCardTaskId(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(null);

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(0, result.getLearningCount());
            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getWrongCount());
            assertTrue(result.getFirstVisit());
            assertTrue(result.getRecentInterviews().isEmpty());
            assertTrue(result.getWeakPoints().isEmpty());
            assertEquals(0, result.getTodayLearnCards());
            assertEquals(0, result.getTodayReviewCards());
            assertEquals(0, result.getTodayCompletedCards());
            assertEquals(BigDecimal.ZERO, result.getTodayCardCompletionRate());
            assertEquals(0, result.getMasteredCardCount());
            assertEquals(0, result.getReviewDebtCount());
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
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(4L);

            RecentInterviewVO recent = new RecentInterviewVO();
            recent.setSessionId(1L);
            recent.setDirection("Java Backend");
            recent.setTotalScore(new BigDecimal("80"));
            recent.setStatus("finished");
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(List.of(recent));

            WeakPointVO weak = new WeakPointVO();
            weak.setCategoryName("JVM");
            weak.setWrongCount(5);
            weak.setScore(new BigDecimal("60"));
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(List.of(weak));

            KnowledgeCardTask task = new KnowledgeCardTask();
            task.setId(9L);
            task.setCurrentDay(2);
            task.setDays(5);
            task.setStatus("active");
            task.setUpdateTime(LocalDateTime.now());
            when(dashboardMetricsMapper.selectLatestCardTaskId(1L)).thenReturn(9L);
            when(knowledgeCardTaskMapper.selectById(9L)).thenReturn(task);
            when(dashboardMetricsMapper.countTodayLearnCards(9L, 2)).thenReturn(3L);
            when(dashboardMetricsMapper.countTodayReviewCards(9L, 2)).thenReturn(2L);
            when(dashboardMetricsMapper.countTodayCompletedCards(9L)).thenReturn(4L);
            when(dashboardMetricsMapper.countMasteredCards(9L)).thenReturn(11L);

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(8, result.getLearningCount());
            assertEquals(new BigDecimal("78.50"), result.getAverageScore());
            assertEquals(10, result.getWrongCount());
            assertFalse(result.getFirstVisit());
            assertEquals(1, result.getRecentInterviews().size());
            assertEquals(1, result.getWeakPoints().size());
            assertEquals(3, result.getTodayLearnCards());
            assertEquals(2, result.getTodayReviewCards());
            assertEquals(4, result.getTodayCompletedCards());
            assertEquals(new BigDecimal("80.00"), result.getTodayCardCompletionRate());
            assertEquals(11, result.getMasteredCardCount());
            assertEquals(4, result.getReviewDebtCount());
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
