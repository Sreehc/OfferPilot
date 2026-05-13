package com.offerpilot.community.service;

import com.offerpilot.community.entity.UserStats;

public interface UserStatsService {
    void refreshAllRanks();
    void refreshUserStats(Long userId);
    UserStats getUserStats(Long userId);
    String calculateRankTitle(int communityScore);
}
