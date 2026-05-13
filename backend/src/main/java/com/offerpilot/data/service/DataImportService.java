package com.offerpilot.data.service;

import com.offerpilot.data.dto.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface DataImportService {
    ImportResultVO importQuestions(MultipartFile file);
}
