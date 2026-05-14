package com.offerpilot.chat.vo;

import com.offerpilot.common.vo.ContextSourceVO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSendVO {
    private Long sessionId;
    private String sessionTitle;
    private String answer;
    private String answerMode;
    private String knowledgeScope;
    private String contextType;
    private ContextSourceVO contextSource;
    private List<ChatMessageReferenceVO> references;
    private List<String> suggestedQuestions;
}
