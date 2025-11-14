package com.MediConnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous method execution.
 * Enables @Async annotation support and configures a thread pool for notification processing.
 * 
 * This allows notification creation to run in the background, improving response times
 * for user actions like liking posts, commenting, etc.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Configures a thread pool executor for async notification processing.
     * 
     * @return Executor with configured thread pool settings
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(5);
        
        // Maximum pool size: maximum number of threads that can be created
        executor.setMaxPoolSize(10);
        
        // Queue capacity: number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("notification-async-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time for tasks to complete (in seconds)
        executor.setAwaitTerminationSeconds(60);
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }
}

