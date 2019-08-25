package com.trxs.pulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncTimerTaskConfig
{
    @Bean("timerTaskExecutor")
    public Executor taskExecutor()
    {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(8);// 最小线程数
        taskExecutor.setMaxPoolSize(10);// 最大线程数
        taskExecutor.setQueueCapacity(100);// 等待队列

        taskExecutor.initialize();

        return taskExecutor;
    }
}