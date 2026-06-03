package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.interview.dto.JobPrepSessionCreateRequest;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.interview.service.impl.InterviewJobPrepServiceImpl;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.application.mapper.JobApplicationMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewJobPrepServiceImplTest {

    @Mock
    private JobPrepSessionMapper jobPrepSessionMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;
    @Mock
    private UserProviderConfigService userProviderConfigService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterviewJobPrepServiceImpl service;

    @Test
    void createSession_includesProviderReadinessAndDegradedSummaryWhenSearchMissing() {
        when(resumeFileMapper.selectOne(any())).thenReturn(null);
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of(
                UserProviderConfigItemVO.builder()
                        .scope("search")
                        .label("联网搜索")
                        .status("missing")
                        .statusMessage("还没有保存这类配置。")
                        .build()));
        when(jobPrepSessionMapper.insert(any(JobPrepSession.class))).thenAnswer(invocation -> {
            JobPrepSession session = invocation.getArgument(0);
            session.setId(101L);
            return 1;
        });

        JobPrepSessionCreateRequest request = new JobPrepSessionCreateRequest();
        request.setJobTitle("Java 后端开发");
        request.setJdText("负责 Java、Spring Boot、Redis 和 Kafka 相关服务建设");

        JobPrepSessionVO result = service.createSession(1L, request);

        assertEquals(1, result.getProviderReadiness().size());
        assertEquals("search", result.getProviderReadiness().get(0).getScope());
        assertEquals("missing", result.getProviderReadiness().get(0).getStatus());
        assertEquals("degraded", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("联网搜索"));
        assertTrue(result.getSummary().contains("降级生成"));
        assertTrue(result.getNextActions().stream().anyMatch(item -> item.contains("联网搜索未完全就绪")));
    }

    @Test
    void latest_returnsMostRecentJobPrepSession() {
        JobPrepSession session = new JobPrepSession();
        session.setId(42L);
        session.setUserId(1L);
        session.setApplicationId(8L);
        session.setResumeFileId(13L);
        session.setCompany("字节跳动");
        session.setJobTitle("后端开发");
        session.setStatus("ready");
        session.setMatchScore(new BigDecimal("78"));
        session.setMatchedKeywordsJson("[\"Java\",\"Redis\"]");
        session.setMissingKeywordsJson("[\"Kafka\"]");
        session.setFocusAreasJson("[\"先补 Kafka\"]");
        session.setResumeTalkingPointsJson("[\"项目 A 可重点讲缓存优化\"]");
        session.setMockQuestionsJson("[\"如果岗位重点要求 Kafka，你会怎么补位？\"]");
        session.setNextActionsJson("[\"先围绕 Kafka 安排一轮专项训练。\"]");
        session.setSummary("最近一次 JD 备面");
        session.setUpdateTime(LocalDateTime.of(2026, 6, 3, 10, 0));
        when(jobPrepSessionMapper.selectOne(any())).thenReturn(session);
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of());

        ResumeFile resume = new ResumeFile();
        resume.setId(13L);
        resume.setTitle("后端专项简历");
        when(resumeFileMapper.selectById(13L)).thenReturn(resume);

        JobPrepSessionVO result = service.latest(1L);

        assertEquals("42", String.valueOf(result.getId()));
        assertEquals("后端专项简历", result.getResumeTitle());
        assertEquals("字节跳动", result.getCompany());
        assertEquals("后端开发", result.getJobTitle());
        assertEquals("degraded", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("JD 备面"));
        assertTrue(result.getMissingKeywords().contains("Kafka"));
    }
}
