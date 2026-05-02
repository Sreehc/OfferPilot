package com.bytecoach.wrong.service;

import com.bytecoach.common.dto.PageResult;
import com.bytecoach.wrong.dto.WrongMasteryUpdateRequest;
import com.bytecoach.wrong.vo.WrongQuestionVO;
import java.util.List;

public interface WrongService {

    PageResult<WrongQuestionVO> list(Long userId, int pageNum, int pageSize);

    List<WrongQuestionVO> listAll(Long userId);

    WrongQuestionVO detail(Long userId, Long id);

    void updateMastery(Long userId, Long id, WrongMasteryUpdateRequest request);

    void delete(Long userId, Long id);
}
