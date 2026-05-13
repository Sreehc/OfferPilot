package com.offerpilot.data.service;

import jakarta.servlet.http.HttpServletResponse;

public interface DataExportService {
    void exportQuestions(HttpServletResponse response);
    void exportUsers(HttpServletResponse response);
    void exportMyData(Long userId, HttpServletResponse response);
}
