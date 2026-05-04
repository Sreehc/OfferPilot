package com.bytecoach.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.admin.dto.AdminOverviewVO;
import com.bytecoach.admin.dto.AdminTrendVO;
import com.bytecoach.admin.service.AdminOverviewService;
import com.bytecoach.auth.entity.LoginLog;
import com.bytecoach.auth.mapper.LoginLogMapper;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.mapper.UserMapper;
import com.bytecoach.wrong.entity.ReviewLog;
import com.bytecoach.wrong.mapper.ReviewLogMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOverviewServiceImpl implements AdminOverviewService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final ReviewLogMapper reviewLogMapper;

    @Override
    public AdminOverviewVO getOverview() {
        long totalUsers = userMapper.selectCount(null);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long todayNew = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .between(User::getCreateTime, todayStart, todayEnd));

        // Today active: unique users with successful login today
        long todayActive = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getStatus, 1)
                .between(LoginLog::getCreateTime, todayStart, todayEnd));

        long totalInterviews = interviewSessionMapper.selectCount(null);
        long totalReviews = reviewLogMapper.selectCount(null);

        return AdminOverviewVO.builder()
                .totalUsers(totalUsers)
                .todayActive(todayActive)
                .todayNew(todayNew)
                .totalInterviews(totalInterviews)
                .totalReviews(totalReviews)
                .build();
    }

    @Override
    public List<AdminTrendVO> getTrend() {
        List<AdminTrendVO> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            long newUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .between(User::getCreateTime, dayStart, dayEnd));

            long activeUsers = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                    .eq(LoginLog::getStatus, 1)
                    .between(LoginLog::getCreateTime, dayStart, dayEnd));

            trend.add(AdminTrendVO.builder()
                    .date(date.format(fmt))
                    .newUsers(newUsers)
                    .activeUsers(activeUsers)
                    .build());
        }
        return trend;
    }
}
