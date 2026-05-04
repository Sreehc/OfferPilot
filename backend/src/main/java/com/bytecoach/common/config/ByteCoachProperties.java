package com.bytecoach.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration for ByteCoach business parameters.
 * All values can be overridden via environment variables with the BYTECOACH_ prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bytecoach")
public class ByteCoachProperties {

    private Review review = new Review();
    private Notification notification = new Notification();
    private Community community = new Community();
    private Adaptive adaptive = new Adaptive();
    private Plan plan = new Plan();
    private Interview interview = new Interview();
    private Dashboard dashboard = new Dashboard();
    private RateLimit rateLimit = new RateLimit();
    private AiQuota aiQuota = new AiQuota();
    private Document document = new Document();

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
        /** Cron for daily review/plan reminder notifications. */
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
    public static class Plan {
        /** Cron for daily plan health check. */
        private String cronHealthCheck = "0 0 23 * * ?";
        /** Completion rate below this triggers auto-adjustment. */
        private double lowCompletionThreshold = 0.5;
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
}
