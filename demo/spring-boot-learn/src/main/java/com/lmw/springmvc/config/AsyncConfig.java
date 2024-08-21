package com.lmw.springmvc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 异步线程池配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-21 21:33
 */
@Configuration
public class AsyncConfig implements WebMvcConfigurer {
	private AsyncTaskExecutor taskExecutor;

	public AsyncConfig(@Qualifier("asyncTaskExecutor") AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 配置异步请求处理选项。
	 *
	 * @param configurer 帮助配置异步请求处理选项的类
	 */
	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setTaskExecutor(taskExecutor);
		configurer.setDefaultTimeout(30_000);
	}
}
