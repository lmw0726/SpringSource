package com.lmw.springmvc.returnValue;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.DeferredResultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * DefferedResult类型返回值解析示例
 * 这个控制器是 {@link DeferredResultMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-21 22:17
 */
@Controller
@RequestMapping("/returnValue")
public class DeferredResultMethodController {
	/**
	 * DeferredResult类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/deferredResult")
	public DeferredResult<StreamingResponseBody> deferredResult(HttpServletResponse response) {
		response.setContentType("image/jpeg");

		// 创建局部的线程池
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(2);
		executor.setThreadNamePrefix("AsyncPart-");
		executor.initialize();

		ResourceLoader resourceLoader = new ClassRelativeResourceLoader(StreamingResponseBodyController.class);
		Resource resource = resourceLoader.getResource("classpath:/image/flower.jpg");
		DeferredResult<StreamingResponseBody> deferredResult = new DeferredResult<>();

		Future<?> future = executor.submit(() -> {
			StreamingResponseBody stream = outputStream -> {
				try (InputStream inputStream = resource.getInputStream()) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
				}
			};
			deferredResult.setResult(stream);
		});
		deferredResult.onCompletion(() -> {
			executor.shutdown();
		});
		deferredResult.onTimeout(() -> {
			System.out.println("超时啦");
		});

		return deferredResult;
	}
}
