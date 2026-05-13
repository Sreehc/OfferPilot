package com.offerpilot.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration for OfferPilot business parameters.
 * All values can be overridden via environment variables with the OFFERPILOT_ prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "offerpilot")
public class OfferPilotProperties {

    private Review review = new Review();
    private Notification notification = new Notification();
    private Community community = new Community();
    private Adaptive adaptive = new Adaptive();
    private Interview interview = new Interview();
    private Dashboard dashboard = new Dashboard();
    private RateLimit rateLimit = new RateLimit();
    private AiQuota aiQuota = new AiQuota();
    private Document document = new Document();
    private LoginSecurity loginSecurity = new LoginSecurity();
    private Auth auth = new Auth();
    private Storage storage = new Storage();

    @Data
    public static class Review {
        /** Fixed rate in ms for initializing SM-2 fields on new wrong questions. */
        private long initFixedRateMs = 600_000;
        /** Max batch size per SM-2 initialization run. */
        private int initBatchLimit = 500;
        /** TTL in hours for cached daily review counts. */
        private int cacheTtlHours = 24;
        /** Max review items shown per day. */
        private int dailyLimit = 30;
        /** Cron for caching daily review counts. */
        private String cronDailyCache = "0 5 0 * * ?";
    }

    @Data
    public static class Notification {
        /** Cron for daily review reminder notifications. */
        private String cronDailyReminder = "0 0 8 * * ?";
    }

    @Data
    public static class Community {
        /** Cron for daily rank recalculation. */
        private String cronRankRefresh = "0 5 0 * * ?";
    }

    @Data
    public static class Adaptive {
        /** TTL in hours for cached ability profiles. */
        private int cacheTtlHours = 24;
        /** Ability score below this threshold marks a category as weak. */
        private double weakThreshold = 50.0;
        /** Cron for daily adaptive profile refresh. */
        private String cronRefresh = "0 10 0 * * ?";
    }

    @Data
    public static class Interview {
        /** Score below this threshold flags a question as wrong. */
        private int wrongThreshold = 60;
    }

    @Data
    public static class Dashboard {
        /** Cache TTL in minutes for dashboard overview. */
        private long cacheTtlMinutes = 5;
    }

    @Data
    public static class RateLimit {
        /** Max requests per window per IP. */
        private int maxRequests = 60;
        /** Sliding window duration in seconds. */
        private int windowSeconds = 60;
    }

    @Data
    public static class AiQuota {
        /** Max LLM calls per user per day. */
        private int dailyLimit = 100;
    }

    @Data
    public static class Document {
        /** Max characters extracted from PDF by Tika. */
        private int tikaMaxChars = 1_000_000;
        /** Target chunk size for document splitting. */
        private int maxChunkSize = 800;
    }

    @Data
    public static class LoginSecurity {
        /** Max consecutive login failures before account lock. */
        private int failLimit = 5;
        /** Time window in minutes for counting consecutive failures. */
        private int failWindowMinutes = 5;
        /** Account lock duration in minutes. */
        private int lockMinutes = 30;
    }

    @Data
    public static class Auth {
        /** TTL in minutes for email verification codes. */
        private int emailVerificationCodeTtlMinutes = 10;
        /** TTL in minutes for password reset codes. */
        private int passwordResetCodeTtlMinutes = 15;
        /** Whether to expose debug codes in API responses. */
        private boolean exposeDebugCodes = false;
        /** Reserved GitHub OAuth flag. */
        private boolean githubEnabled = false;
        private String githubClientId;
        private String githubClientSecret;
    }

    @Data
    public static class Storage {
        /** Current storage provider: local / minio (reserved). */
        private String provider = "local";
        /** Root directory used by the local storage adapter. */
        private String localRoot = "./data/storage";
        /** Public base path for files that can be served directly, such as avatars. */
        private String publicBaseUrl = "/api/files/public";
        /** Reserved MinIO endpoint and bucket config. */
        private String minioEndpoint;
        private String minioAccessKey;
        private String minioSecretKey;
        private String minioBucket = "offerpilot";
        /** Upload safety limits by asset type. */
        private long avatarMaxBytes = 2L * 1024 * 1024;
        private long knowledgeMaxBytes = 20L * 1024 * 1024;
        private long interviewAudioMaxBytes = 15L * 1024 * 1024;
    }
}
