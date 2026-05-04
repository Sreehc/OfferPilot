package com.bytecoach.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.common.exception.BizException;
import com.bytecoach.common.result.PageResult;
import com.bytecoach.community.dto.CommunityAnswerUpsertRequest;
import com.bytecoach.community.dto.CommunityQuestionUpsertRequest;
import com.bytecoach.community.dto.CommunityVoteRequest;
import com.bytecoach.community.entity.CommunityAnswer;
import com.bytecoach.community.entity.CommunityQuestion;
import com.bytecoach.community.entity.CommunityVote;
import com.bytecoach.community.entity.UserStats;
import com.bytecoach.community.mapper.CommunityAnswerMapper;
import com.bytecoach.community.mapper.CommunityQuestionMapper;
import com.bytecoach.community.mapper.CommunityVoteMapper;
import com.bytecoach.community.mapper.UserStatsMapper;
import com.bytecoach.community.service.CommunityService;
import com.bytecoach.community.vo.CommunityAnswerVO;
import com.bytecoach.community.vo.CommunityQuestionVO;
import com.bytecoach.community.vo.LeaderboardEntryVO;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.security.util.SecurityUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityQuestionMapper questionMapper;
    private final CommunityAnswerMapper answerMapper;
    private final CommunityVoteMapper voteMapper;
    private final UserStatsMapper userStatsMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Long createQuestion(CommunityQuestionUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommunityQuestion question = new CommunityQuestion();
        question.setUserId(userId);
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setCategoryId(request.getCategoryId());
        question.setStatus("open");
        question.setUpvoteCount(0);
        question.setAnswerCount(0);
        questionMapper.insert(question);

        ensureUserStats(userId);
        userStatsMapper.update(null, new LambdaUpdateWrapper<UserStats>()
                .eq(UserStats::getUserId, userId)
                .setSql("community_questions = community_questions + 1"));

        return question.getId();
    }

    @Override
    @Transactional
    public void updateQuestion(CommunityQuestionUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommunityQuestion question = questionMapper.selectById(request.getId());
        if (question == null) throw new BizException("问题不存在");
        if (!question.getUserId().equals(userId)) throw new BizException("只能编辑自己的问题");

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setCategoryId(request.getCategoryId());
        questionMapper.updateById(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommunityQuestion question = questionMapper.selectById(id);
        if (question == null) throw new BizException("问题不存在");
        if (!question.getUserId().equals(userId)) throw new BizException("只能删除自己的问题");

        questionMapper.deleteById(id);
        answerMapper.delete(new LambdaQueryWrapper<CommunityAnswer>()
                .eq(CommunityAnswer::getQuestionId, id));
    }

    @Override
    public CommunityQuestionVO getQuestionDetail(Long id) {
        CommunityQuestion question = questionMapper.selectById(id);
        if (question == null) throw new BizException("问题不存在");

        CommunityQuestionVO vo = toQuestionVO(question);

        List<CommunityAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<CommunityAnswer>()
                        .eq(CommunityAnswer::getQuestionId, id)
                        .orderByDesc(CommunityAnswer::getIsAccepted)
                        .orderByDesc(CommunityAnswer::getUpvoteCount)
                        .orderByAsc(CommunityAnswer::getCreatedAt));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<CommunityAnswerVO> answerVOs = answers.stream()
                .map(a -> {
                    CommunityAnswerVO avo = toAnswerVO(a);
                    avo.setHasVoted(hasVoted(currentUserId, "answer", a.getId()));
                    return avo;
                })
                .collect(Collectors.toList());

        vo.setAnswers(answerVOs);
        vo.setAccepted(answers.stream().anyMatch(CommunityAnswer::getIsAccepted));
        return vo;
    }

    @Override
    public PageResult<CommunityQuestionVO> listQuestions(int page, int size, String sort, Long categoryId, String keyword) {
        LambdaQueryWrapper<CommunityQuestion> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(CommunityQuestion::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(CommunityQuestion::getTitle, keyword)
                    .or()
                    .like(CommunityQuestion::getContent, keyword));
        }

        if ("hot".equals(sort)) {
            wrapper.orderByDesc(CommunityQuestion::getUpvoteCount)
                    .orderByDesc(CommunityQuestion::getAnswerCount);
        } else {
            wrapper.orderByDesc(CommunityQuestion::getCreatedAt);
        }

        Page<CommunityQuestion> pageParam = new Page<>(page, size);
        Page<CommunityQuestion> result = questionMapper.selectPage(pageParam, wrapper);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<CommunityQuestionVO> voList = result.getRecords().stream()
                .map(q -> {
                    CommunityQuestionVO vo = toQuestionVO(q);
                    vo.setHasVoted(hasVoted(currentUserId, "question", q.getId()));
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal());
    }

    @Override
    @Transactional
    public Long submitAnswer(CommunityAnswerUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        CommunityQuestion question = questionMapper.selectById(request.getQuestionId());
        if (question == null) throw new BizException("问题不存在");

        CommunityAnswer answer = new CommunityAnswer();
        answer.setQuestionId(request.getQuestionId());
        answer.setUserId(userId);
        answer.setContent(request.getContent());
        answer.setIsAccepted(false);
        answer.setUpvoteCount(0);
        answerMapper.insert(answer);

        questionMapper.update(null, new LambdaUpdateWrapper<CommunityQuestion>()
                .eq(CommunityQuestion::getId, request.getQuestionId())
                .setSql("answer_count = answer_count + 1"));

        ensureUserStats(userId);
        userStatsMapper.update(null, new LambdaUpdateWrapper<UserStats>()
                .eq(UserStats::getUserId, userId)
                .setSql("community_answers = community_answers + 1"));

        return answer.getId();
    }

    @Override
    @Transactional
    public void deleteAnswer(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommunityAnswer answer = answerMapper.selectById(id);
        if (answer == null) throw new BizException("回答不存在");
        if (!answer.getUserId().equals(userId)) throw new BizException("只能删除自己的回答");

        answerMapper.deleteById(id);
        questionMapper.update(null, new LambdaUpdateWrapper<CommunityQuestion>()
                .eq(CommunityQuestion::getId, answer.getQuestionId())
                .setSql("answer_count = answer_count - 1"));
    }

    @Override
    @Transactional
    public void acceptAnswer(Long questionId, Long answerId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommunityQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new BizException("问题不存在");
        if (!question.getUserId().equals(userId)) throw new BizException("只能采纳自己问题下的回答");

        CommunityAnswer answer = answerMapper.selectById(answerId);
        if (answer == null || !answer.getQuestionId().equals(questionId)) {
            throw new BizException("回答不存在或不属于该问题");
        }

        answerMapper.update(null, new LambdaUpdateWrapper<CommunityAnswer>()
                .eq(CommunityAnswer::getQuestionId, questionId)
                .eq(CommunityAnswer::getIsAccepted, true)
                .set(CommunityAnswer::getIsAccepted, false));

        answer.setIsAccepted(true);
        answerMapper.updateById(answer);

        question.setStatus("resolved");
        questionMapper.updateById(question);

        ensureUserStats(answer.getUserId());
        userStatsMapper.update(null, new LambdaUpdateWrapper<UserStats>()
                .eq(UserStats::getUserId, answer.getUserId())
                .setSql("community_accepted = community_accepted + 1, community_score = community_score + 50"));

        // Notify answer author that their answer was accepted
        try {
            notificationService.send(answer.getUserId(), "community",
                    "回答被采纳",
                    "你在问题「" + question.getTitle() + "」下的回答已被采纳，获得 50 社区积分！",
                    "/community/question/" + questionId);
        } catch (Exception e) {
            // swallow — notification is non-critical
        }
    }

    @Override
    @Transactional
    public void vote(CommunityVoteRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String targetType = request.getTargetType();

        if (!"question".equals(targetType) && !"answer".equals(targetType)) {
            throw new BizException("目标类型无效");
        }

        Long existing = voteMapper.selectCount(new LambdaQueryWrapper<CommunityVote>()
                .eq(CommunityVote::getUserId, userId)
                .eq(CommunityVote::getTargetType, targetType)
                .eq(CommunityVote::getTargetId, request.getTargetId()));

        if (existing > 0) {
            throw new BizException("已经点过赞了");
        }

        CommunityVote vote = new CommunityVote();
        vote.setUserId(userId);
        vote.setTargetType(targetType);
        vote.setTargetId(request.getTargetId());
        vote.setValue(1);
        voteMapper.insert(vote);

        if ("question".equals(targetType)) {
            questionMapper.update(null, new LambdaUpdateWrapper<CommunityQuestion>()
                    .eq(CommunityQuestion::getId, request.getTargetId())
                    .setSql("upvote_count = upvote_count + 1"));

            CommunityQuestion q = questionMapper.selectById(request.getTargetId());
            if (q != null) {
                ensureUserStats(q.getUserId());
                userStatsMapper.update(null, new LambdaUpdateWrapper<UserStats>()
                        .eq(UserStats::getUserId, q.getUserId())
                        .setSql("community_score = community_score + 2"));

                // Notify question author (skip self-votes)
                if (!q.getUserId().equals(userId)) {
                    try {
                        notificationService.send(q.getUserId(), "community",
                                "收到点赞",
                                "你的问题「" + q.getTitle() + "」收到了一个赞！",
                                "/community/question/" + q.getId());
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            answerMapper.update(null, new LambdaUpdateWrapper<CommunityAnswer>()
                    .eq(CommunityAnswer::getId, request.getTargetId())
                    .setSql("upvote_count = upvote_count + 1"));

            CommunityAnswer a = answerMapper.selectById(request.getTargetId());
            if (a != null) {
                ensureUserStats(a.getUserId());
                userStatsMapper.update(null, new LambdaUpdateWrapper<UserStats>()
                        .eq(UserStats::getUserId, a.getUserId())
                        .setSql("community_score = community_score + 2"));

                // Notify answer author (skip self-votes)
                if (!a.getUserId().equals(userId)) {
                    try {
                        CommunityQuestion q = questionMapper.selectById(a.getQuestionId());
                        String qTitle = q != null ? q.getTitle() : "未知问题";
                        notificationService.send(a.getUserId(), "community",
                                "收到点赞",
                                "你在问题「" + qTitle + "」下的回答收到了一个赞！",
                                "/community/question/" + a.getQuestionId());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public List<LeaderboardEntryVO> getLeaderboard(int limit) {
        List<UserStats> stats = userStatsMapper.selectList(
                new LambdaQueryWrapper<UserStats>()
                        .orderByDesc(UserStats::getCommunityScore)
                        .last("LIMIT " + limit));

        List<LeaderboardEntryVO> list = new java.util.ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            UserStats s = stats.get(i);
            LeaderboardEntryVO vo = new LeaderboardEntryVO();
            vo.setUserId(s.getUserId());
            vo.setRankTitle(s.getRankTitle());
            vo.setCommunityScore(s.getCommunityScore());
            vo.setCommunityQuestions(s.getCommunityQuestions());
            vo.setCommunityAnswers(s.getCommunityAnswers());
            vo.setCommunityAccepted(s.getCommunityAccepted());
            vo.setPosition(i + 1);
            list.add(vo);
        }
        return list;
    }

    private CommunityQuestionVO toQuestionVO(CommunityQuestion q) {
        CommunityQuestionVO vo = new CommunityQuestionVO();
        vo.setId(q.getId());
        vo.setUserId(q.getUserId());
        vo.setTitle(q.getTitle());
        vo.setContent(q.getContent());
        vo.setCategoryId(q.getCategoryId());
        vo.setStatus(q.getStatus());
        vo.setUpvoteCount(q.getUpvoteCount());
        vo.setAnswerCount(q.getAnswerCount());
        vo.setCreatedAt(q.getCreatedAt());
        return vo;
    }

    private CommunityAnswerVO toAnswerVO(CommunityAnswer a) {
        CommunityAnswerVO vo = new CommunityAnswerVO();
        vo.setId(a.getId());
        vo.setQuestionId(a.getQuestionId());
        vo.setUserId(a.getUserId());
        vo.setContent(a.getContent());
        vo.setIsAccepted(a.getIsAccepted());
        vo.setUpvoteCount(a.getUpvoteCount());
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
    }

    private boolean hasVoted(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        return voteMapper.selectCount(new LambdaQueryWrapper<CommunityVote>()
                .eq(CommunityVote::getUserId, userId)
                .eq(CommunityVote::getTargetType, targetType)
                .eq(CommunityVote::getTargetId, targetId)) > 0;
    }

    private void ensureUserStats(Long userId) {
        UserStats existing = userStatsMapper.selectOne(
                new LambdaQueryWrapper<UserStats>()
                        .eq(UserStats::getUserId, userId));
        if (existing == null) {
            UserStats stats = new UserStats();
            stats.setUserId(userId);
            stats.setTotalScore(java.math.BigDecimal.ZERO);
            stats.setInterviewCount(0);
            stats.setAvgScore(java.math.BigDecimal.ZERO);
            stats.setReviewStreak(0);
            stats.setTotalReviews(0);
            stats.setCommunityScore(0);
            stats.setCommunityQuestions(0);
            stats.setCommunityAnswers(0);
            stats.setCommunityAccepted(0);
            stats.setRankTitle("见习生");
            userStatsMapper.insert(stats);
        }
    }
}
