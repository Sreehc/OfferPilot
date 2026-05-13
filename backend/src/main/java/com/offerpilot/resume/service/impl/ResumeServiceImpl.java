package com.offerpilot.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.knowledge.service.DocumentParserService;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.offerpilot.resume.service.ResumeService;
import com.offerpilot.resume.vo.ResumeFileVO;
import com.offerpilot.resume.vo.ResumeProjectQuestionVO;
import com.offerpilot.resume.vo.ResumeProjectVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka",
            "RabbitMQ", "Elasticsearch", "Docker", "Kubernetes", "Linux", "Nginx",
            "JVM", "Netty", "MongoDB", "Git", "Maven", "Vue", "React", "Dubbo");

    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(?im).*(大学|学院|本科|硕士|博士|专业|学历|education).*");
    private static final Pattern PROJECT_SPLIT_PATTERN = Pattern.compile("(?m)(?=^(?:项目|Project|PROJECT|[0-9一二三四五六七八九十]+[、.]?项目).*$)");

    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final FileStorageService fileStorageService;
    private final UploadPolicyService uploadPolicyService;
    private final DocumentParserService documentParserService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResumeFileVO upload(Long userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        uploadPolicyService.validate(StorageDirectory.RESUME, originalFilename, file.getContentType(), file.getSize());
        if (!documentParserService.isSupported(originalFilename)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "简历文件仅支持: " + String.join(", ", documentParserService.supportedExtensions()));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "简历读取失败: " + e.getMessage());
        }

        List<String> chunks = documentParserService.parse(new ByteArrayInputStream(bytes), originalFilename);
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "简历内容为空或无法解析");
        }
        String rawText = String.join("\n\n", chunks);
        StoredFile storedFile = fileStorageService.store(StorageDirectory.RESUME, originalFilename, bytes, file.getContentType());

        ResumeParseSnapshot snapshot = parseResume(rawText, stripExtension(originalFilename));

        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setUserId(userId);
        resumeFile.setTitle(stripExtension(originalFilename));
        resumeFile.setFileUrl(storedFile.getStorageKey());
        resumeFile.setFileType(fileTypeFromName(originalFilename));
        resumeFile.setParseStatus("parsed");
        resumeFile.setRawText(rawText);
        resumeFile.setSummary(snapshot.summary());
        resumeFile.setSkills(String.join(",", snapshot.skills()));
        resumeFile.setEducation(snapshot.education());
        resumeFile.setSelfIntro(snapshot.selfIntro());
        resumeFile.setInterviewResumeText(snapshot.interviewResume());
        resumeFile.setLastParsedAt(LocalDateTime.now());
        resumeFileMapper.insert(resumeFile);

        for (int i = 0; i < snapshot.projects().size(); i++) {
            ParsedProject parsed = snapshot.projects().get(i);
            ResumeProject project = new ResumeProject();
            project.setResumeFileId(resumeFile.getId());
            project.setUserId(userId);
            project.setProjectName(parsed.projectName());
            project.setRoleName(parsed.roleName());
            project.setTechStack(parsed.techStack());
            project.setResponsibility(parsed.responsibility());
            project.setAchievement(parsed.achievement());
            project.setProjectSummary(parsed.projectSummary());
            project.setFollowUpQuestionsJson(writeJson(parsed.followUpQuestions()));
            project.setRiskHints(String.join(",", parsed.riskHints()));
            project.setSortOrder(i + 1);
            resumeProjectMapper.insert(project);
        }

        log.info("Resume uploaded: userId={}, resumeId={}, title={}", userId, resumeFile.getId(), resumeFile.getTitle());
        return buildDetail(resumeFile, loadProjects(resumeFile.getId()));
    }

    @Override
    public List<ResumeFileVO> list(Long userId) {
        return resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getUserId, userId)
                        .orderByDesc(ResumeFile::getUpdateTime))
                .stream()
                .map(item -> ResumeFileVO.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .fileType(item.getFileType())
                        .parseStatus(item.getParseStatus())
                        .summary(item.getSummary())
                        .skills(splitComma(item.getSkills()))
                        .education(item.getEducation())
                        .selfIntro(item.getSelfIntro())
                        .interviewResumeText(item.getInterviewResumeText())
                        .lastParsedAt(item.getLastParsedAt())
                        .updateTime(item.getUpdateTime())
                        .projects(List.of())
                        .build())
                .toList();
    }

    @Override
    public ResumeFileVO latest(Long userId) {
        ResumeFile file = resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getUpdateTime)
                .last("LIMIT 1"));
        return file == null ? null : buildDetail(file, loadProjects(file.getId()));
    }

    @Override
    public ResumeFileVO detail(Long userId, Long resumeId) {
        ResumeFile file = getOwnedResume(userId, resumeId);
        return buildDetail(file, loadProjects(resumeId));
    }

    @Override
    public List<ResumeProjectVO> projectQuestions(Long userId, Long resumeId) {
        getOwnedResume(userId, resumeId);
        return loadProjects(resumeId);
    }

    @Override
    public String selfIntro(Long userId, Long resumeId) {
        return getOwnedResume(userId, resumeId).getSelfIntro();
    }

    @Override
    public String interviewResume(Long userId, Long resumeId) {
        return getOwnedResume(userId, resumeId).getInterviewResumeText();
    }

    private ResumeFile getOwnedResume(Long userId, Long resumeId) {
        ResumeFile file = resumeFileMapper.selectById(resumeId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume file not found");
        }
        return file;
    }

    private ResumeFileVO buildDetail(ResumeFile file, List<ResumeProjectVO> projects) {
        return ResumeFileVO.builder()
                .id(file.getId())
                .title(file.getTitle())
                .fileType(file.getFileType())
                .parseStatus(file.getParseStatus())
                .summary(file.getSummary())
                .skills(splitComma(file.getSkills()))
                .education(file.getEducation())
                .selfIntro(file.getSelfIntro())
                .interviewResumeText(file.getInterviewResumeText())
                .lastParsedAt(file.getLastParsedAt())
                .updateTime(file.getUpdateTime())
                .projects(projects)
                .build();
    }

    private List<ResumeProjectVO> loadProjects(Long resumeId) {
        return resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                        .eq(ResumeProject::getResumeFileId, resumeId)
                        .orderByAsc(ResumeProject::getSortOrder)
                        .orderByAsc(ResumeProject::getId))
                .stream()
                .map(project -> ResumeProjectVO.builder()
                        .id(project.getId())
                        .projectName(project.getProjectName())
                        .roleName(project.getRoleName())
                        .techStack(project.getTechStack())
                        .responsibility(project.getResponsibility())
                        .achievement(project.getAchievement())
                        .projectSummary(project.getProjectSummary())
                        .followUpQuestions(parseQuestions(project.getFollowUpQuestionsJson()))
                        .riskHints(splitComma(project.getRiskHints()))
                        .build())
                .toList();
    }

    private ResumeParseSnapshot parseResume(String rawText, String fallbackTitle) {
        List<String> normalizedLines = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        List<String> skills = extractSkills(rawText);
        String education = extractEducation(normalizedLines);
        List<ParsedProject> projects = extractProjects(rawText, normalizedLines, skills, fallbackTitle);
        String summary = buildSummary(normalizedLines, projects, skills, fallbackTitle);
        String selfIntro = buildSelfIntro(fallbackTitle, skills, projects);
        String interviewResume = buildInterviewResume(fallbackTitle, summary, skills, projects);
        return new ResumeParseSnapshot(summary, skills, education, selfIntro, interviewResume, projects);
    }

    private List<String> extractSkills(String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);
        return SKILL_KEYWORDS.stream()
                .filter(skill -> lower.contains(skill.toLowerCase(Locale.ROOT)))
                .distinct()
                .limit(10)
                .toList();
    }

    private String extractEducation(List<String> lines) {
        List<String> matched = new ArrayList<>();
        for (String line : lines) {
            if (EDUCATION_PATTERN.matcher(line).matches()) {
                matched.add(line);
            }
            if (matched.size() >= 3) {
                break;
            }
        }
        return matched.isEmpty() ? "未在简历中稳定识别到教育信息，建议补充学校 / 专业 / 学历。" : String.join("；", matched);
    }

    private List<ParsedProject> extractProjects(String rawText, List<String> lines, List<String> globalSkills, String fallbackTitle) {
        List<String> segments = new ArrayList<>();
        Matcher matcher = PROJECT_SPLIT_PATTERN.matcher(rawText);
        int previous = -1;
        while (matcher.find()) {
            if (previous >= 0) {
                segments.add(rawText.substring(previous, matcher.start()).trim());
            }
            previous = matcher.start();
        }
        if (previous >= 0) {
            segments.add(rawText.substring(previous).trim());
        }
        if (segments.isEmpty()) {
            String fallbackSegment = lines.stream().limit(12).collect(Collectors.joining("\n"));
            segments.add(StringUtils.hasText(fallbackSegment) ? fallbackSegment : fallbackTitle);
        }

        List<ParsedProject> projects = new ArrayList<>();
        for (String segment : segments.stream().filter(StringUtils::hasText).limit(4).toList()) {
            List<String> segmentLines = Arrays.stream(segment.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
            if (segmentLines.isEmpty()) {
                continue;
            }
            String projectName = normalizeProjectName(segmentLines.get(0), fallbackTitle);
            List<String> skills = extractSkills(segment);
            String techStack = skills.isEmpty() ? String.join(", ", globalSkills.stream().limit(4).toList()) : String.join(", ", skills);
            String roleName = findFirstLine(segmentLines, "角色", "岗位", "负责", "担任");
            String responsibility = findFirstLine(segmentLines, "负责", "职责", "参与", "设计", "实现");
            String achievement = findFirstLine(segmentLines, "提升", "优化", "降低", "%", "QPS", "TPS", "上线", "支撑", "落地");
            String projectSummary = abbreviate(String.join("；", segmentLines), 180);
            List<String> riskHints = buildRiskHints(techStack, responsibility, achievement);
            List<ResumeProjectQuestionVO> questions = buildQuestions(projectName, techStack, responsibility, achievement);
            projects.add(new ParsedProject(
                    projectName,
                    StringUtils.hasText(roleName) ? roleName : "项目核心负责人 / 主要贡献者",
                    StringUtils.hasText(techStack) ? techStack : "建议补充技术栈",
                    StringUtils.hasText(responsibility) ? responsibility : "建议补充你在项目中的职责与决策点",
                    StringUtils.hasText(achievement) ? achievement : "建议补充量化结果、性能收益或业务价值",
                    projectSummary,
                    questions,
                    riskHints));
        }
        return projects.stream()
                .sorted(Comparator.comparing(ParsedProject::projectName))
                .toList();
    }

    private String buildSummary(List<String> lines, List<ParsedProject> projects, List<String> skills, String fallbackTitle) {
        String projectNames = projects.stream().map(ParsedProject::projectName).limit(3).collect(Collectors.joining("、"));
        String skillText = skills.isEmpty() ? "Java 后端常用技术栈" : String.join("、", skills.stream().limit(6).toList());
        String lead = lines.isEmpty() ? fallbackTitle : abbreviate(lines.get(0), 40);
        return "简历核心信息已抽取，可围绕 " + skillText + " 和项目「" + projectNames + "」准备项目表达。当前摘要来源于简历标题/抬头「" + lead + "」。";
    }

    private String buildSelfIntro(String fallbackTitle, List<String> skills, List<ParsedProject> projects) {
        String skillText = skills.isEmpty() ? "Java 后端开发相关技术" : String.join("、", skills.stream().limit(6).toList());
        String projectText = projects.stream()
                .limit(2)
                .map(project -> project.projectName() + "（" + abbreviate(project.achievement(), 18) + "）")
                .collect(Collectors.joining("，"));
        return "您好，我目前主要围绕 " + skillText + " 做后端方向训练和项目实践。"
                + "简历中重点项目包括 " + (StringUtils.hasText(projectText) ? projectText : fallbackTitle)
                + "。我更关注在真实业务里如何做架构权衡、性能优化和问题排查，希望在面试中把项目价值和技术深度讲清楚。";
    }

    private String buildInterviewResume(String fallbackTitle, String summary, List<String> skills, List<ParsedProject> projects) {
        StringBuilder builder = new StringBuilder();
        builder.append("【面试版简历提纲】\n");
        builder.append("候选人定位：").append(fallbackTitle).append("\n");
        builder.append("技能关键词：").append(skills.isEmpty() ? "建议补充 Java / 中间件 / 数据库等关键词" : String.join("、", skills)).append("\n");
        builder.append("项目亮点摘要：").append(summary).append("\n\n");
        builder.append("【项目展开顺序】\n");
        for (ParsedProject project : projects) {
            builder.append("- ").append(project.projectName()).append("：")
                    .append(project.responsibility()).append("；结果/价值：")
                    .append(project.achievement()).append("\n");
        }
        builder.append("\n【建议面试表达】\n先用 30 秒讲项目背景，再用 60 秒讲你的职责、核心决策、技术权衡和结果数据，最后准备 2 到 3 个追问点。");
        return builder.toString();
    }

    private List<String> buildRiskHints(String techStack, String responsibility, String achievement) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        if (!StringUtils.hasText(techStack) || techStack.length() < 8) {
            hints.add("技术栈描述偏弱，建议补充核心框架和中间件");
        }
        if (!StringUtils.hasText(responsibility) || responsibility.length() < 12) {
            hints.add("职责不够具体，建议补充你负责的模块、决策和权衡");
        }
        if (!StringUtils.hasText(achievement) || !achievement.matches(".*(\\d|%|毫秒|万|QPS|TPS).*")) {
            hints.add("成果缺少量化数据，建议补充性能、稳定性或业务指标");
        }
        if (hints.isEmpty()) {
            hints.add("项目信息较完整，建议重点准备深挖追问和项目取舍");
        }
        return new ArrayList<>(hints);
    }

    private List<ResumeProjectQuestionVO> buildQuestions(String projectName, String techStack, String responsibility, String achievement) {
        List<String> techItems = splitComma(techStack);
        String firstTech = techItems.isEmpty() ? "核心框架" : techItems.get(0);
        return List.of(
                ResumeProjectQuestionVO.builder()
                        .question("如果让你用 1 分钟介绍「" + projectName + "」，你会怎么讲背景、职责和结果？")
                        .intent("验证项目表达是否成体系")
                        .build(),
                ResumeProjectQuestionVO.builder()
                        .question("在「" + projectName + "」里，你为什么选择 " + firstTech + " 相关方案？当时权衡了什么？")
                        .intent("追问技术方案选择与取舍")
                        .build(),
                ResumeProjectQuestionVO.builder()
                        .question("你提到「" + abbreviate(achievement, 20) + "」，这个结果具体是怎么落地和验证的？")
                        .intent("验证结果真实性与度量方式")
                        .build());
    }

    private String normalizeProjectName(String raw, String fallbackTitle) {
        String normalized = raw.replaceAll("^[0-9一二三四五六七八九十]+[、.]?", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackTitle + " 项目";
        }
        return abbreviate(normalized, 40);
    }

    private String findFirstLine(List<String> lines, String... keywords) {
        for (String line : lines) {
            for (String keyword : keywords) {
                if (line.contains(keyword)) {
                    return line;
                }
            }
        }
        return lines.size() > 1 ? lines.get(1) : null;
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

    private List<ResumeProjectQuestionVO> parseQuestions(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<ResumeProjectQuestionVO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse resume project questions: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "简历结构化数据保存失败");
        }
    }

    private String stripExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return StringUtils.hasText(originalFilename) ? originalFilename : "resume";
        }
        return originalFilename.substring(0, originalFilename.lastIndexOf('.'));
    }

    private String fileTypeFromName(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String abbreviate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private record ResumeParseSnapshot(
            String summary,
            List<String> skills,
            String education,
            String selfIntro,
            String interviewResume,
            List<ParsedProject> projects) {
    }

    private record ParsedProject(
            String projectName,
            String roleName,
            String techStack,
            String responsibility,
            String achievement,
            String projectSummary,
            List<ResumeProjectQuestionVO> followUpQuestions,
            List<String> riskHints) {
    }
}
