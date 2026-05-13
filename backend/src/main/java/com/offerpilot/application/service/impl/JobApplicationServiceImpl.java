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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        jobApplicationMapper.insert(application);

        createEvent(application.getId(), userId, "note", "已创建投递记录", "已完成岗位录入和 JD 初步分析。", LocalDateTime.now(), null);
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
        jobApplicationMapper.updateById(application);
        createEvent(applicationId, userId, "status_change", "状态更新为「" + statusLabel(application.getStatus()) + "」",
                StringUtils.hasText(request.getNote()) ? request.getNote() : "已更新投递状态。", LocalDateTime.now(), application.getStatus());
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
                request.getResult());
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
        jobApplicationMapper.updateById(application);
        createEvent(applicationId, userId, "analysis", "已刷新 JD 分析", analysis.summary(), LocalDateTime.now(), null);
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
                             String content, LocalDateTime eventTime, String result) {
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(applicationId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTitle(title);
        event.setContent(content);
        event.setEventTime(eventTime);
        event.setResult(result);
        jobApplicationEventMapper.insert(event);
    }

    private List<JobApplicationVO> buildVos(List<JobApplication> applications, boolean includeEvents) {
        return applications.stream().map(application -> buildVo(application, includeEvents)).toList();
    }

    private JobApplicationVO buildVo(JobApplication application, boolean includeEvents) {
        ResumeFile resume = application.getResumeFileId() == null ? null : resumeFileMapper.selectById(application.getResumeFileId());
        List<JobApplicationEventVO> events = includeEvents
                ? jobApplicationEventMapper.selectList(new LambdaQueryWrapper<JobApplicationEvent>()
                                .eq(JobApplicationEvent::getApplicationId, application.getId())
                                .orderByDesc(JobApplicationEvent::getEventTime)
                                .orderByDesc(JobApplicationEvent::getId))
                        .stream()
                        .map(event -> JobApplicationEventVO.builder()
                                .id(event.getId())
                                .eventType(event.getEventType())
                                .title(event.getTitle())
                                .content(event.getContent())
                                .eventTime(event.getEventTime())
                                .result(event.getResult())
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
                .applyDate(application.getApplyDate())
                .nextStepDate(application.getNextStepDate())
                .updateTime(application.getUpdateTime())
                .events(events)
                .build();
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

    private record ResumeSnapshot(Long resumeId, String resumeTitle, List<String> resumeSkills, List<String> projectSkills) {
    }

    private record AnalysisSnapshot(BigDecimal matchScore, List<String> jdKeywords, List<String> missingKeywords, String summary) {
    }
}
