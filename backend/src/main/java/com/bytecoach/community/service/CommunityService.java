package com.bytecoach.community.service;

import com.bytecoach.common.dto.PageResult;
import com.bytecoach.community.dto.CommunityAnswerUpsertRequest;
import com.bytecoach.community.dto.CommunityQuestionUpsertRequest;
import com.bytecoach.community.dto.CommunityVoteRequest;
import com.bytecoach.community.vo.CommunityAnswerVO;
import com.bytecoach.community.vo.CommunityQuestionVO;
import com.bytecoach.community.vo.LeaderboardEntryVO;
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
