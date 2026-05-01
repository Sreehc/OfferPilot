package com.bytecoach.question.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.question.dto.QuestionQuery;
import com.bytecoach.question.dto.QuestionUpsertRequest;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.vo.QuestionVO;
import java.util.List;

public interface QuestionService extends IService<Question> {
    PageResult<QuestionVO> listQuestions(QuestionQuery query);

    QuestionVO getQuestionDetail(Long id);

    QuestionVO createQuestion(QuestionUpsertRequest request);

    QuestionVO updateQuestion(QuestionUpsertRequest request);

    void deleteQuestion(Long id);
}
