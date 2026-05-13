package com.offerpilot.data.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.mapper.CategoryMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.data.dto.ImportResultVO;
import com.offerpilot.data.dto.QuestionImportRow;
import com.offerpilot.data.service.DataImportService;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public ImportResultVO importQuestions(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 .xlsx / .xls 格式");
        }

        Map<String, Long> categoryMap = buildCategoryNameMap();
        List<String> errors = new ArrayList<>();
        List<Question> toInsert = new ArrayList<>();
        int[] counter = {0, 0}; // [success, fail]

        try {
            EasyExcel.read(file.getInputStream(), QuestionImportRow.class, new ReadListener<QuestionImportRow>() {
                private int rowNum = 0;

                @Override
                public void invoke(QuestionImportRow row, AnalysisContext context) {
                    rowNum++;
                    try {
                        validateRow(row, rowNum);

                        Question q = new Question();
                        q.setTitle(row.getTitle().trim());
                        q.setDifficulty(row.getDifficulty() != null ? row.getDifficulty().trim().toLowerCase() : "medium");
                        q.setType(row.getType());
                        q.setFrequency(row.getFrequency() == null ? 0 : row.getFrequency());
                        q.setJobDirection(row.getJobDirection());
                        q.setApplicableScope(row.getApplicableScope());
                        q.setTags(row.getTags());
                        q.setStandardAnswer(row.getStandardAnswer());
                        q.setInterviewAnswer(row.getInterviewAnswer());
                        q.setFollowUpSuggestions(row.getFollowUpSuggestions());
                        q.setCommonMistakes(row.getCommonMistakes());

                        // Resolve category
                        if (row.getCategoryName() != null && !row.getCategoryName().isBlank()) {
                            Long catId = categoryMap.get(row.getCategoryName().trim());
                            if (catId == null) {
                                errors.add("第 " + rowNum + " 行：分类「" + row.getCategoryName() + "」不存在");
                                counter[1]++;
                                return;
                            }
                            q.setCategoryId(catId);
                        } else {
                            errors.add("第 " + rowNum + " 行：分类名称不能为空");
                            counter[1]++;
                            return;
                        }

                        toInsert.add(q);
                    } catch (Exception e) {
                        errors.add("第 " + rowNum + " 行：" + e.getMessage());
                        counter[1]++;
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // Batch insert
                    for (Question q : toInsert) {
                        try {
                            questionMapper.insert(q);
                            counter[0]++;
                        } catch (Exception e) {
                            errors.add("插入失败：" + q.getTitle() + " - " + e.getMessage());
                            counter[1]++;
                        }
                    }
                }
            }).sheet().doRead();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件解析失败：" + e.getMessage());
        }

        return ImportResultVO.builder()
                .successCount(counter[0])
                .failCount(counter[1])
                .errors(errors)
                .build();
    }

    private void validateRow(QuestionImportRow row, int rowNum) {
        if (row.getTitle() == null || row.getTitle().isBlank()) {
            throw new IllegalArgumentException("题目标题不能为空");
        }
        if (row.getDifficulty() != null) {
            String d = row.getDifficulty().trim().toLowerCase();
            if (!d.equals("easy") && !d.equals("medium") && !d.equals("hard")) {
                throw new IllegalArgumentException("难度必须为 easy/medium/hard");
            }
        }
    }

    private Map<String, Long> buildCategoryNameMap() {
        Map<String, Long> map = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) {
            map.put(c.getName(), c.getId());
        }
        return map;
    }
}
