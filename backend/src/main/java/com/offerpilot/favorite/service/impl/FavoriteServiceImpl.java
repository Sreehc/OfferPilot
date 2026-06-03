package com.offerpilot.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.community.entity.CommunityQuestion;
import com.offerpilot.community.mapper.CommunityQuestionMapper;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.favorite.dto.FavoriteTagUpsertRequest;
import com.offerpilot.favorite.dto.FavoriteUpsertRequest;
import com.offerpilot.favorite.entity.Favorite;
import com.offerpilot.favorite.entity.FavoriteTag;
import com.offerpilot.favorite.mapper.FavoriteMapper;
import com.offerpilot.favorite.mapper.FavoriteTagMapper;
import com.offerpilot.favorite.service.FavoriteService;
import com.offerpilot.favorite.vo.FavoriteStatsVO;
import com.offerpilot.favorite.vo.FavoriteTagVO;
import com.offerpilot.favorite.vo.FavoriteVO;
import com.offerpilot.knowledge.entity.KnowledgeDoc;
import com.offerpilot.knowledge.mapper.KnowledgeDocMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final FavoriteTagMapper favoriteTagMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final QuestionMapper questionMapper;
    private final CommunityQuestionMapper communityQuestionMapper;
    private final DashboardService dashboardService;

    @Override
    public PageResult<FavoriteVO> list(Long userId, String targetType, Long tagId, String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId);
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(Favorite::getTargetType, targetType);
        }
        if (tagId != null) {
            wrapper.eq(Favorite::getTagId, tagId);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);

        long total = favoriteMapper.selectCount(wrapper);

        int offset = (Math.max(pageNum, 1) - 1) * Math.max(pageSize, 1);
        List<Favorite> favorites = favoriteMapper.selectList(
                wrapper.last("LIMIT " + Math.max(pageSize, 1) + " OFFSET " + offset));

        List<FavoriteVO> voList;
        if (favorites.isEmpty()) {
            voList = List.of();
        } else {
            // Collect tag ids for batch lookup
            Set<Long> tagIds = favorites.stream()
                    .map(Favorite::getTagId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, FavoriteTag> tagMap = tagIds.isEmpty()
                    ? Collections.emptyMap()
                    : favoriteTagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(FavoriteTag::getId, Function.identity(), (a, b) -> a));

            voList = favorites.stream().map(fav -> {
                String title = resolveTitle(fav.getTargetType(), fav.getTargetId());
                String summary = resolveSummary(fav.getTargetType(), fav.getTargetId());
                String categoryName = resolveCategoryName(fav.getTargetType(), fav.getTargetId());
                FavoriteTag tag = fav.getTagId() != null ? tagMap.get(fav.getTagId()) : null;

                return FavoriteVO.builder()
                        .id(fav.getId())
                        .targetType(fav.getTargetType())
                        .targetId(fav.getTargetId())
                        .title(title)
                        .summary(summary)
                        .categoryName(categoryName)
                        .tagId(fav.getTagId())
                        .tagName(tag != null ? tag.getName() : null)
                        .createTime(fav.getCreateTime())
                        .build();
            }).toList();
        }

        return PageResult.<FavoriteVO>builder()
                .records(voList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) total / Math.max(pageSize, 1)))
                .build();
    }

    @Override
    public FavoriteStatsVO stats(Long userId) {
        long total = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
        long knowledgeCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId).eq(Favorite::getTargetType, "knowledge"));
        long questionCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId).eq(Favorite::getTargetType, "question"));
        long communityCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId).eq(Favorite::getTargetType, "community"));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .ge(Favorite::getCreateTime, todayStart));

        return FavoriteStatsVO.builder()
                .total(total)
                .knowledgeCount(knowledgeCount)
                .questionCount(questionCount)
                .communityCount(communityCount)
                .todayCount(todayCount)
                .build();
    }

    @Override
    @Transactional
    public FavoriteVO add(Long userId, FavoriteUpsertRequest request) {
        Long existing = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTargetType, request.getTargetType())
                        .eq(Favorite::getTargetId, request.getTargetId()));
        if (existing > 0) {
            Favorite fav = favoriteMapper.selectOne(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, userId)
                            .eq(Favorite::getTargetType, request.getTargetType())
                            .eq(Favorite::getTargetId, request.getTargetId()));
            if (fav == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "favorite not found");
            }
            return FavoriteVO.builder()
                    .id(fav.getId())
                    .targetType(fav.getTargetType())
                    .targetId(fav.getTargetId())
                    .tagId(fav.getTagId())
                    .createTime(fav.getCreateTime())
                    .build();
        }

        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setTargetType(request.getTargetType());
        fav.setTargetId(request.getTargetId());
        fav.setTagId(request.getTagId());
        favoriteMapper.insert(fav);

        dashboardService.evictCache(userId);

        return FavoriteVO.builder()
                .id(fav.getId())
                .targetType(fav.getTargetType())
                .targetId(fav.getTargetId())
                .title(resolveTitle(fav.getTargetType(), fav.getTargetId()))
                .summary(resolveSummary(fav.getTargetType(), fav.getTargetId()))
                .categoryName(resolveCategoryName(fav.getTargetType(), fav.getTargetId()))
                .tagId(fav.getTagId())
                .createTime(fav.getCreateTime())
                .build();
    }

    @Override
    @Transactional
    public void remove(Long userId, Long favoriteId) {
        Favorite fav = getOwnedFavorite(userId, favoriteId);
        favoriteMapper.deleteById(fav.getId());
        dashboardService.evictCache(userId);
    }

    @Override
    @Transactional
    public void batchRemove(Long userId, List<Long> ids) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .in(Favorite::getId, ids));
        dashboardService.evictCache(userId);
    }

    @Override
    public boolean isFavorited(Long userId, String targetType, Long targetId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTargetType, targetType)
                        .eq(Favorite::getTargetId, targetId)) > 0;
    }

    @Override
    public List<FavoriteTagVO> listTags(Long userId) {
        List<FavoriteTag> tags = favoriteTagMapper.selectList(
                new LambdaQueryWrapper<FavoriteTag>()
                        .eq(FavoriteTag::getUserId, userId)
                        .orderByAsc(FavoriteTag::getSortOrder));

        return tags.stream().map(tag -> {
            long count = favoriteMapper.selectCount(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, userId)
                            .eq(Favorite::getTagId, tag.getId()));
            return FavoriteTagVO.builder()
                    .id(tag.getId())
                    .name(tag.getName())
                    .count((int) count)
                    .sortOrder(tag.getSortOrder())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public FavoriteTagVO createTag(Long userId, FavoriteTagUpsertRequest request) {
        FavoriteTag tag = new FavoriteTag();
        tag.setUserId(userId);
        tag.setName(request.getName());
        tag.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        favoriteTagMapper.insert(tag);

        return FavoriteTagVO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .count(0)
                .sortOrder(tag.getSortOrder())
                .build();
    }

    @Override
    @Transactional
    public void deleteTag(Long userId, Long tagId) {
        FavoriteTag tag = favoriteTagMapper.selectById(tagId);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "tag not found");
        }
        // Clear tagId from associated favorites
        favoriteMapper.update(null,
                new LambdaUpdateWrapper<Favorite>()
                        .eq(Favorite::getTagId, tagId)
                        .set(Favorite::getTagId, null));
        favoriteTagMapper.deleteById(tagId);
    }

    @Override
    @Transactional
    public void updateFavoriteTag(Long userId, Long favoriteId, Long tagId) {
        Favorite fav = getOwnedFavorite(userId, favoriteId);
        if (tagId != null) {
            FavoriteTag tag = favoriteTagMapper.selectById(tagId);
            if (tag == null || !tag.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "tag not found");
            }
        }
        fav.setTagId(tagId);
        favoriteMapper.updateById(fav);
    }

    private Favorite getOwnedFavorite(Long userId, Long id) {
        Favorite fav = favoriteMapper.selectById(id);
        if (fav == null || !fav.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "favorite not found");
        }
        return fav;
    }

    private String resolveTitle(String targetType, Long targetId) {
        return switch (targetType) {
            case "knowledge" -> {
                KnowledgeDoc doc = knowledgeDocMapper.selectById(targetId);
                yield doc != null ? doc.getTitle() : "已删除的资料";
            }
            case "question" -> {
                Question q = questionMapper.selectById(targetId);
                yield q != null ? q.getTitle() : "已删除的题目";
            }
            case "community" -> {
                CommunityQuestion cq = communityQuestionMapper.selectById(targetId);
                yield cq != null ? cq.getTitle() : "已删除的问题";
            }
            default -> "未知内容";
        };
    }

    private String resolveSummary(String targetType, Long targetId) {
        return switch (targetType) {
            case "knowledge" -> {
                KnowledgeDoc doc = knowledgeDocMapper.selectById(targetId);
                yield doc != null ? doc.getSummary() : null;
            }
            case "question" -> {
                Question q = questionMapper.selectById(targetId);
                yield q != null ? q.getStandardAnswer() : null;
            }
            case "community" -> {
                CommunityQuestion cq = communityQuestionMapper.selectById(targetId);
                yield cq != null ? cq.getContent() : null;
            }
            default -> null;
        };
    }

    private String resolveCategoryName(String targetType, Long targetId) {
        // Category resolution is not critical; return null for simplicity
        // Can be enhanced later with batch category lookup
        return null;
    }
}
