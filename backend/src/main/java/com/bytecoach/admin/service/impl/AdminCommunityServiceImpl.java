package com.bytecoach.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.admin.dto.AdminContentReviewVO;
import com.bytecoach.admin.service.AdminCommunityService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.community.entity.CommunityAnswer;
import com.bytecoach.community.entity.CommunityQuestion;
import com.bytecoach.community.mapper.CommunityAnswerMapper;
import com.bytecoach.community.mapper.CommunityQuestionMapper;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCommunityServiceImpl implements AdminCommunityService {

    private final CommunityQuestionMapper questionMapper;
    private final CommunityAnswerMapper answerMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    @Override
    public PageResult<AdminContentReviewVO> listPending(int pageNum, int pageSize) {
        // Get pending questions
        Page<CommunityQuestion> qPage = questionMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CommunityQuestion>()
                        .eq(CommunityQuestion::getStatus, "pending")
                        .orderByDesc(CommunityQuestion::getCreateTime));

        List<AdminContentReviewVO> records = qPage.getRecords().stream()
                .map(q -> {
                    String username = resolveUsername(q.getUserId());
                    return AdminContentReviewVO.builder()
                            .id(q.getId())
                            .type("question")
                            .userId(q.getUserId())
                            .username(username)
                            .title(q.getTitle())
                            .content(truncate(q.getContent(), 200))
                            .createTime(q.getCreateTime())
                            .build();
                })
                .toList();

        return PageResult.<AdminContentReviewVO>builder()
                .records(records)
                .total(qPage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) qPage.getTotal() / pageSize))
                .build();
    }

    @Override
    @Transactional
    public void approve(Long id) {
        CommunityQuestion question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "内容不存在");
        }
        question.setStatus("approved");
        questionMapper.updateById(question);

        // Notify the author
        notificationService.send(question.getUserId(), "content_approved",
                "内容审核通过",
                "您发布的问题「" + question.getTitle() + "」已通过审核。",
                "/community/question/" + id);
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        CommunityQuestion question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "内容不存在");
        }
        question.setStatus("rejected");
        questionMapper.updateById(question);

        String content = "您发布的问题「" + question.getTitle() + "」未通过审核。";
        if (reason != null && !reason.isBlank()) {
            content += "原因：" + reason;
        }
        notificationService.send(question.getUserId(), "content_rejected",
                "内容审核未通过", content, null);
    }

    private String resolveUsername(Long userId) {
        try {
            User user = userService.getById(userId);
            return user != null ? user.getUsername() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
