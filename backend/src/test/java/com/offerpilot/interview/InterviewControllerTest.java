package com.offerpilot.interview;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.common.handler.GlobalExceptionHandler;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.interview.controller.InterviewController;
import com.offerpilot.interview.service.InterviewCopilotPrepService;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.service.InterviewVoiceService;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import com.offerpilot.security.model.LoginUser;
import com.offerpilot.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock
    private InterviewService interviewService;
    @Mock
    private InterviewCopilotPrepService interviewCopilotPrepService;
    @Mock
    private InterviewCopilotRealtimeService interviewCopilotRealtimeService;
    @Mock
    private InterviewJobPrepService interviewJobPrepService;
    @Mock
    private InterviewRecordingReviewService interviewRecordingReviewService;
    @Mock
    private InterviewVoiceService interviewVoiceService;
    @Mock
    private UploadPolicyService uploadPolicyService;

    private MockMvc mockMvc;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        InterviewController controller = new InterviewController(
                interviewService,
                interviewCopilotPrepService,
                interviewCopilotRealtimeService,
                interviewJobPrepService,
                interviewRecordingReviewService,
                interviewVoiceService,
                uploadPolicyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @BeforeEach
    void setAuthenticatedUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole("USER");
        user.setStatus(1);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(user), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void latestJobPrepSession_returnsMostRecentSnapshot() throws Exception {
        when(interviewJobPrepService.latest(1L)).thenReturn(JobPrepSessionVO.builder()
                .id(91L)
                .company("字节跳动")
                .jobTitle("后端开发")
                .summary("最近一次 JD 备面")
                .build());

        mockMvc.perform(get("/api/interview/job-prep/sessions/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("91"))
                .andExpect(jsonPath("$.data.company").value("字节跳动"))
                .andExpect(jsonPath("$.data.jobTitle").value("后端开发"));
    }

    @Test
    void latestCopilotPrepSession_returnsMostRecentSnapshot() throws Exception {
        when(interviewCopilotPrepService.latest(1L)).thenReturn(CopilotPrepSessionVO.builder()
                .id(58L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .summary("最近一次 Copilot Prep")
                .build());

        mockMvc.perform(get("/api/interview/copilot/prep-sessions/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("58"))
                .andExpect(jsonPath("$.data.company").value("美团"))
                .andExpect(jsonPath("$.data.jobTitle").value("资深 Java 工程师"));
    }

    @Test
    void latestCopilotRealtimeSession_returnsMostRecentSnapshot() throws Exception {
        when(interviewCopilotRealtimeService.latest(1L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(77L)
                .company("小红书")
                .jobTitle("Java 后端")
                .status("live")
                .build());

        mockMvc.perform(get("/api/interview/copilot/realtime-sessions/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("77"))
                .andExpect(jsonPath("$.data.company").value("小红书"))
                .andExpect(jsonPath("$.data.status").value("live"));
    }

    @Test
    void latestRecordingReview_returnsMostRecentSnapshot() throws Exception {
        when(interviewRecordingReviewService.latest(1L)).thenReturn(RecordingReviewSessionVO.builder()
                .id(105L)
                .direction("系统设计")
                .jobRole("后端开发")
                .status("completed")
                .build());

        mockMvc.perform(get("/api/interview/recording-reviews/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("105"))
                .andExpect(jsonPath("$.data.direction").value("系统设计"))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }
}
