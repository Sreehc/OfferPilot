package com.bytecoach.interview.service;

import com.bytecoach.common.dto.PageResult;
import com.bytecoach.interview.dto.InterviewAnswerRequest;
import com.bytecoach.interview.dto.InterviewStartRequest;
import com.bytecoach.interview.vo.InterviewAnswerVO;
import com.bytecoach.interview.vo.InterviewCurrentQuestionVO;
import com.bytecoach.interview.vo.InterviewDetailVO;
import com.bytecoach.interview.vo.InterviewHistoryVO;
import java.util.List;

public interface InterviewService {

    InterviewCurrentQuestionVO start(Long userId, InterviewStartRequest request);

    InterviewCurrentQuestionVO current(Long userId, Long sessionId);

    InterviewAnswerVO answer(Long userId, InterviewAnswerRequest request);

    InterviewDetailVO detail(Long userId, Long sessionId);

    PageResult<InterviewHistoryVO> history(Long userId, String direction, int pageNum, int pageSize);

    List<InterviewHistoryVO> trendData(Long userId, int limit);
}
