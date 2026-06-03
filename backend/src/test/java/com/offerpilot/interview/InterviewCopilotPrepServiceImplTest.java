package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.interview.service.impl.InterviewCopilotPrepServiceImpl;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewCopilotPrepServiceImplTest {

    @Mock
    private CopilotPrepSessionMapper copilotPrepSessionMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private JobPrepSessionMapper jobPrepSessionMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;
    @Mock
    private UserProviderConfigService userProviderConfigService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterviewCopilotPrepServiceImpl service;

    @Test
    void latest_returnsMostRecentCopilotPrepSession() {
        CopilotPrepSession session = new CopilotPrepSession();
        session.setId(53L);
        session.setUserId(1L);
        session.setApplicationId(7L);
        session.setResumeFileId(15L);
        session.setJobPrepSessionId(31L);
        session.setCompany("美团");
        session.setJobTitle("资深 Java 工程师");
        session.setStatus("ready");
        session.setSummary("最近一次 Copilot Prep");
        session.setOpeningBriefJson("[\"开场先讲最贴近岗位的项目背景和核心职责。\"]");
        session.setKeyRisksJson("[\"Kafka 追问深挖容易暴露准备边界。\"]");
        session.setLiveCuesJson("[\"如果问题很大，先回答结论，再拆为什么和怎么做。\"]");
        session.setFollowUpQuestionsJson("[\"如果继续追问 Kafka，你会用哪个项目例子应对？\"]");
        session.setNextActionsJson("[\"先把开场提纲压成 60-90 秒口语版，再进入实时阶段。\"]");
        session.setProviderReadinessJson("[{\"scope\":\"asr\",\"label\":\"语音识别\",\"status\":\"ready\",\"statusMessage\":\"配置完整\"}]");
        session.setUpdateTime(LocalDateTime.of(2026, 6, 3, 21, 0));
        when(copilotPrepSessionMapper.selectOne(any())).thenReturn(session);

        ResumeFile resume = new ResumeFile();
        resume.setId(15L);
        resume.setTitle("Java 后端简历");
        when(resumeFileMapper.selectById(15L)).thenReturn(resume);

        CopilotPrepSessionVO result = service.latest(1L);

        assertEquals("53", String.valueOf(result.getId()));
        assertEquals("Java 后端简历", result.getResumeTitle());
        assertEquals("美团", result.getCompany());
        assertEquals("资深 Java 工程师", result.getJobTitle());
        assertEquals("ready", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("已就绪"));
        assertEquals("realtime_copilot", result.getSuggestedAgentType());
        assertEquals("interview_live", result.getSuggestedTriggerSource());
        assertEquals("继续实时阶段", result.getNextActionLabel());
        assertEquals("/interview?workspace=copilot-live&copilotPrepSessionId=53", result.getNextActionPath());
        assertTrue(result.getOpeningBrief().stream().anyMatch(item -> item.contains("项目背景")));
        assertTrue(result.getNextActions().stream().anyMatch(item -> item.contains("60-90 秒口语版")));
    }
}
