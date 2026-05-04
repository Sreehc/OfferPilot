package com.bytecoach.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.community.entity.UserStats;
import com.bytecoach.community.mapper.UserStatsMapper;
import com.bytecoach.community.service.UserStatsService;
import com.bytecoach.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final UserStatsMapper userStatsMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void refreshAllRanks() {
        List<UserStats> all = userStatsMapper.selectList(null);
        int updated = 0;
        for (UserStats stats : all) {
            String newRank = calculateRankTitle(stats.getCommunityScore() != null ? stats.getCommunityScore() : 0);
            if (!newRank.equals(stats.getRankTitle())) {
                String oldRank = stats.getRankTitle();
                stats.setRankTitle(newRank);
                userStatsMapper.updateById(stats);
                updated++;

                // Notify user about rank upgrade
                try {
                    notificationService.send(stats.getUserId(), "rank",
                            "等级提升",
                            "恭喜！你的社区等级从「" + oldRank + "」提升为「" + newRank + "」，继续加油！",
                            "/community/leaderboard");
                } catch (Exception ignored) {
                }
            }
        }
        log.info("Rank refresh completed: {} users updated out of {}", updated, all.size());
    }

    @Override
    @Transactional
    public void refreshUserStats(Long userId) {
        UserStats stats = getUserStats(userId);
        if (stats == null) return;

        String newRank = calculateRankTitle(stats.getCommunityScore() != null ? stats.getCommunityScore() : 0);
        if (!newRank.equals(stats.getRankTitle())) {
            String oldRank = stats.getRankTitle();
            stats.setRankTitle(newRank);
            userStatsMapper.updateById(stats);

            try {
                notificationService.send(userId, "rank",
                        "等级提升",
                        "恭喜！你的社区等级从「" + oldRank + "」提升为「" + newRank + "」，继续加油！",
                        "/community/leaderboard");
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public UserStats getUserStats(Long userId) {
        return userStatsMapper.selectOne(
                new LambdaQueryWrapper<UserStats>()
                        .eq(UserStats::getUserId, userId));
    }

    @Override
    public String calculateRankTitle(int communityScore) {
        if (communityScore >= 5000) return "技术专家";
        if (communityScore >= 2000) return "架构师";
        if (communityScore >= 800) return "高级工程师";
        if (communityScore >= 300) return "中级工程师";
        if (communityScore >= 100) return "初级工程师";
        return "见习生";
    }
}
