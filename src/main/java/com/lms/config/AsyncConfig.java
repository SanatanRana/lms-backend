package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * SYSTEM DESIGN: Asynchronous Task Offloading
 * ────────────────────────────────────────────
 * Non-critical work (emails, notifications, analytics, AI calls) runs
 * in a dedicated thread pool so the HTTP request thread returns fast.
 *
 * Under heavy load:
 *  - Core pool handles steady-state concurrency.
 *  - Queue absorbs burst traffic.
 *  - Max pool handles spikes beyond queue capacity.
 *  - CallerRunsPolicy provides back-pressure instead of rejecting tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);          // Steady-state threads
        executor.setMaxPoolSize(12);          // Spike handling
        executor.setQueueCapacity(100);       // Buffer for burst traffic
        executor.setThreadNamePrefix("lms-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
