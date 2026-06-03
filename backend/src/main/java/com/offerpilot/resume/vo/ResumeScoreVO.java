package com.offerpilot.resume.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeScoreVO {
    private int overallScore;
    private int completenessScore;
    private int keywordCoverage;
    private int atsCompatibility;
    private List<ResumeSuggestionVO> suggestions;
}
