package com.lmw.springmvc.returnValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.servlet.mvc.method.annotation.AsyncTaskMethodReturnValueHandler;

import java.util.concurrent.TimeUnit;

/**
 * WebAsyncTask返回值解析示例
 * 这个控制器是 {@link AsyncTaskMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-22 22:13
 */
@Controller
@RequestMapping("/returnValue")
public class AsyncTaskMethodController {
	private static final Logger logger = LoggerFactory.getLogger(AsyncTaskMethodController.class);
	private static ThreadPoolTaskExecutor executor;

	static {
		executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(5);
		executor.setThreadNamePrefix("PartAsync-");
		executor.initialize();
	}

	/**
	 * WebAsyncTask类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/webAsyncTask")
	public WebAsyncTask<String> webAsyncTask() {

		WebAsyncTask<String> task = new WebAsyncTask<>(60_000L, executor, () -> {
			TimeUnit.SECONDS.sleep(2);
			return "Hello WebAsyncTask!";
		});
		task.onCompletion(() -> logger.info("WebAsyncTask Complete!"));
		task.onError(() -> {
			logger.info("WebAsyncTask Complete!");
			return "WebAsyncTask Error!";
		});
		return task;
	}
}
