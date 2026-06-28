package com.offerpilot.knowledge.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.offerpilot.common.api.Result;
import com.offerpilot.knowledge.service.KnowledgeService;
import com.offerpilot.knowledge.vo.KnowledgeDocVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminKnowledgeControllerTest {

    @Mock
    private KnowledgeService knowledgeService;

    @InjectMocks
    private AdminKnowledgeController controller;

    @Test
    void reindexAll_delegatesToKnowledgeService() {
        List<KnowledgeDocVO> docs = List.of(KnowledgeDocVO.builder().id(1L).title("doc-1").build());
        when(knowledgeService.reindexAll()).thenReturn(docs);

        Result<List<KnowledgeDocVO>> result = controller.reindexAll();

        assertEquals(docs, result.getData());
        verify(knowledgeService).reindexAll();
    }
}
