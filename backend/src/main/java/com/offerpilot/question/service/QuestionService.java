package com.offerpilot.question.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.question.dto.QuestionQuery;
import com.offerpilot.question.dto.QuestionUpsertRequest;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.vo.QuestionVO;
import java.util.List;

public interface QuestionService extends IService<Question> {
    PageResult<QuestionVO> listQuestions(QuestionQuery query);

    QuestionVO getQuestionDetail(Long id);

    QuestionVO createQuestion(QuestionUpsertRequest request);

    QuestionVO updateQuestion(QuestionUpsertRequest request);

    void deleteQuestion(Long id);
}
