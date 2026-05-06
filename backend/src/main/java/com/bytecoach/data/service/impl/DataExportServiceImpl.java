package com.bytecoach.data.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.mapper.CategoryMapper;
import com.bytecoach.data.dto.QuestionExportRow;
import com.bytecoach.data.dto.UserExportRow;
import com.bytecoach.data.service.DataExportService;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.mapper.UserMapper;
import com.bytecoach.wrong.entity.ReviewLog;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.ReviewLogMapper;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewRecordMapper interviewRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;

    @Override
    public void exportQuestions(HttpServletResponse response) {
        setResponseHeaders(response, "题库导出");

        Map<Long, String> categoryMap = buildCategoryMap();
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>().orderByAsc(Question::getId));

        List<QuestionExportRow> rows = questions.stream()
                .map(q -> QuestionExportRow.builder()
                        .id(q.getId())
                        .title(q.getTitle())
                        .categoryId(q.getCategoryId())
                        .categoryName(categoryMap.getOrDefault(q.getCategoryId(), ""))
                        .difficulty(q.getDifficulty())
                        .tags(q.getTags())
                        .standardAnswer(q.getStandardAnswer())
                        .createTime(q.getCreateTime() != null ? q.getCreateTime().format(FMT) : "")
                        .build())
                .toList();

        try {
            EasyExcel.write(response.getOutputStream(), QuestionExportRow.class).sheet("题库").doWrite(rows);
        } catch (IOException ex) {
            throw new IllegalStateException("导出题库失败", ex);
        }
    }

    @Override
    public void exportUsers(HttpServletResponse response) {
        setResponseHeaders(response, "用户列表导出");

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId));

        List<UserExportRow> rows = users.stream()
                .map(u -> UserExportRow.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .status(u.getStatus() == 1 ? "正常" : "封禁")
                        .createTime(u.getCreateTime() != null ? u.getCreateTime().format(FMT) : "")
                        .lastLoginTime(u.getLastLoginTime() != null ? u.getLastLoginTime().format(FMT) : "")
                        .build())
                .toList();

        try {
            EasyExcel.write(response.getOutputStream(), UserExportRow.class).sheet("用户列表").doWrite(rows);
        } catch (IOException ex) {
            throw new IllegalStateException("导出用户列表失败", ex);
        }
    }

    @Override
    public void exportMyData(Long userId, HttpServletResponse response) {
        setResponseHeaders(response, "个人数据导出");

        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            // Sheet 1: 面试记录
            List<InterviewSession> sessions = interviewSessionMapper.selectList(
                    new LambdaQueryWrapper<InterviewSession>()
                            .eq(InterviewSession::getUserId, userId)
                            .orderByDesc(InterviewSession::getCreateTime));
            Map<Long, Question> questionMap = new HashMap<>();
            for (Question question : questionMapper.selectList(null)) {
                questionMap.put(question.getId(), question);
            }

            List<Map<String, Object>> interviewRows = new ArrayList<>();
            for (InterviewSession s : sessions) {
                List<InterviewRecord> records = interviewRecordMapper.selectList(
                        new LambdaQueryWrapper<InterviewRecord>()
                                .eq(InterviewRecord::getSessionId, s.getId()));
                for (InterviewRecord r : records) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("面试方向", s.getDirection());
                    row.put("面试时间", s.getCreateTime() != null ? s.getCreateTime().format(FMT) : "");
                    row.put("总分", s.getTotalScore());
                    row.put("得分", r.getScore());
                    row.put("我的回答", r.getUserAnswer());
                    row.put("点评", r.getComment());
                    Question question = questionMap.get(r.getQuestionId());
                    row.put("标准答案", question != null ? question.getStandardAnswer() : "");
                    interviewRows.add(row);
                }
            }
            WriteSheet sheet1 = EasyExcel.writerSheet("面试记录")
                    .head(headOf("面试方向", "面试时间", "总分", "得分", "我的回答", "点评", "标准答案"))
                    .build();
            writer.write(interviewRows, sheet1);

            // Sheet 2: 错题本
            List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                    new LambdaQueryWrapper<WrongQuestion>()
                            .eq(WrongQuestion::getUserId, userId)
                            .orderByDesc(WrongQuestion::getCreateTime));

            List<Map<String, Object>> wrongRows = new ArrayList<>();
            for (WrongQuestion w : wrongs) {
                Map<String, Object> row = new HashMap<>();
                row.put("题目ID", w.getQuestionId());
                row.put("来源", w.getSourceType());
                row.put("掌握程度", w.getMasteryLevel());
                row.put("复习次数", w.getReviewCount());
                row.put("下次复习", w.getNextReviewDate() != null ? w.getNextReviewDate().toString() : "");
                row.put("错误原因", w.getErrorReason());
                wrongRows.add(row);
            }
            WriteSheet sheet2 = EasyExcel.writerSheet("错题本")
                    .head(headOf("题目ID", "来源", "掌握程度", "复习次数", "下次复习", "错误原因"))
                    .build();
            writer.write(wrongRows, sheet2);

            // Sheet 3: 复习记录
            List<ReviewLog> reviews = reviewLogMapper.selectList(
                    new LambdaQueryWrapper<ReviewLog>()
                            .eq(ReviewLog::getUserId, userId)
                            .orderByDesc(ReviewLog::getCreateTime));

            List<Map<String, Object>> reviewRows = new ArrayList<>();
            for (ReviewLog r : reviews) {
                Map<String, Object> row = new HashMap<>();
                row.put("错题ID", r.getWrongQuestionId());
                row.put("评分", r.getRating());
                row.put("复习时间", r.getCreateTime() != null ? r.getCreateTime().format(FMT) : "");
                row.put("EF变化", r.getEaseFactorBefore() + " → " + r.getEaseFactorAfter());
                row.put("间隔变化", r.getIntervalBefore() + "天 → " + r.getIntervalAfter() + "天");
                reviewRows.add(row);
            }
            WriteSheet sheet3 = EasyExcel.writerSheet("复习记录")
                    .head(headOf("错题ID", "评分", "复习时间", "EF变化", "间隔变化"))
                    .build();
            writer.write(reviewRows, sheet3);
        } catch (IOException ex) {
            throw new IllegalStateException("导出个人数据失败", ex);
        }
    }

    private void setResponseHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(filename + ".xlsx", StandardCharsets.UTF_8));
    }

    private Map<Long, String> buildCategoryMap() {
        Map<Long, String> map = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    private List<List<String>> headOf(String... headers) {
        List<List<String>> head = new ArrayList<>();
        for (String h : headers) {
            head.add(List.of(h));
        }
        return head;
    }
}
