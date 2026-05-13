package com.offerpilot.knowledge.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeSearchVO {
    private String query;
    private List<Reference> references;

    @Data
    @Builder
    public static class Reference {
        private Long docId;
        private Long chunkId;
        private String docTitle;
        private String snippet;
        private Float score;
        private String libraryScope;
        private String businessType;
        private String fileType;
    }
}
