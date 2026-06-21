package com.example.trackingbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    // config threadpool for job schedule
    // spring will use less thread if dont have this config
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Bean
    public ThreadPoolTaskScheduler trackingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4); // set 4 pool de chay, tranh bi delay cac thread schedule
        scheduler.setThreadNamePrefix("tracking-scheduler-");
        scheduler.setDaemon(true); //không cố giữ app sống nếu app đang shutdown
        scheduler.setWaitForTasksToCompleteOnShutdown(true);//Khi app tắt, chờ job đang chạy xong thay vì cắt ngang ngay.
        scheduler.setAwaitTerminationSeconds(30);//Chờ tối đa 30 giây.
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setErrorHandler(error -> log.error("Scheduled job failed", error));

        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(trackingTaskScheduler());
    }
}
