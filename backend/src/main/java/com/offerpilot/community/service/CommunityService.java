package com.offerpilot.community.service;

import com.offerpilot.common.dto.PageResult;
import com.offerpilot.community.dto.CommunityAnswerUpsertRequest;
import com.offerpilot.community.dto.CommunityQuestionUpsertRequest;
import com.offerpilot.community.dto.CommunityVoteRequest;
import com.offerpilot.community.vo.CommunityAnswerVO;
import com.offerpilot.community.vo.CommunityQuestionVO;
import com.offerpilot.community.vo.LeaderboardEntryVO;
import java.util.List;

public interface CommunityService {
    Long createQuestion(CommunityQuestionUpsertRequest request);
    void updateQuestion(CommunityQuestionUpsertRequest request);
    void deleteQuestion(Long id);
    CommunityQuestionVO getQuestionDetail(Long id);
    PageResult<CommunityQuestionVO> listQuestions(int page, int size, String sort, Long categoryId, String keyword);
    Long submitAnswer(CommunityAnswerUpsertRequest request);
    void deleteAnswer(Long id);
    void acceptAnswer(Long questionId, Long answerId);
    void vote(CommunityVoteRequest request);
    List<LeaderboardEntryVO> getLeaderboard(int limit);
}
