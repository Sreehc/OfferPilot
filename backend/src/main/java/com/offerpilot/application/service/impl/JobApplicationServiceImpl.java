package com.offerpilot.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.application.dto.JobApplicationCreateRequest;
import com.offerpilot.application.dto.JobApplicationEventCreateRequest;
import com.offerpilot.application.dto.JobApplicationStatusRequest;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.entity.JobApplicationEvent;
import com.offerpilot.application.mapper.JobApplicationEventMapper;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.application.service.JobApplicationService;
import com.offerpilot.application.vo.JobApplicationEventVO;
import com.offerpilot.application.vo.JobApplicationVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final List<String> JD_KEYWORDS = List.of(
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "RabbitMQ",
            "Elasticsearch", "Docker", "Kubernetes", "Linux", "微服务", "高并发", "分布式", "缓存", "消息队列",
            "SQL", "JVM", "Netty", "Nacos", "Dubbo");

    private final JobApplicationMapper jobApplicationMapper;
    private final JobApplicationEventMapper jobApplicationEventMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;

    @Override
    @Transactional
    public JobApplicationVO create(Long userId, JobApplicationCreateRequest request) {
        ResumeSnapshot snapshot = resolveResumeSnapshot(userId, request.getResumeFileId());
        AnalysisSnapshot analysis = analyzeJd(request.getJdText(), snapshot);

        JobApplication application = new JobApplication();
        application.setUserId(userId);
        application.setResumeFileId(snapshot.resumeId());
        application.setCompany(request.getCompany().trim());
        application.setJobTitle(request.getJobTitle().trim());
        application.setCity(request.getCity());
        application.setSource(request.getSource());
        application.setJdText(request.getJdText());
        application.setStatus("saved");
        application.setMatchScore(analysis.matchScore());
        application.setJdKeywords(String.join(",", analysis.jdKeywords()));
        application.setMissingKeywords(String.join(",", analysis.missingKeywords()));
        application.setAnalysisSummary(analysis.summary());
        application.setApplyDate(request.getApplyDate());
        application.setReviewSuggestion(buildReviewSuggestion("saved", analysis, List.of()));
        application.setNextStepSuggestion(buildNextStepSuggestion("saved", analysis, null, List.of()));
        jobApplicationMapper.insert(application);

        createEvent(application.getId(), userId, "note", "已记录岗位并完成初步分析", "这条投递已经建档，接下来先决定什么时候投递。", LocalDateTime.now(), null, null, null, null);
        return detail(userId, application.getId());
    }

    @Override
    public List<JobApplicationVO> board(Long userId) {
        List<JobApplication> applications = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .orderByDesc(JobApplication::getUpdateTime));
        return buildVos(applications, false);
    }

    @Override
    public JobApplicationVO detail(Long userId, Long applicationId) {
        JobApplication application = getOwnedApplication(userId, applicationId);
        return buildVo(application, true);
    }

    @Override
    @Transactional
    public JobApplicationVO updateStatus(Long userId, Long applicationId, JobApplicationStatusRequest request) {
        JobApplication application = getOwnedApplication(userId, applicationId);
        application.setStatus(normalizeStatus(request.getStatus()));
        if (request.getNextStepDate() != null) {
            application.setNextStepDate(request.getNextStepDate());
        }
        if (application.getApplyDate() == null && "applied".equals(application.getStatus())) {
            application.setApplyDate(LocalDate.now());
        }
        application.setReviewSuggestion(buildReviewSuggestion(application.getStatus(), toAnalysisSnapshot(application), loadEvents(applicationId)));
        application.setNextStepSuggestion(buildNextStepSuggestion(application.getStatus(), toAnalysisSnapshot(application), request.getNextStepDate(), loadEvents(applicationId)));
        jobApplicationMapper.updateById(application);
        createEvent(applicationId, userId, "status_change", "已推进到「" + statusLabel(application.getStatus()) + "」",
                StringUtils.hasText(request.getNote()) ? request.getNote() : "已更新当前推进阶段。", LocalDateTime.now(), application.getStatus(), null, null, null);
        refreshSuggestions(application);
        return detail(userId, applicationId);
    }

    @Override
    @Transactional
    public JobApplicationVO addEvent(Long userId, Long applicationId, JobApplicationEventCreateRequest request) {
        getOwnedApplication(userId, applicationId);
        createEvent(
                applicationId,
                userId,
                request.getEventType(),
                request.getTitle(),
                request.getContent(),
                request.getEventTime() == null ? LocalDateTime.now() : request.getEventTime(),
                request.getResult(),
                request.getInterviewRound(),
                request.getInterviewer(),
                request.getFeedbackTags());
        JobApplication application = getOwnedApplication(userId, applicationId);
        refreshSuggestions(application);
        return detail(userId, applicationId);
    }

    @Override
    @Transactional
    public JobApplicationVO analyze(Long userId, Long applicationId) {
        JobApplication application = getOwnedApplication(userId, applicationId);
        ResumeSnapshot snapshot = resolveResumeSnapshot(userId, application.getResumeFileId());
        AnalysisSnapshot analysis = analyzeJd(application.getJdText(), snapshot);
        application.setResumeFileId(snapshot.resumeId());
        application.setMatchScore(analysis.matchScore());
        application.setJdKeywords(String.join(",", analysis.jdKeywords()));
        application.setMissingKeywords(String.join(",", analysis.missingKeywords()));
        application.setAnalysisSummary(analysis.summary());
        application.setReviewSuggestion(buildReviewSuggestion(application.getStatus(), analysis, loadEvents(applicationId)));
        application.setNextStepSuggestion(buildNextStepSuggestion(application.getStatus(), analysis, application.getNextStepDate(), loadEvents(applicationId)));
        jobApplicationMapper.updateById(application);
        createEvent(applicationId, userId, "analysis", "已刷新 JD 分析", analysis.summary(), LocalDateTime.now(), null, null, null, null);
        return detail(userId, applicationId);
    }

    private JobApplication getOwnedApplication(Long userId, Long applicationId) {
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null || !application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "job application not found");
        }
        return application;
    }

    private ResumeSnapshot resolveResumeSnapshot(Long userId, Long preferredResumeId) {
        ResumeFile resume = preferredResumeId == null
                ? resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getUserId, userId)
                        .orderByDesc(ResumeFile::getUpdateTime)
                        .last("LIMIT 1"))
                : resumeFileMapper.selectById(preferredResumeId);
        if (resume != null && !resume.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "resume does not belong to current user");
        }
        if (resume == null) {
            return new ResumeSnapshot(null, null, List.of(), List.of());
        }
        List<ResumeProject> projects = resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeFileId, resume.getId())
                .orderByAsc(ResumeProject::getSortOrder));
        List<String> resumeSkills = splitComma(resume.getSkills());
        List<String> projectSkills = projects.stream()
                .flatMap(project -> splitComma(project.getTechStack()).stream())
                .distinct()
                .toList();
        return new ResumeSnapshot(resume.getId(), resume.getTitle(), resumeSkills, projectSkills);
    }

    private AnalysisSnapshot analyzeJd(String jdText, ResumeSnapshot resumeSnapshot) {
        List<String> jdKeywords = extractKeywords(jdText);
        Set<String> resumeKeywords = new LinkedHashSet<>();
        resumeKeywords.addAll(resumeSnapshot.resumeSkills());
        resumeKeywords.addAll(resumeSnapshot.projectSkills());

        List<String> matched = jdKeywords.stream()
                .filter(keyword -> containsIgnoreCase(resumeKeywords, keyword))
                .distinct()
                .toList();
        List<String> missing = jdKeywords.stream()
                .filter(keyword -> !containsIgnoreCase(resumeKeywords, keyword))
                .distinct()
                .limit(6)
                .toList();

        BigDecimal score;
        if (jdKeywords.isEmpty()) {
            score = new BigDecimal("60");
        } else {
            double raw = 40 + matched.size() * 12 - missing.size() * 3;
            raw = Math.max(25, Math.min(95, raw));
            score = BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
        }

        String summary = buildAnalysisSummary(resumeSnapshot, matched, missing, score);
        return new AnalysisSnapshot(score, jdKeywords, missing, summary);
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
                    .filter(token -> token.length() >= 2 && token.length() <= 12)
                    .filter(token -> token.matches(".*[\\u4e00-\\u9fa5A-Za-z].*"))
                    .limit(6)
                    .forEach(keywords::add);
        }
        return new ArrayList<>(keywords);
    }

    private String buildAnalysisSummary(ResumeSnapshot resumeSnapshot, List<String> matched, List<String> missing, BigDecimal score) {
        String matchedText = matched.isEmpty() ? "当前简历里没有明显命中 JD 关键词" : "已命中：" + String.join("、", matched);
        String missingText = missing.isEmpty() ? "当前没有明显短板关键词，可继续准备真实案例和量化结果。" : "建议补足：" + String.join("、", missing);
        String resumeText = resumeSnapshot.resumeTitle() == null ? "当前未绑定简历，匹配度仅基于 JD 本身估算。" : "当前绑定简历为《" + resumeSnapshot.resumeTitle() + "》。";
        return resumeText + matchedText + "；" + missingText + "；综合匹配度约 " + score.stripTrailingZeros().toPlainString() + " 分。";
    }

    private void createEvent(Long applicationId, Long userId, String eventType, String title,
                             String content, LocalDateTime eventTime, String result,
                             Integer interviewRound, String interviewer, List<String> feedbackTags) {
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(applicationId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTitle(title);
        event.setContent(content);
        event.setEventTime(eventTime);
        event.setResult(result);
        event.setInterviewRound(interviewRound);
        event.setInterviewer(interviewer);
        event.setFeedbackTags(feedbackTags == null ? null : String.join(",", feedbackTags));
        jobApplicationEventMapper.insert(event);
    }

    private List<JobApplicationVO> buildVos(List<JobApplication> applications, boolean includeEvents) {
        return applications.stream().map(application -> buildVo(application, includeEvents)).toList();
    }

    private JobApplicationVO buildVo(JobApplication application, boolean includeEvents) {
        ResumeFile resume = application.getResumeFileId() == null ? null : resumeFileMapper.selectById(application.getResumeFileId());
        List<JobApplicationEventVO> events = includeEvents
                ? loadEvents(application.getId()).stream()
                        .map(event -> JobApplicationEventVO.builder()
                                .id(event.getId())
                                .eventType(event.getEventType())
                                .title(event.getTitle())
                                .content(event.getContent())
                                .eventTime(event.getEventTime())
                                .result(event.getResult())
                                .interviewRound(event.getInterviewRound())
                                .interviewer(event.getInterviewer())
                                .feedbackTags(splitComma(event.getFeedbackTags()))
                                .build())
                        .toList()
                : List.of();

        return JobApplicationVO.builder()
                .id(application.getId())
                .resumeFileId(application.getResumeFileId())
                .resumeTitle(resume == null ? null : resume.getTitle())
                .company(application.getCompany())
                .jobTitle(application.getJobTitle())
                .city(application.getCity())
                .source(application.getSource())
                .jdText(application.getJdText())
                .status(application.getStatus())
                .matchScore(application.getMatchScore())
                .jdKeywords(splitComma(application.getJdKeywords()))
                .missingKeywords(splitComma(application.getMissingKeywords()))
                .analysisSummary(application.getAnalysisSummary())
                .reviewSuggestion(application.getReviewSuggestion())
                .nextStepSuggestion(application.getNextStepSuggestion())
                .applyDate(application.getApplyDate())
                .nextStepDate(application.getNextStepDate())
                .updateTime(application.getUpdateTime())
                .events(events)
                .build();
    }

    private List<JobApplicationEvent> loadEvents(Long applicationId) {
        return jobApplicationEventMapper.selectList(new LambdaQueryWrapper<JobApplicationEvent>()
                .eq(JobApplicationEvent::getApplicationId, applicationId)
                .orderByDesc(JobApplicationEvent::getEventTime)
                .orderByDesc(JobApplicationEvent::getId));
    }

    private void refreshSuggestions(JobApplication application) {
        List<JobApplicationEvent> events = loadEvents(application.getId());
        AnalysisSnapshot analysis = toAnalysisSnapshot(application);
        application.setReviewSuggestion(buildReviewSuggestion(application.getStatus(), analysis, events));
        application.setNextStepSuggestion(buildNextStepSuggestion(application.getStatus(), analysis, application.getNextStepDate(), events));
        jobApplicationMapper.updateById(application);
    }

    private AnalysisSnapshot toAnalysisSnapshot(JobApplication application) {
        return new AnalysisSnapshot(
                application.getMatchScore(),
                splitComma(application.getJdKeywords()),
                splitComma(application.getMissingKeywords()),
                application.getAnalysisSummary());
    }

    private String buildReviewSuggestion(String status, AnalysisSnapshot analysis, List<JobApplicationEvent> events) {
        JobApplicationEvent latestInterview = events.stream()
                .filter(event -> "interview".equals(event.getEventType()))
                .findFirst()
                .orElse(null);
        String gapText = analysis.missingKeywords().isEmpty()
                ? "当前没有明显的关键词缺口，重点改成准备项目案例和追问。"
                : "下一轮前优先补 " + String.join("、", analysis.missingKeywords().stream().limit(3).toList()) + "。";
        if ("rejected".equals(status)) {
            return latestInterview != null && StringUtils.hasText(latestInterview.getContent())
                    ? "这次投递可以重点复盘：" + abbreviate(latestInterview.getContent(), 120)
                    : "这次投递已经结束，先回看面试过程里暴露出来的短板，再调整下一批岗位准备。";
        }
        if ("offer".equals(status)) {
            return "这条投递已经进入结果阶段，回看面试里最有效的项目表达和关键词命中，复用到下一批岗位。";
        }
        if (latestInterview != null) {
            String feedbackText = splitComma(latestInterview.getFeedbackTags()).isEmpty()
                    ? gapText
                    : "本轮反馈集中在 " + String.join("、", splitComma(latestInterview.getFeedbackTags())) + "。";
            return feedbackText + " 结合这次面试记录，把下一轮要补的项目表达和知识点再收紧一轮。";
        }
        if ("applied".equals(status) || "written".equals(status) || "interview".equals(status)) {
            return gapText + " 在推进中的岗位里，优先准备最可能被追问的项目案例和结果数据。";
        }
        return "先把 JD 缺口和项目表达对齐，再决定什么时候正式推进这条投递。";
    }

    private String buildNextStepSuggestion(String status, AnalysisSnapshot analysis, LocalDate nextStepDate, List<JobApplicationEvent> events) {
        String nextDateText = nextStepDate == null ? "" : "，目标时间是 " + nextStepDate;
        if ("saved".equals(status)) {
            return "先确认简历版本和 JD 缺口，再完成正式投递" + nextDateText + "。";
        }
        if ("applied".equals(status)) {
            return "投递已经发出，下一步去记录反馈窗口，并准备一面最可能问到的项目和缺口关键词" + nextDateText + "。";
        }
        if ("written".equals(status)) {
            return "当前阶段先完成笔试或作业，再把题目和薄弱点沉淀成后续复盘素材" + nextDateText + "。";
        }
        if ("interview".equals(status)) {
            JobApplicationEvent latestInterview = events.stream()
                    .filter(event -> "interview".equals(event.getEventType()))
                    .findFirst()
                    .orElse(null);
            if (latestInterview != null && latestInterview.getInterviewRound() != null) {
                return "先围绕第 " + latestInterview.getInterviewRound() + " 轮反馈补准备，再安排下一轮面试前的专项复盘" + nextDateText + "。";
            }
            return "当前重点是把最新一轮面试问题和反馈整理出来，确保下一轮前能针对性补准备" + nextDateText + "。";
        }
        if ("offer".equals(status)) {
            return "这条投递已经拿到结果，接下来记录最终结论和关键经验，沉淀到下一批岗位。";
        }
        return "这条投递已经结束，先整理淘汰原因和反馈标签，再更新下一批岗位策略。";
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("saved", "applied", "written", "interview", "offer", "rejected").contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported application status");
        }
        return normalized;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "saved" -> "待投递";
            case "applied" -> "已投递";
            case "written" -> "笔试 / 作业";
            case "interview" -> "面试中";
            case "offer" -> "已拿 Offer";
            case "rejected" -> "已淘汰";
            default -> status;
        };
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

    private boolean containsIgnoreCase(Set<String> values, String keyword) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(keyword));
    }

    private String abbreviate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private record ResumeSnapshot(Long resumeId, String resumeTitle, List<String> resumeSkills, List<String> projectSkills) {
    }

    private record AnalysisSnapshot(BigDecimal matchScore, List<String> jdKeywords, List<String> missingKeywords, String summary) {
    }
}
