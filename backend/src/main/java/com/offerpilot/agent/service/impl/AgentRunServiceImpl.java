package com.offerpilot.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.entity.AgentRun;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.agent.service.AgentRunService;
import com.offerpilot.agent.vo.AgentRunVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunServiceImpl implements AgentRunService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentRunMapper agentRunMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AgentRunVO createRun(Long userId, AgentRunCreateRequest request) {
        RunBlueprint blueprint = buildBlueprint(request);
        AgentRun run = new AgentRun();
        run.setUserId(userId);
        run.setAgentType(normalize(request.getAgentType()));
        run.setTriggerSource(normalize(request.getTriggerSource()));
        run.setStatus("completed");
        run.setTitle(blueprint.title());
        run.setSummary(blueprint.summary());
        run.setUserPrompt(trimToNull(request.getUserPrompt()));
        run.setContextRefsJson(writeList(request.getContextRefs()));
        run.setStreamMode(StringUtils.hasText(request.getStreamMode()) ? request.getStreamMode().trim() : "sync");
        run.setResultPayloadJson(writePayload(blueprint.recommendations(), blueprint.checkpoints()));
        run.setNextActionPath(blueprint.nextActionPath());
        run.setRequiresApproval(blueprint.requiresApproval() ? 1 : 0);
        agentRunMapper.insert(run);
        return buildVo(run);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentRunVO> listRuns(Long userId) {
        return agentRunMapper.selectList(new LambdaQueryWrapper<AgentRun>()
                        .eq(AgentRun::getUserId, userId)
                        .orderByDesc(AgentRun::getUpdateTime)
                        .orderByDesc(AgentRun::getId))
                .stream()
                .map(this::buildVo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentRunVO detail(Long userId, Long runId) {
        AgentRun run = agentRunMapper.selectById(runId);
        if (run == null || !run.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "agent run not found");
        }
        return buildVo(run);
    }

    private RunBlueprint buildBlueprint(AgentRunCreateRequest request) {
        String agentType = normalize(request.getAgentType());
        String triggerSource = normalize(request.getTriggerSource());
        List<String> contextRefs = request.getContextRefs() == null ? List.of() : request.getContextRefs().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        String prompt = trimToNull(request.getUserPrompt());

        return switch (agentType) {
            case "study_planner" -> blueprint(
                    "学习计划代理",
                    "已根据当前画像、复盘节奏和上下文生成下一轮训练建议。",
                    mergeRecommendations(
                            List.of("先处理到期待复盘，再安排新的专项训练。", "把低分点拆成 2-3 个可执行任务，避免计划过长。"),
                            prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入下一轮计划排序。") : List.of(),
                            contextRefsText(contextRefs, "优先参考这些上下文：")),
                    List.of("确认今日训练目标", "生成可执行任务", "如有写操作则进入待审批"),
                    "/study-plan",
                    true);
            case "job_prep" -> blueprint(
                    "JD 备面代理",
                    "已整理这次岗位准备的重点缺口、项目表达和模拟前动作。",
                    mergeRecommendations(
                            List.of("先完成 JD 备面，再把结果带入一轮模拟面试。", "围绕缺口关键词准备 1 个项目例子和 1 个原理解释。"),
                            contextRefsText(contextRefs, "当前引用的上下文：")),
                    List.of("整理 JD 缺口", "准备项目表达", "启动模拟或投递动作"),
                    "/interview",
                    true);
            case "recording_review" -> blueprint(
                    "录音复盘代理",
                    "已把录音复盘的结论整理成后续训练动作和复听重点。",
                    mergeRecommendations(
                            List.of("先回听薄弱片段，再改写成结构化口语答案。", "把复盘结果沉淀到下一轮模拟和错题复习里。"),
                            contextRefsText(contextRefs, "本次重点参考：")),
                    List.of("查看转写片段", "提取薄弱点", "确认是否转成正式训练动作"),
                    "/interview",
                    true);
            case "resume_coach" -> blueprint(
                    "简历教练代理",
                    "已按当前岗位目标整理简历修改与项目表达建议。",
                    mergeRecommendations(
                            List.of("先收紧标题和摘要，再调整项目顺序。", "优先补充最贴近岗位的量化结果与关键词。"),
                            contextRefsText(contextRefs, "当前基于以下材料：")),
                    List.of("确认目标岗位", "生成简历修改点", "决定是否写回简历版本"),
                    "/resume",
                    true);
            case "application_strategist" -> blueprint(
                    "投递策略代理",
                    "已根据当前投递状态和反馈节奏整理下一步推进建议。",
                    mergeRecommendations(
                            List.of("优先推进最接近面试的岗位，避免分散精力。", "把面试反馈标签同步到下一批岗位筛选标准。"),
                            contextRefsText(contextRefs, "本次参考的岗位或反馈：")),
                    List.of("查看推进优先级", "确认下一步动作", "必要时进入待审批"),
                    "/applications",
                    true);
            case "interview_review" -> blueprint(
                    "面试复盘代理",
                    "已根据本轮面试结果整理追问重点、低分点和下一轮训练建议。",
                    mergeRecommendations(
                            List.of("先处理低分题，再安排一轮同主题模拟。", "把薄弱点转成错题或复习任务，避免只停留在摘要层。"),
                            contextRefsText(contextRefs, "复盘引用了这些上下文：")),
                    List.of("汇总面试结果", "提取低分点", "决定是否刷新训练计划"),
                    "/interview",
                    true);
            case "realtime_copilot" -> blueprint(
                    "实时 Copilot 代理",
                    "已生成会前准备清单，后续可以继续接实时建议流。",
                    mergeRecommendations(
                            List.of("先完成 Copilot Prep，再进入实时阶段。", "把当前岗位、简历和 JD 统一成可速读的会前提纲。"),
                            contextRefsText(contextRefs, "Prep 阶段引用：")),
                    List.of("会前准备", "连接实时会话", "面后复盘回写"),
                    "/interview",
                    false);
            default -> blueprint(
                    "协调代理",
                    "已生成一份统一的下一步动作清单，可继续下钻到具体模块。",
                    mergeRecommendations(
                            List.of("先确认你现在要推进的是训练、备面、简历还是投递。", "把结果写入对应模块，而不是停留在对话摘要。"),
                            contextRefsText(contextRefs, "当前上下文：")),
                    List.of("识别任务类型", "路由到具体能力", "如涉及写操作则等待审批"),
                    "/dashboard",
                    false);
        };
    }

    private AgentRunVO buildVo(AgentRun run) {
        JsonNode payload = readPayload(run.getResultPayloadJson());
        return AgentRunVO.builder()
                .id(run.getId())
                .agentType(run.getAgentType())
                .triggerSource(run.getTriggerSource())
                .status(run.getStatus())
                .title(run.getTitle())
                .summary(run.getSummary())
                .userPrompt(run.getUserPrompt())
                .contextRefs(readList(run.getContextRefsJson()))
                .streamMode(run.getStreamMode())
                .recommendations(readPayloadArray(payload, "recommendations"))
                .checkpoints(readPayloadArray(payload, "checkpoints"))
                .nextActionPath(run.getNextActionPath())
                .requiresApproval(Integer.valueOf(1).equals(run.getRequiresApproval()))
                .updateTime(run.getUpdateTime())
                .build();
    }

    private RunBlueprint blueprint(String title, String summary, List<String> recommendations, List<String> checkpoints,
                                   String nextActionPath, boolean requiresApproval) {
        return new RunBlueprint(title, summary, recommendations, checkpoints, nextActionPath, requiresApproval);
    }

    private List<String> mergeRecommendations(List<String>... groups) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (List<String> group : groups) {
            merged.addAll(group);
        }
        return merged.stream().limit(5).toList();
    }

    private List<String> contextRefsText(List<String> contextRefs, String prefix) {
        if (contextRefs.isEmpty()) {
            return List.of();
        }
        return List.of(prefix + " " + String.join("、", contextRefs.stream().limit(3).toList()) + "。");
    }

    private String writePayload(List<String> recommendations, List<String> checkpoints) {
        try {
            return objectMapper.writeValueAsString(new ResultPayload(recommendations, checkpoints));
        } catch (JsonProcessingException e) {
            log.warn("Failed to write agent payload", e);
            return "{\"recommendations\":[],\"checkpoints\":[]}";
        }
    }

    private JsonNode readPayload(String raw) {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse agent payload", e);
            return objectMapper.createObjectNode();
        }
    }

    private List<String> readPayloadArray(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        });
        return result;
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write context refs", e);
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
            log.warn("Failed to read context refs", e);
            return List.of();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String abbreviate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private record RunBlueprint(String title, String summary, List<String> recommendations, List<String> checkpoints,
                                String nextActionPath, boolean requiresApproval) {
    }

    private record ResultPayload(List<String> recommendations, List<String> checkpoints) {
    }
}
