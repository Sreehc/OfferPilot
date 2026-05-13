package com.offerpilot.question.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.question.dto.QuestionQuery;
import com.offerpilot.question.dto.QuestionUpsertRequest;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.question.service.QuestionService;
import com.offerpilot.question.vo.QuestionVO;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    private final CategoryService categoryService;

    @Override
    public PageResult<QuestionVO> listQuestions(QuestionQuery query) {
        Page<Question> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Question> result = page(page, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                .eq(query.getCategoryId() != null, Question::getCategoryId, query.getCategoryId())
                .eq(StringUtils.hasText(query.getDifficulty()), Question::getDifficulty, query.getDifficulty())
                .and(StringUtils.hasText(query.getKeyword()), keywordWrapper -> keywordWrapper
                        .like(Question::getTitle, query.getKeyword())
                        .or()
                        .like(Question::getTags, query.getKeyword()))
                .orderByDesc(Question::getUpdateTime));
        Map<Long, Category> categoryMap = categoryService.listByIds(
                        result.getRecords().stream().map(Question::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<QuestionVO> voList = result.getRecords().stream()
                .map(question -> toVO(question, categoryMap.get(question.getCategoryId())))
                .toList();
        return PageResult.<QuestionVO>builder()
                .records(voList)
                .total(result.getTotal())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .totalPages((int) result.getPages())
                .build();
    }

    @Override
    public QuestionVO getQuestionDetail(Long id) {
        Question question = getById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not found");
        }
        Category category = categoryService.getById(question.getCategoryId());
        return toVO(question, category);
    }

    @Override
    public QuestionVO createQuestion(QuestionUpsertRequest request) {
        Category category = categoryService.getRequiredById(request.getCategoryId());
        Question question = new Question();
        fillQuestion(question, request);
        save(question);
        return toVO(question, category);
    }

    @Override
    public QuestionVO updateQuestion(QuestionUpsertRequest request) {
        Question question = getById(request.getId());
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not found");
        }
        Category category = categoryService.getRequiredById(request.getCategoryId());
        fillQuestion(question, request);
        updateById(question);
        return toVO(question, category);
    }

    @Override
    public void deleteQuestion(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "question not found");
        }
    }

    private void fillQuestion(Question question, QuestionUpsertRequest request) {
        question.setTitle(request.getTitle());
        question.setCategoryId(request.getCategoryId());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setFrequency(request.getFrequency() == null ? 0 : request.getFrequency());
        question.setTags(request.getTags());
        question.setStandardAnswer(request.getStandardAnswer());
        question.setScoreStandard(request.getScoreStandard());
        question.setSource(request.getSource());
    }

    private QuestionVO toVO(Question question, Category category) {
        return QuestionVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .categoryId(question.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .type(question.getType())
                .difficulty(question.getDifficulty())
                .frequency(question.getFrequency())
                .tags(question.getTags())
                .standardAnswer(question.getStandardAnswer())
                .scoreStandard(question.getScoreStandard())
                .source(question.getSource())
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build();
    }
}
