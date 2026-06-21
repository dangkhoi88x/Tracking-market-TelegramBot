package com.example.trackingbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {
        // config threadpool cho task async/ tac vu chay nen
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "telegramTaskExecutor")
    public ThreadPoolTaskExecutor telegramTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // thread co ban
        executor.setMaxPoolSize(8); // max thread
        executor.setQueueCapacity(100); // xep hang task toi da
        executor.setThreadNamePrefix("telegram-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // full queue ->task sẽ chạy trên thread gọi hiện tại thay vì bị vứt bỏ.
        executor.initialize();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return telegramTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, params) ->
                log.error("Async method failed: {}", method.getName(), exception);
    }
}
