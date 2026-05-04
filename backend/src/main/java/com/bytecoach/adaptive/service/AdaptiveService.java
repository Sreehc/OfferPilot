package com.bytecoach.adaptive.service;

import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.adaptive.vo.CategoryAbilityVO;
import com.bytecoach.adaptive.vo.RecommendInterviewVO;
import com.bytecoach.adaptive.vo.RecommendQuestionsVO;
import java.util.List;

public interface AdaptiveService {
    AbilityProfileVO getAbilityProfile(Long userId);
    List<CategoryAbilityVO> getCategoryAbilities(Long userId);
    List<String> getWeakCategories(Long userId);
    String getRecommendedDifficulty(Long userId);
    RecommendInterviewVO getRecommendInterview(Long userId);
    List<RecommendQuestionsVO> getRecommendQuestions(Long userId, int limit);
    void refreshAbilityProfile(Long userId);
}
