package com.offerpilot.common.storage;

import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UploadPolicyService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> KNOWLEDGE_EXTENSIONS = Set.of("md", "markdown", "txt", "text", "pdf");
    private static final Set<String> RESUME_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("webm", "wav", "mp3", "m4a", "ogg", "mpeg");

    private final OfferPilotProperties offerPilotProperties;

    public void validate(StorageDirectory directory, String originalFilename, String contentType, long size) {
        String extension = getExtension(originalFilename);
        if (!StringUtils.hasText(originalFilename) || !StringUtils.hasText(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }

        switch (directory) {
            case AVATAR -> validateAvatar(extension, contentType, size);
            case KNOWLEDGE -> validateKnowledge(extension, size);
            case RESUME -> validateResume(extension, size);
            case INTERVIEW_AUDIO -> validateInterviewAudio(extension, contentType, size);
            default -> {
                // Reserved directories keep default platform multipart limits for now.
            }
        }
    }

    private void validateAvatar(String extension, String contentType, long size) {
        if (!IMAGE_EXTENSIONS.contains(extension) || contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "头像仅支持 jpg、png、webp、gif 图片");
        }
        if (size > offerPilotProperties.getStorage().getAvatarMaxBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "头像图片大小不能超过 2MB");
        }
    }

    private void validateKnowledge(String extension, long size) {
        if (!KNOWLEDGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "知识文档仅支持 md、txt、pdf");
        }
        if (size > offerPilotProperties.getStorage().getKnowledgeMaxBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "知识文档大小不能超过 20MB");
        }
    }

    private void validateResume(String extension, long size) {
        if (!RESUME_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "简历文件仅支持 pdf、doc、docx");
        }
        if (size > offerPilotProperties.getStorage().getResumeMaxBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "简历文件大小不能超过 10MB");
        }
    }

    private void validateInterviewAudio(String extension, String contentType, long size) {
        if (!AUDIO_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "语音文件仅支持 webm、wav、mp3、m4a、ogg");
        }
        if (contentType != null && !contentType.startsWith("audio/") && !"application/octet-stream".equals(contentType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "语音文件类型不合法");
        }
        if (size > offerPilotProperties.getStorage().getInterviewAudioMaxBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "语音文件大小不能超过 15MB");
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
