package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.interview.dto.CopilotPrepSessionCreateRequest;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.interview.service.InterviewCopilotPrepService;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewCopilotPrepServiceImpl implements InterviewCopilotPrepService {

    private static final List<String> JD_KEYWORDS = List.of(
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "RabbitMQ",
            "Elasticsearch", "Docker", "Kubernetes", "Linux", "微服务", "高并发", "分布式", "缓存", "消息队列",
            "SQL", "JVM", "Netty", "Nacos", "Dubbo", "系统设计", "性能优化", "事务", "并发");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<CopilotPrepSessionVO.ProviderReadinessVO>> PROVIDER_LIST_TYPE = new TypeReference<>() {
    };
    private static final Set<String> COPILOT_PROVIDER_SCOPES = Set.of("asr", "search", "voiceprint");

    private final CopilotPrepSessionMapper copilotPrepSessionMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobPrepSessionMapper jobPrepSessionMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final UserProviderConfigService userProviderConfigService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CopilotPrepSessionVO createSession(Long userId, CopilotPrepSessionCreateRequest request) {
        JobApplication application = resolveApplication(userId, request.getApplicationId());
        JobPrepSession jobPrepSession = resolveJobPrepSession(userId, request.getJobPrepSessionId());

        Long resumeId = firstNonNull(
                request.getResumeId(),
                jobPrepSession == null ? null : jobPrepSession.getResumeFileId(),
                application == null ? null : application.getResumeFileId());
        ResumeSnapshot resumeSnapshot = resolveResumeSnapshot(userId, resumeId);

        String company = firstNonBlank(
                request.getCompany(),
                jobPrepSession == null ? null : jobPrepSession.getCompany(),
                application == null ? null : application.getCompany());
        String jobTitle = firstNonBlank(
                request.getJobTitle(),
                jobPrepSession == null ? null : jobPrepSession.getJobTitle(),
                application == null ? null : application.getJobTitle(),
                "Java 后端开发");
        String jdText = firstNonBlank(
                request.getJdText(),
                jobPrepSession == null ? null : jobPrepSession.getJdText(),
                application == null ? null : application.getJdText());
        if (!StringUtils.hasText(jobTitle) && !StringUtils.hasText(jdText) && resumeSnapshot.resumeId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "copilot prep needs at least a role, jd, or resume");
        }

        PrepBlueprint blueprint = buildBlueprint(company, jobTitle, jdText, trimToNull(request.getNotes()), resumeSnapshot);

        CopilotPrepSession session = new CopilotPrepSession();
        session.setUserId(userId);
        session.setApplicationId(application == null ? null : application.getId());
        session.setResumeFileId(resumeSnapshot.resumeId());
        session.setJobPrepSessionId(jobPrepSession == null ? null : jobPrepSession.getId());
        session.setCompany(company);
        session.setJobTitle(jobTitle);
        session.setJdText(jdText);
        session.setNotes(trimToNull(request.getNotes()));
        session.setStatus("ready");
        session.setSummary(blueprint.summary());
        session.setOpeningBriefJson(writeList(blueprint.openingBrief()));
        session.setKeyRisksJson(writeList(blueprint.keyRisks()));
        session.setLiveCuesJson(writeList(blueprint.liveCues()));
        session.setFollowUpQuestionsJson(writeList(blueprint.followUpQuestions()));
        session.setNextActionsJson(writeList(blueprint.nextActions()));
        session.setProviderReadinessJson(writeProviderList(blueprint.providerReadiness()));
        copilotPrepSessionMapper.insert(session);
        return buildVo(session, resumeSnapshot.resumeTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public CopilotPrepSessionVO detail(Long userId, Long sessionId) {
        CopilotPrepSession session = copilotPrepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "copilot prep session not found");
        }
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public CopilotPrepSessionVO latest(Long userId) {
        CopilotPrepSession session = copilotPrepSessionMapper.selectOne(new LambdaQueryWrapper<CopilotPrepSession>()
                .eq(CopilotPrepSession::getUserId, userId)
                .orderByDesc(CopilotPrepSession::getUpdateTime)
                .orderByDesc(CopilotPrepSession::getId)
                .last("LIMIT 1"));
        if (session == null) {
            return null;
        }
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle());
    }

    private PrepBlueprint buildBlueprint(String company, String jobTitle, String jdText, String notes, ResumeSnapshot resumeSnapshot) {
        List<String> jdKeywords = extractKeywords(jdText);
        List<String> matchedKeywords = jdKeywords.stream()
                .filter(keyword -> containsIgnoreCase(resumeSnapshot.resumeKeywords(), keyword))
                .distinct()
                .toList();
        List<String> missingKeywords = jdKeywords.stream()
                .filter(keyword -> !containsIgnoreCase(resumeSnapshot.resumeKeywords(), keyword))
                .distinct()
                .limit(5)
                .toList();

        List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness = resolveProviderReadiness();
        List<String> openingBrief = buildOpeningBrief(company, jobTitle, matchedKeywords, missingKeywords, resumeSnapshot);
        List<String> keyRisks = buildKeyRisks(jobTitle, missingKeywords, providerReadiness, resumeSnapshot);
        List<String> liveCues = buildLiveCues(company, jobTitle, matchedKeywords, missingKeywords, notes);
        List<String> followUpQuestions = buildFollowUpQuestions(jobTitle, missingKeywords, resumeSnapshot);
        List<String> nextActions = buildNextActions(providerReadiness, jobTitle, resumeSnapshot, notes);
        String summary = buildSummary(company, jobTitle, matchedKeywords, missingKeywords, providerReadiness);

        return new PrepBlueprint(summary, openingBrief, keyRisks, liveCues, followUpQuestions, nextActions, providerReadiness);
    }

    private List<String> buildOpeningBrief(String company, String jobTitle, List<String> matchedKeywords,
                                           List<String> missingKeywords, ResumeSnapshot resumeSnapshot) {
        LinkedHashSet<String> brief = new LinkedHashSet<>();
        brief.add("开场先用 20-30 秒交代你最贴近「" + defaultText(jobTitle, "当前岗位") + "」的项目背景和核心职责。");
        if (!matchedKeywords.isEmpty()) {
            brief.add("优先把 " + String.join("、", matchedKeywords.stream().limit(3).toList()) + " 绑定到实际结果，不要只背关键词。");
        }
        if (!resumeSnapshot.projects().isEmpty()) {
            ResumeProject project = resumeSnapshot.projects().get(0);
            brief.add("主案例建议选项目「" + project.getProjectName() + "」，提前准备“场景 -> 动作 -> 结果 -> 取舍”四句版。");
        }
        if (!missingKeywords.isEmpty()) {
            brief.add("对缺口点 " + String.join("、", missingKeywords.stream().limit(2).toList()) + " 先准备原理解释，再补一个项目替代案例。");
        }
        return brief.stream().limit(4).toList();
    }

    private List<String> buildKeyRisks(String jobTitle, List<String> missingKeywords,
                                       List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness,
                                       ResumeSnapshot resumeSnapshot) {
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        if (!missingKeywords.isEmpty()) {
            risks.add("岗位重点要求 " + String.join("、", missingKeywords.stream().limit(3).toList()) + "，如果被连续追问容易暴露准备深度不够。");
        }
        if (resumeSnapshot.projects().isEmpty()) {
            risks.add("当前没有明显项目案例可展开，实时阶段更容易卡在“只会讲概念”。");
        }
        providerReadiness.stream()
                .filter(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus()))
                .map(item -> item.getLabel() + " 未完全就绪，实时阶段可能需要降级。")
                .forEach(risks::add);
        risks.add("面对「" + defaultText(jobTitle, "当前岗位") + "」的开放题时，先给结论，再讲例子，避免一上来铺太长背景。");
        return risks.stream().limit(4).toList();
    }

    private List<String> buildLiveCues(String company, String jobTitle, List<String> matchedKeywords,
                                       List<String> missingKeywords, String notes) {
        LinkedHashSet<String> cues = new LinkedHashSet<>();
        cues.add("如果问题很大，先回答结论，再拆成“为什么 / 怎么做 / 结果如何”三个段落。");
        if (!matchedKeywords.isEmpty()) {
            cues.add("优先把 " + String.join("、", matchedKeywords.stream().limit(2).toList()) + " 讲成你亲手做过的动作，不要退回泛化定义。");
        }
        if (!missingKeywords.isEmpty()) {
            cues.add("被问到 " + String.join("、", missingKeywords.stream().limit(2).toList()) + " 时，先承认边界，再补你做过的近似场景和取舍。");
        }
        if (StringUtils.hasText(company)) {
            cues.add("涉及公司场景时，尽量把回答往「" + company.trim() + "」可能关心的稳定性、效率或协作价值上收。");
        }
        if (StringUtils.hasText(notes)) {
            cues.add("特别注意本次备注里的提醒：" + abbreviate(notes, 36));
        }
        if (StringUtils.hasText(jobTitle)) {
            cues.add("始终回到「" + jobTitle.trim() + "」岗位价值：交付、排障、性能、协作。");
        }
        return cues.stream().limit(5).toList();
    }

    private List<String> buildFollowUpQuestions(String jobTitle, List<String> missingKeywords, ResumeSnapshot resumeSnapshot) {
        LinkedHashSet<String> questions = new LinkedHashSet<>();
        for (String keyword : missingKeywords.stream().limit(3).toList()) {
            questions.add("如果面试官追问你在 " + keyword + " 上做过什么，你会先举哪个项目例子？");
        }
        for (ResumeProject project : resumeSnapshot.projects().stream().limit(2).toList()) {
            questions.add("围绕项目「" + project.getProjectName() + "」，准备一次“最难问题怎么定位并收口”的追问。");
        }
        questions.add("针对「" + defaultText(jobTitle, "当前岗位") + "」，如果面试官问你入职后两周先补什么，你会怎么答？");
        return questions.stream().limit(5).toList();
    }

    private List<String> buildNextActions(List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness, String jobTitle,
                                          ResumeSnapshot resumeSnapshot, String notes) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        actions.add("先把开场提纲压成 60-90 秒口语版，再进入实时阶段。");
        actions.add("会前快速回看 1 次最强项目案例，确保能讲清场景、动作、结果和取舍。");
        if (providerReadiness.stream().anyMatch(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus()))) {
            actions.add("实时阶段前先补齐缺失 provider，至少确认 ASR 和联网搜索的可用性。");
        }
        if (StringUtils.hasText(jobTitle)) {
            actions.add("把这份 Prep 和「" + jobTitle.trim() + "」岗位 JD、简历放在同一页，避免实时阶段来回切换。");
        }
        if (StringUtils.hasText(notes)) {
            actions.add("把备注里的特殊提醒单独抄成一条会前提示：" + abbreviate(notes, 30));
        }
        if (resumeSnapshot.resumeId() == null) {
            actions.add("补一份简历上下文后再做下一轮 Prep，实时建议会更贴近真实项目。");
        }
        return actions.stream().limit(5).toList();
    }

    private String buildSummary(String company, String jobTitle, List<String> matchedKeywords,
                                List<String> missingKeywords, List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness) {
        String base = StringUtils.hasText(company)
                ? "已为「" + company.trim() + " / " + defaultText(jobTitle, "目标岗位") + "」整理会前 Prep。"
                : "已整理当前岗位的会前 Prep。";
        String matchText = matchedKeywords.isEmpty()
                ? "当前更依赖项目表达和原理口径。"
                : "优先围绕 " + String.join("、", matchedKeywords.stream().limit(3).toList()) + " 展开案例。";
        String gapText = missingKeywords.isEmpty()
                ? "当前没有明显关键词缺口，可以把重点放在追问深挖。"
                : "仍需重点补 " + String.join("、", missingKeywords.stream().limit(2).toList()) + " 的回答边界。";
        long degradedCount = providerReadiness.stream().filter(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus())).count();
        String providerText = degradedCount == 0 ? "实时阶段依赖已基本就绪。" : "当前有 " + degradedCount + " 项实时依赖还没完全就绪。";
        return base + matchText + gapText + providerText;
    }

    private List<CopilotPrepSessionVO.ProviderReadinessVO> resolveProviderReadiness() {
        Map<String, UserProviderConfigItemVO> configMap = new LinkedHashMap<>();
        for (UserProviderConfigItemVO item : userProviderConfigService.listCurrentUserConfigs()) {
            configMap.put(item.getScope(), item);
        }
        List<CopilotPrepSessionVO.ProviderReadinessVO> readiness = new ArrayList<>();
        for (String scope : List.of("asr", "search", "voiceprint")) {
            UserProviderConfigItemVO item = configMap.get(scope);
            readiness.add(CopilotPrepSessionVO.ProviderReadinessVO.builder()
                    .scope(scope)
                    .label(item == null ? fallbackProviderLabel(scope) : item.getLabel())
                    .status(item == null ? "missing" : item.getStatus())
                    .statusMessage(item == null ? "还没有保存这类配置。" : item.getStatusMessage())
                    .build());
        }
        return readiness;
    }

    private JobApplication resolveApplication(Long userId, Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null || !application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "job application not found");
        }
        return application;
    }

    private JobPrepSession resolveJobPrepSession(Long userId, Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        JobPrepSession session = jobPrepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "job prep session not found");
        }
        return session;
    }

    private ResumeSnapshot resolveResumeSnapshot(Long userId, Long resumeId) {
        ResumeFile resume = resumeId == null
                ? resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getUpdateTime)
                .last("LIMIT 1"))
                : resumeFileMapper.selectById(resumeId);
        if (resume == null) {
            return new ResumeSnapshot(null, null, null, List.of(), List.of(), Set.of());
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "resume does not belong to current user");
        }
        List<ResumeProject> projects = resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeFileId, resume.getId())
                .orderByAsc(ResumeProject::getSortOrder)
                .orderByAsc(ResumeProject::getId));
        LinkedHashSet<String> keywords = new LinkedHashSet<>(splitComma(resume.getSkills()));
        for (ResumeProject project : projects) {
            keywords.add(project.getProjectName());
            keywords.addAll(splitComma(project.getTechStack()));
        }
        return new ResumeSnapshot(resume.getId(), resume.getTitle(), resume.getSummary(), splitComma(resume.getSkills()), projects, keywords);
    }

    private CopilotPrepSessionVO buildVo(CopilotPrepSession session, String resumeTitle) {
        List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness = readProviderList(session.getProviderReadinessJson());
        return CopilotPrepSessionVO.builder()
                .id(session.getId())
                .applicationId(session.getApplicationId())
                .resumeFileId(session.getResumeFileId())
                .jobPrepSessionId(session.getJobPrepSessionId())
                .resumeTitle(resumeTitle)
                .company(session.getCompany())
                .jobTitle(session.getJobTitle())
                .jdText(session.getJdText())
                .notes(session.getNotes())
                .status(session.getStatus())
                .summary(session.getSummary())
                .openingBrief(readList(session.getOpeningBriefJson()))
                .keyRisks(readList(session.getKeyRisksJson()))
                .liveCues(readList(session.getLiveCuesJson()))
                .followUpQuestions(readList(session.getFollowUpQuestionsJson()))
                .nextActions(readList(session.getNextActionsJson()))
                .providerStatus(resolveProviderStatus(providerReadiness))
                .providerStatusMessage(buildProviderStatusMessage(providerReadiness))
                .suggestedAgentType("realtime_copilot")
                .suggestedTriggerSource("interview_live")
                .nextActionLabel("继续实时阶段")
                .nextActionPath(buildRealtimeWorkspacePath(session))
                .providerReadiness(providerReadiness)
                .updateTime(session.getUpdateTime())
                .build();
    }

    private String buildRealtimeWorkspacePath(CopilotPrepSession session) {
        if (session.getId() == null) {
            return "/interview?workspace=copilot-live";
        }
        return "/interview?workspace=copilot-live&copilotPrepSessionId=" + session.getId();
    }

    private String resolveProviderStatus(List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness) {
        boolean hasUnavailable = providerReadiness.stream()
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        return hasUnavailable ? "degraded" : "ready";
    }

    private String buildProviderStatusMessage(List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness) {
        List<String> unavailable = providerReadiness.stream()
                .filter(item -> !isProviderAvailable(item.getStatus()))
                .map(CopilotPrepSessionVO.ProviderReadinessVO::getLabel)
                .toList();
        if (unavailable.isEmpty()) {
            return "Copilot Prep 当前依赖已就绪。";
        }
        return "Copilot Prep 当前有依赖未完全就绪：" + String.join("、", unavailable) + "，会前建议会按降级模式生成。";
    }

    private List<String> extractKeywords(String jdText) {
        if (!StringUtils.hasText(jdText)) {
            return List.of();
        }
        String normalized = jdText.toLowerCase(Locale.ROOT);
        return JD_KEYWORDS.stream()
                .filter(keyword -> normalized.contains(keyword.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private boolean containsIgnoreCase(Set<String> values, String keyword) {
        return values.stream().anyMatch(value -> keyword.equalsIgnoreCase(value));
    }

    private List<String> splitComma(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，/]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write copilot prep list", e);
            return "[]";
        }
    }

    private List<String> readList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read copilot prep list", e);
            return List.of();
        }
    }

    private String writeProviderList(List<CopilotPrepSessionVO.ProviderReadinessVO> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write copilot provider list", e);
            return "[]";
        }
    }

    private List<CopilotPrepSessionVO.ProviderReadinessVO> readProviderList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, PROVIDER_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read copilot provider list", e);
            return List.of();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isProviderAvailable(String status) {
        return "ready".equalsIgnoreCase(status) || "saved".equalsIgnoreCase(status);
    }

    private String fallbackProviderLabel(String scope) {
        return switch (scope == null ? "" : scope.toLowerCase(Locale.ROOT)) {
            case "search" -> "联网搜索";
            case "asr" -> "语音识别";
            case "oss" -> "对象存储";
            case "voiceprint" -> "声纹识别";
            case "llm" -> "主模型";
            case "embedding" -> "向量模型";
            default -> scope == null ? "" : scope.toUpperCase(Locale.ROOT);
        };
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String abbreviate(String text, int limit) {
        String trimmed = text.trim();
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit) + "...";
    }

    private record ResumeSnapshot(Long resumeId, String resumeTitle, String summary, List<String> skills,
                                  List<ResumeProject> projects, Set<String> resumeKeywords) {
    }

    private record PrepBlueprint(String summary, List<String> openingBrief, List<String> keyRisks, List<String> liveCues,
                                 List<String> followUpQuestions, List<String> nextActions,
                                 List<CopilotPrepSessionVO.ProviderReadinessVO> providerReadiness) {
    }
}
