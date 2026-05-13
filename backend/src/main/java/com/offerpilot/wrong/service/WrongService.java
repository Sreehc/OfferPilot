package com.offerpilot.wrong.service;

import com.offerpilot.common.dto.PageResult;
import com.offerpilot.wrong.dto.WrongMasteryUpdateRequest;
import com.offerpilot.wrong.vo.WrongQuestionVO;
import java.util.List;

public interface WrongService {

    PageResult<WrongQuestionVO> list(Long userId, int pageNum, int pageSize);

    List<WrongQuestionVO> listAll(Long userId);

    WrongQuestionVO detail(Long userId, Long id);

    void updateMastery(Long userId, Long id, WrongMasteryUpdateRequest request);

    void delete(Long userId, Long id);
}
