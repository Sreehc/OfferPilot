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
import com.offerpilot.interview.dto.JobPrepSessionCreateRequest;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class InterviewJobPrepServiceImpl implements InterviewJobPrepService {

    private static final List<String> JD_KEYWORDS = List.of(
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "RabbitMQ",
            "Elasticsearch", "Docker", "Kubernetes", "Linux", "微服务", "高并发", "分布式", "缓存", "消息队列",
            "SQL", "JVM", "Netty", "Nacos", "Dubbo", "系统设计", "性能优化", "事务", "并发");

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final Set<String> JOB_PREP_PROVIDER_SCOPES = Set.of("search");

    private final JobPrepSessionMapper jobPrepSessionMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final UserProviderConfigService userProviderConfigService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public JobPrepSessionVO createSession(Long userId, JobPrepSessionCreateRequest request) {
        JobApplication application = resolveApplication(userId, request.getApplicationId());
        Long resumeId = firstNonNull(request.getResumeId(), application == null ? null : application.getResumeFileId());
        ResumeSnapshot resumeSnapshot = resolveResumeSnapshot(userId, resumeId);
        String company = firstNonBlank(request.getCompany(), application == null ? null : application.getCompany());
        String jobTitle = firstNonBlank(request.getJobTitle(), application == null ? null : application.getJobTitle());
        String jdText = firstNonBlank(request.getJdText(), application == null ? null : application.getJdText());
        if (!StringUtils.hasText(jdText)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "jd text is required");
        }

        PrepBlueprint blueprint = buildBlueprint(jobTitle, jdText, resumeSnapshot);

        JobPrepSession session = new JobPrepSession();
        session.setUserId(userId);
        session.setApplicationId(application == null ? null : application.getId());
        session.setResumeFileId(resumeSnapshot.resumeId());
        session.setCompany(company);
        session.setJobTitle(jobTitle);
        session.setJdText(jdText);
        session.setStatus("ready");
        session.setMatchScore(blueprint.matchScore());
        session.setMatchedKeywordsJson(writeList(blueprint.matchedKeywords()));
        session.setMissingKeywordsJson(writeList(blueprint.missingKeywords()));
        session.setFocusAreasJson(writeList(blueprint.focusAreas()));
        session.setResumeTalkingPointsJson(writeList(blueprint.resumeTalkingPoints()));
        session.setMockQuestionsJson(writeList(blueprint.mockQuestions()));
        session.setNextActionsJson(writeList(blueprint.nextActions()));
        session.setSummary(blueprint.summary());
        jobPrepSessionMapper.insert(session);
        return buildVo(session, resumeSnapshot.resumeTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public JobPrepSessionVO detail(Long userId, Long sessionId) {
        JobPrepSession session = jobPrepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "job prep session not found");
        }
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public JobPrepSessionVO latest(Long userId) {
        JobPrepSession session = jobPrepSessionMapper.selectOne(new LambdaQueryWrapper<JobPrepSession>()
                .eq(JobPrepSession::getUserId, userId)
                .orderByDesc(JobPrepSession::getUpdateTime)
                .orderByDesc(JobPrepSession::getId)
                .last("LIMIT 1"));
        if (session == null) {
            return null;
        }
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle());
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

    private ResumeSnapshot resolveResumeSnapshot(Long userId, Long resumeId) {
        ResumeFile resume = resumeId == null
                ? resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getUserId, userId)
                        .orderByDesc(ResumeFile::getUpdateTime)
                        .last("LIMIT 1"))
                : resumeFileMapper.selectById(resumeId);
        if (resume == null) {
            return new ResumeSnapshot(null, null, null, List.of(), List.of());
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "resume does not belong to current user");
        }
        List<ResumeProject> projects = resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeFileId, resume.getId())
                .orderByAsc(ResumeProject::getSortOrder)
                .orderByAsc(ResumeProject::getId));
        return new ResumeSnapshot(resume.getId(), resume.getTitle(), resume.getSummary(), splitComma(resume.getSkills()), projects);
    }

    private PrepBlueprint buildBlueprint(String jobTitle, String jdText, ResumeSnapshot resumeSnapshot) {
        List<String> jdKeywords = extractKeywords(jdText);
        Set<String> resumeKeywords = new LinkedHashSet<>(resumeSnapshot.skills());
        for (ResumeProject project : resumeSnapshot.projects()) {
            resumeKeywords.addAll(splitComma(project.getTechStack()));
            resumeKeywords.add(project.getProjectName());
        }

        List<String> matchedKeywords = jdKeywords.stream()
                .filter(keyword -> containsIgnoreCase(resumeKeywords, keyword))
                .distinct()
                .toList();
        List<String> missingKeywords = jdKeywords.stream()
                .filter(keyword -> !containsIgnoreCase(resumeKeywords, keyword))
                .distinct()
                .limit(6)
                .toList();

        BigDecimal matchScore = resolveMatchScore(jdKeywords, matchedKeywords, missingKeywords);
        List<String> focusAreas = buildFocusAreas(jobTitle, matchedKeywords, missingKeywords, resumeSnapshot);
        List<String> resumeTalkingPoints = buildResumeTalkingPoints(jobTitle, matchedKeywords, missingKeywords, resumeSnapshot);
        List<String> mockQuestions = buildMockQuestions(jobTitle, matchedKeywords, missingKeywords, resumeSnapshot);
        List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness = resolveProviderReadiness();
        List<String> nextActions = buildNextActions(missingKeywords, resumeTalkingPoints, resumeSnapshot.resumeTitle(), providerReadiness);
        String summary = buildSummary(jobTitle, resumeSnapshot.resumeTitle(), matchedKeywords, missingKeywords, matchScore, providerReadiness);

        return new PrepBlueprint(
                matchScore,
                matchedKeywords,
                missingKeywords,
                focusAreas,
                resumeTalkingPoints,
                mockQuestions,
                nextActions,
                providerReadiness,
                summary);
    }

    private BigDecimal resolveMatchScore(List<String> jdKeywords, List<String> matchedKeywords, List<String> missingKeywords) {
        if (jdKeywords.isEmpty()) {
            return new BigDecimal("60.00");
        }
        double raw = 45 + matchedKeywords.size() * 10 - missingKeywords.size() * 3;
        raw = Math.max(30, Math.min(96, raw));
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> buildFocusAreas(String jobTitle, List<String> matchedKeywords, List<String> missingKeywords,
                                         ResumeSnapshot resumeSnapshot) {
        List<String> areas = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            areas.add("先补 " + String.join("、", missingKeywords.stream().limit(3).toList()) + "，避免一面被直接问住。");
        }
        if (!matchedKeywords.isEmpty()) {
            areas.add("把 " + String.join("、", matchedKeywords.stream().limit(3).toList()) + " 对应到具体项目结果，准备 1-2 个可量化案例。");
        }
        if (!resumeSnapshot.projects().isEmpty()) {
            ResumeProject project = resumeSnapshot.projects().get(0);
            areas.add("围绕项目「" + project.getProjectName() + "」准备追问：职责边界、关键取舍、性能或稳定性结果。");
        }
        if (StringUtils.hasText(jobTitle)) {
            areas.add("按目标岗位「" + jobTitle + "」视角收紧表达，优先突出离上线、稳定性、排障最近的经历。");
        }
        return areas.stream().distinct().limit(4).toList();
    }

    private List<String> buildResumeTalkingPoints(String jobTitle, List<String> matchedKeywords, List<String> missingKeywords,
                                                  ResumeSnapshot resumeSnapshot) {
        List<String> points = new ArrayList<>();
        for (ResumeProject project : resumeSnapshot.projects()) {
            List<String> projectHits = splitComma(project.getTechStack()).stream()
                    .filter(keyword -> containsIgnoreCase(new LinkedHashSet<>(matchedKeywords), keyword))
                    .toList();
            String summary = firstNonBlank(project.getAchievement(), project.getProjectSummary(), project.getResponsibility());
            if (!StringUtils.hasText(summary)) {
                continue;
            }
            if (!projectHits.isEmpty()) {
                points.add("项目「" + project.getProjectName() + "」可重点讲 " + String.join("、", projectHits) + "："
                        + abbreviate(summary, 72));
            } else {
                points.add("项目「" + project.getProjectName() + "」可作为案例补位：" + abbreviate(summary, 72));
            }
        }
        if (points.isEmpty() && StringUtils.hasText(resumeSnapshot.summary())) {
            points.add("先把简历总结压成 60 秒版本，重点说清你负责的核心系统、指标和结果。");
        }
        if (!missingKeywords.isEmpty()) {
            points.add("对缺口关键词 " + String.join("、", missingKeywords.stream().limit(2).toList())
                    + " 先准备原理 + 实战取舍，不要只停在概念层。");
        }
        if (StringUtils.hasText(jobTitle)) {
            points.add("所有项目表达都回到「" + jobTitle + "」需要的价值：交付、稳定性、性能、协作。");
        }
        return points.stream().distinct().limit(5).toList();
    }

    private List<String> buildMockQuestions(String jobTitle, List<String> matchedKeywords, List<String> missingKeywords,
                                            ResumeSnapshot resumeSnapshot) {
        List<String> questions = new ArrayList<>();
        for (String keyword : missingKeywords.stream().limit(3).toList()) {
            questions.add("如果岗位重点要求 " + keyword + "，你会怎么解释自己做过的相关设计、排障或取舍？");
        }
        for (ResumeProject project : resumeSnapshot.projects().stream().limit(2).toList()) {
            questions.add("结合项目「" + project.getProjectName() + "」，讲一次最难的问题定位和最终结果。");
        }
        if (!matchedKeywords.isEmpty()) {
            questions.add("你在 " + String.join("、", matchedKeywords.stream().limit(2).toList()) + " 上做过哪些优化？为什么这么做？");
        }
        questions.add(StringUtils.hasText(jobTitle)
                ? "如果你来做「" + jobTitle + "」，入职前两周你会优先补什么？"
                : "如果明天就是一面，你会先补哪三个点？为什么？");
        return questions.stream().distinct().limit(6).toList();
    }

    private List<String> buildNextActions(List<String> missingKeywords, List<String> resumeTalkingPoints, String resumeTitle,
                                          List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness) {
        List<String> actions = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            actions.add("先围绕 " + String.join("、", missingKeywords.stream().limit(3).toList()) + " 安排一轮专项训练。");
        }
        actions.add("把这次 JD 备面结果转成 3-5 道模拟面试题，进入下一轮面试演练。");
        if (!resumeTalkingPoints.isEmpty()) {
            actions.add("把最强的 2 个项目表达写成口语版提纲，控制在 90 秒内。");
        }
        if (StringUtils.hasText(resumeTitle)) {
            actions.add("回看简历《" + resumeTitle + "》，确认关键词命中和项目排序是否还需要调整。");
        }
        if (providerReadiness.stream().anyMatch(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus()))) {
            actions.add("联网搜索未完全就绪时，先按当前 JD 和简历草案继续推进，再补公司与岗位背景研究。");
        }
        return actions.stream().distinct().limit(4).toList();
    }

    private String buildSummary(String jobTitle, String resumeTitle, List<String> matchedKeywords,
                                List<String> missingKeywords, BigDecimal matchScore,
                                List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness) {
        String roleText = StringUtils.hasText(jobTitle) ? "目标岗位「" + jobTitle + "」" : "当前岗位";
        String resumeText = StringUtils.hasText(resumeTitle)
                ? "当前使用简历《" + resumeTitle + "》。"
                : "当前未绑定简历，结果主要基于 JD 本身估算。";
        String matchText = matchedKeywords.isEmpty()
                ? "简历里暂时没有明显命中关键词。"
                : "已命中 " + String.join("、", matchedKeywords.stream().limit(4).toList()) + "。";
        String gapText = missingKeywords.isEmpty()
                ? "当前没有明显关键词缺口，重点改成准备项目追问和量化结果。"
                : "需要优先补 " + String.join("、", missingKeywords.stream().limit(4).toList()) + "。";
        long degradedCount = providerReadiness.stream().filter(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus())).count();
        String providerText = degradedCount == 0
                ? "岗位研究依赖已基本就绪。"
                : "当前有 " + degradedCount + " 项岗位研究依赖未完全就绪，结果会优先基于 JD 和简历本身降级生成。";
        return roleText + " 的当前备面匹配度约 " + matchScore.stripTrailingZeros().toPlainString() + " 分。"
                + resumeText + matchText + gapText + providerText;
    }

    private List<JobPrepSessionVO.ProviderReadinessVO> resolveProviderReadiness() {
        Map<String, UserProviderConfigItemVO> configMap = new LinkedHashMap<>();
        for (UserProviderConfigItemVO item : userProviderConfigService.listCurrentUserConfigs()) {
            if (item != null && StringUtils.hasText(item.getScope())) {
                configMap.put(item.getScope(), item);
            }
        }
        List<JobPrepSessionVO.ProviderReadinessVO> readiness = new ArrayList<>();
        for (String scope : JOB_PREP_PROVIDER_SCOPES) {
            UserProviderConfigItemVO item = configMap.get(scope);
            readiness.add(JobPrepSessionVO.ProviderReadinessVO.builder()
                    .scope(scope)
                    .label(item == null ? fallbackProviderLabel(scope) : item.getLabel())
                    .status(item == null ? "missing" : item.getStatus())
                    .statusMessage(item == null ? "还没有保存这类配置。" : item.getStatusMessage())
                    .build());
        }
        return readiness;
    }

    private List<String> extractKeywords(String jdText) {
        if (!StringUtils.hasText(jdText)) {
            return List.of();
        }
        String lower = jdText.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String keyword : JD_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                keywords.add(keyword);
            }
        }
        if (keywords.isEmpty()) {
            Arrays.stream(jdText.split("[，,。；;、\\s]+"))
                    .map(String::trim)
                    .filter(token -> token.length() >= 2 && token.length() <= 16)
                    .filter(token -> token.matches(".*[\\u4e00-\\u9fa5A-Za-z].*"))
                    .limit(8)
                    .forEach(keywords::add);
        }
        return new ArrayList<>(keywords);
    }

    private boolean containsIgnoreCase(Set<String> values, String keyword) {
        return values.stream().filter(StringUtils::hasText).anyMatch(value -> value.equalsIgnoreCase(keyword));
    }

    private List<String> splitComma(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize job prep list", e);
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
            log.warn("Failed to parse job prep list", e);
            return List.of();
        }
    }

    private JobPrepSessionVO buildVo(JobPrepSession session, String resumeTitle) {
        List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness = resolveProviderReadiness();
        return JobPrepSessionVO.builder()
                .id(session.getId())
                .applicationId(session.getApplicationId())
                .resumeFileId(session.getResumeFileId())
                .resumeTitle(resumeTitle)
                .company(session.getCompany())
                .jobTitle(session.getJobTitle())
                .jdText(session.getJdText())
                .status(session.getStatus())
                .matchScore(session.getMatchScore())
                .matchedKeywords(readList(session.getMatchedKeywordsJson()))
                .missingKeywords(readList(session.getMissingKeywordsJson()))
                .focusAreas(readList(session.getFocusAreasJson()))
                .resumeTalkingPoints(readList(session.getResumeTalkingPointsJson()))
                .mockQuestions(readList(session.getMockQuestionsJson()))
                .nextActions(readList(session.getNextActionsJson()))
                .providerStatus(resolveProviderStatus(providerReadiness, false))
                .providerStatusMessage(buildProviderStatusMessage(providerReadiness, false, "JD 备面"))
                .providerReadiness(providerReadiness)
                .summary(session.getSummary())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private String resolveProviderStatus(List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness, boolean requiredOnly) {
        boolean hasUnavailable = providerReadiness.stream()
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        if (!hasUnavailable) {
            return "ready";
        }
        return requiredOnly ? "blocked" : "degraded";
    }

    private String buildProviderStatusMessage(List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness,
                                              boolean requiredOnly,
                                              String label) {
        List<String> unavailable = providerReadiness.stream()
                .filter(item -> !isProviderAvailable(item.getStatus()))
                .map(JobPrepSessionVO.ProviderReadinessVO::getLabel)
                .toList();
        if (unavailable.isEmpty()) {
            return label + " 当前依赖已就绪。";
        }
        return requiredOnly
                ? label + " 当前缺少关键依赖：" + String.join("、", unavailable) + "。"
                : label + " 当前有依赖未完全就绪：" + String.join("、", unavailable) + "，相关能力会按降级模式运行。";
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String abbreviate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private record ResumeSnapshot(Long resumeId, String resumeTitle, String summary, List<String> skills,
                                  List<ResumeProject> projects) {
    }

    private record PrepBlueprint(BigDecimal matchScore, List<String> matchedKeywords, List<String> missingKeywords,
                                 List<String> focusAreas, List<String> resumeTalkingPoints, List<String> mockQuestions,
                                 List<String> nextActions, List<JobPrepSessionVO.ProviderReadinessVO> providerReadiness,
                                 String summary) {
    }
}
