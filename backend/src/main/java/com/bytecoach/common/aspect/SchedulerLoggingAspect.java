package com.bytecoach.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that automatically logs execution timing and errors
 * for all @Scheduled methods in the application.
 */
@Slf4j
@Aspect
@Component
public class SchedulerLoggingAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object logScheduledExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[Scheduler] {} completed in {}ms", method, elapsed);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[Scheduler] {} failed after {}ms: {}", method, elapsed, ex.getMessage(), ex);
            throw ex;
        }
    }
}
