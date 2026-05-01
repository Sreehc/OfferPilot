package com.bytecoach.wrong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.dashboard.service.DashboardService;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.dto.WrongMasteryUpdateRequest;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.bytecoach.wrong.service.WrongService;
import com.bytecoach.wrong.vo.WrongQuestionVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WrongServiceImpl implements WrongService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final DashboardService dashboardService;

    @Override
    public PageResult<WrongQuestionVO> list(Long userId, int pageNum, int pageSize) {
        long total = wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));

        int offset = (Math.max(pageNum, 1) - 1) * Math.max(pageSize, 1);
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .orderByDesc(WrongQuestion::getUpdateTime)
                        .last("LIMIT " + Math.max(pageSize, 1) + " OFFSET " + offset));

        List<WrongQuestionVO> voList;
        if (wrongs.isEmpty()) {
            voList = List.of();
        } else {
            Set<Long> questionIds = wrongs.stream()
                    .map(WrongQuestion::getQuestionId)
                    .collect(Collectors.toSet());
            Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                    .stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
            voList = wrongs.stream()
                    .map(wrong -> {
                        Question q = questionMap.get(wrong.getQuestionId());
                        return WrongQuestionVO.builder()
                                .id(wrong.getId())
                                .questionId(wrong.getQuestionId())
                                .title(q != null ? q.getTitle() : "Unknown")
                                .masteryLevel(wrong.getMasteryLevel())
                                .standardAnswer(wrong.getStandardAnswer())
                                .errorReason(wrong.getErrorReason())
                                .build();
                    })
                    .toList();
        }

        return PageResult.<WrongQuestionVO>builder()
                .records(voList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) total / Math.max(pageSize, 1)))
                .build();
    }

    @Override
    public WrongQuestionVO detail(Long userId, Long id) {
        WrongQuestion wrong = getOwnedWrong(userId, id);
        Question q = questionMapper.selectById(wrong.getQuestionId());
        return WrongQuestionVO.builder()
                .id(wrong.getId())
                .questionId(wrong.getQuestionId())
                .title(q != null ? q.getTitle() : "Unknown")
                .masteryLevel(wrong.getMasteryLevel())
                .standardAnswer(wrong.getStandardAnswer())
                .errorReason(wrong.getErrorReason())
                .build();
    }

    @Override
    public void updateMastery(Long userId, Long id, WrongMasteryUpdateRequest request) {
        WrongQuestion wrong = getOwnedWrong(userId, id);
        wrong.setMasteryLevel(request.getMasteryLevel());
        wrong.setReviewCount(wrong.getReviewCount() + 1);
        wrong.setLastReviewTime(LocalDateTime.now());
        wrongQuestionMapper.updateById(wrong);
    }

    @Override
    public void delete(Long userId, Long id) {
        WrongQuestion wrong = getOwnedWrong(userId, id);
        wrongQuestionMapper.deleteById(wrong.getId());
        dashboardService.evictCache(userId);
    }

    private WrongQuestion getOwnedWrong(Long userId, Long id) {
        WrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null || !wrong.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "wrong question not found");
        }
        return wrong;
    }
}
