package com.offerpilot.interview.service;

import com.offerpilot.common.dto.PageResult;
import com.offerpilot.cards.vo.TodayCardsTaskVO;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.dto.InterviewStartRequest;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import java.util.List;

public interface InterviewService {

    InterviewCurrentQuestionVO start(Long userId, InterviewStartRequest request);

    InterviewCurrentQuestionVO current(Long userId, Long sessionId);

    InterviewAnswerVO answer(Long userId, InterviewAnswerRequest request);

    InterviewDetailVO detail(Long userId, Long sessionId);

    PageResult<InterviewHistoryVO> history(Long userId, String direction, int pageNum, int pageSize);

    List<InterviewHistoryVO> trendData(Long userId, int limit);

    InterviewDetailVO generateCards(Long userId, Long sessionId);

    TodayCardsTaskVO activateCards(Long userId, Long sessionId);
}
