package com.offerpilot.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MybatisPlusConfig {

    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * Slow query interceptor — logs any SQL taking longer than 500ms.
     */
    @Bean
    public Interceptor slowQueryInterceptor() {
        return new SlowQueryInterceptor(SLOW_QUERY_THRESHOLD_MS);
    }

    @Intercepts({
            @Signature(type = StatementHandler.class, method = "query", args = {java.sql.Statement.class, org.apache.ibatis.session.ResultHandler.class}),
            @Signature(type = StatementHandler.class, method = "update", args = {java.sql.Statement.class})
    })
    static class SlowQueryInterceptor implements Interceptor {
        private final long thresholdMs;

        SlowQueryInterceptor(long thresholdMs) {
            this.thresholdMs = thresholdMs;
        }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            long start = System.currentTimeMillis();
            try {
                return invocation.proceed();
            } finally {
                long duration = System.currentTimeMillis() - start;
                if (duration > thresholdMs && invocation.getTarget() instanceof StatementHandler handler) {
                    String sql = handler.getBoundSql().getSql();
                    log.warn("SQL SLOW QUERY ({}ms): {}", duration, sql.replaceAll("\\s+", " ").trim());
                }
            }
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}

