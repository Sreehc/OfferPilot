package com.offerpilot.favorite.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteStatsVO {
    private long total;
    private long knowledgeCount;
    private long questionCount;
    private long communityCount;
    private long todayCount;
}
