package com.bytecoach.data.service;

import com.bytecoach.data.dto.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface DataImportService {
    ImportResultVO importQuestions(MultipartFile file);
}
