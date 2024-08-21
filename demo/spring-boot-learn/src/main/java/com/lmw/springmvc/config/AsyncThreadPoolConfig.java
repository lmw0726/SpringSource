package com.lmw.springmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步线程池配置——全局配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-21 21:40
 */
@Configuration
public class AsyncThreadPoolConfig {
	/**
	 * 异步线程池，线程名前缀为Async-
	 *
	 * @return 异步线程池
	 */
	@Bean("asyncTaskExecutor")
	public AsyncTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("Async-");
		executor.initialize();
		return executor;
	}
}
