package com.bytecoach.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.admin.dto.AdminUserDetailVO;
import com.bytecoach.admin.dto.AdminUserUpdateRequest;
import com.bytecoach.admin.service.AdminUserService;
import com.bytecoach.auth.service.DeviceService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.community.entity.CommunityAnswer;
import com.bytecoach.community.entity.CommunityQuestion;
import com.bytecoach.community.entity.UserStats;
import com.bytecoach.community.mapper.CommunityAnswerMapper;
import com.bytecoach.community.mapper.CommunityQuestionMapper;
import com.bytecoach.community.mapper.UserStatsMapper;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserService userService;
    private final DeviceService deviceService;
    private final InterviewSessionMapper interviewSessionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final CommunityQuestionMapper communityQuestionMapper;
    private final CommunityAnswerMapper communityAnswerMapper;
    private final UserStatsMapper userStatsMapper;

    @Override
    public PageResult<UserInfoVO> listUsers(String keyword, String role, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = userService.page(new Page<>(pageNum, pageSize), wrapper);

        List<UserInfoVO> records = page.getRecords().stream()
                .map(u -> UserInfoVO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .avatar(u.getAvatar())
                        .role(u.getRole())
                        .status(u.getStatus())
                        .createTime(u.getCreateTime())
                        .lastLoginTime(u.getLastLoginTime())
                        .build())
                .toList();

        return PageResult.<UserInfoVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();
    }

    @Override
    @Transactional
    public void updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userService.updateById(user);
    }

    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setStatus(0);
        userService.updateById(user);
        // Revoke all devices
        deviceService.revokeAllOtherDevices(userId, null);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setStatus(1);
        userService.updateById(user);
    }

    @Override
    public AdminUserDetailVO getUserDetail(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        long interviewCount = interviewSessionMapper.selectCount(
                new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getUserId, userId));
        long wrongCount = wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        long questionCount = communityQuestionMapper.selectCount(
                new LambdaQueryWrapper<CommunityQuestion>().eq(CommunityQuestion::getUserId, userId));
        long answerCount = communityAnswerMapper.selectCount(
                new LambdaQueryWrapper<CommunityAnswer>().eq(CommunityAnswer::getUserId, userId));

        UserStats stats = userStatsMapper.selectOne(
                new LambdaQueryWrapper<UserStats>().eq(UserStats::getUserId, userId));

        return AdminUserDetailVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .interviewCount((int) interviewCount)
                .wrongCount((int) wrongCount)
                .reviewCount(stats != null ? stats.getTotalReviews() : 0)
                .communityQuestions((int) questionCount)
                .communityAnswers((int) answerCount)
                .build();
    }
}
