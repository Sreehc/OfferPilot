package com.offerpilot.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.RecentInterviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.mapper.DashboardMetricsMapper;
import com.offerpilot.dashboard.service.impl.DashboardServiceImpl;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AdaptiveService adaptiveService;
    @Mock
    private OfferPilotProperties props;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private ReviewLogMapper reviewLogMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void overview_emptyData_marksFirstVisitAndBuildsEmptyApplicationSummary() {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);

            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(null);
            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder().build());
            when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class))).thenReturn("{}");

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(0, result.getLearningCount());
            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getWrongCount());
            assertTrue(result.getFirstVisit());
            assertTrue(result.getRecentInterviews().isEmpty());
            assertTrue(result.getWeakPoints().isEmpty());
            assertEquals(0, result.getReviewDebtCount());
            assertEquals(0, result.getStudyStreak());
            assertEquals(0, result.getApplicationSummary().getTotalCount());
            assertEquals("还没有投递记录", result.getApplicationSummary().getTopCompany());
        }
    }

    @Test
    void overview_hasData_buildsApplicationSummaryAndNotFirstVisit() {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);

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

            JobApplication active = new JobApplication();
            active.setStatus("interview");
            active.setCompany("Alpha");
            active.setMatchScore(new BigDecimal("85"));

            JobApplication offer = new JobApplication();
            offer.setStatus("offer");
            offer.setCompany("Beta");
            offer.setMatchScore(new BigDecimal("95"));

            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(active, offer));
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(
                    AbilityProfileVO.builder()
                            .weakCategories(List.of("JVM"))
                            .suggestedFocus("JVM")
                            .build());
            when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class))).thenReturn("{}");

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(8, result.getLearningCount());
            assertEquals(new BigDecimal("78.50"), result.getAverageScore());
            assertEquals(10, result.getWrongCount());
            assertFalse(result.getFirstVisit());
            assertEquals(1, result.getRecentInterviews().size());
            assertEquals(1, result.getWeakPoints().size());
            assertEquals(4, result.getReviewDebtCount());
            assertEquals(2, result.getApplicationSummary().getTotalCount());
            assertEquals(1, result.getApplicationSummary().getActiveCount());
            assertEquals(1, result.getApplicationSummary().getOfferCount());
            assertEquals(new BigDecimal("90.00"), result.getApplicationSummary().getAverageMatchScore());
            assertEquals("Beta", result.getApplicationSummary().getTopCompany());
            verify(valueOperations).set(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void overview_returnsCachedPayloadWhenPresent() throws Exception {
        DashboardOverviewVO cached = DashboardOverviewVO.builder()
                .learningCount(2)
                .averageScore(new BigDecimal("88"))
                .wrongCount(1)
                .recentInterviews(List.of())
                .weakPoints(List.of())
                .firstVisit(false)
                .build();

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn("{cached}");
            when(objectMapper.readValue("{cached}", DashboardOverviewVO.class)).thenReturn(cached);

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(2, result.getLearningCount());
            verify(dashboardMetricsMapper, never()).countChatSessions(1L);
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
