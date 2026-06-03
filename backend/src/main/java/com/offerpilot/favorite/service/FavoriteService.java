package com.offerpilot.favorite.service;

import com.offerpilot.common.dto.PageResult;
import com.offerpilot.favorite.dto.FavoriteTagUpsertRequest;
import com.offerpilot.favorite.dto.FavoriteUpsertRequest;
import com.offerpilot.favorite.vo.FavoriteStatsVO;
import com.offerpilot.favorite.vo.FavoriteTagVO;
import com.offerpilot.favorite.vo.FavoriteVO;
import java.util.List;

public interface FavoriteService {
    PageResult<FavoriteVO> list(Long userId, String targetType, Long tagId, String keyword, int pageNum, int pageSize);
    FavoriteStatsVO stats(Long userId);
    FavoriteVO add(Long userId, FavoriteUpsertRequest request);
    void remove(Long userId, Long favoriteId);
    void batchRemove(Long userId, List<Long> ids);
    boolean isFavorited(Long userId, String targetType, Long targetId);
    List<FavoriteTagVO> listTags(Long userId);
    FavoriteTagVO createTag(Long userId, FavoriteTagUpsertRequest request);
    void deleteTag(Long userId, Long tagId);
    void updateFavoriteTag(Long userId, Long favoriteId, Long tagId);
}
