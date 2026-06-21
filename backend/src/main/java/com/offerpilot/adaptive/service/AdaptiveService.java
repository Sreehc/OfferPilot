package com.offerpilot.adaptive.service;

import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.AdaptiveRecommendationVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.adaptive.vo.RecommendInterviewVO;
import com.offerpilot.adaptive.vo.RecommendQuestionsVO;
import java.util.List;

public interface AdaptiveService {
    AbilityProfileVO getAbilityProfile(Long userId);
    List<CategoryAbilityVO> getCategoryAbilities(Long userId);
    List<String> getWeakCategories(Long userId);
    String getRecommendedDifficulty(Long userId);
    RecommendInterviewVO getRecommendInterview(Long userId);
    List<RecommendQuestionsVO> getRecommendQuestions(Long userId, int limit);
    List<AdaptiveRecommendationVO> getRecommendations(Long userId, int limit);
    void refreshAbilityProfile(Long userId);
}
