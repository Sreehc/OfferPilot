package com.offerpilot.resume.service;

import com.offerpilot.resume.dto.ResumeUpdateRequest;
import com.offerpilot.resume.vo.ResumeFileVO;
import com.offerpilot.resume.vo.ResumeInterviewResumeVO;
import com.offerpilot.resume.vo.ResumeProjectVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    ResumeFileVO upload(Long userId, MultipartFile file);

    List<ResumeFileVO> list(Long userId);

    ResumeFileVO latest(Long userId);

    ResumeFileVO detail(Long userId, Long resumeId);

    List<ResumeProjectVO> projectQuestions(Long userId, Long resumeId);

    String selfIntro(Long userId, Long resumeId);

    ResumeInterviewResumeVO interviewResume(Long userId, Long resumeId);

    ResumeFileVO retryParse(Long userId, Long resumeId);

    ResumeFileVO update(Long userId, Long resumeId, ResumeUpdateRequest request);
}
