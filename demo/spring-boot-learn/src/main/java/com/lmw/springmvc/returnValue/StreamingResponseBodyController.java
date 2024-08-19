package com.lmw.springmvc.returnValue;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBodyReturnValueHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * StreamingResponseBody类型的返回值解析示例
 * 这个控制器是 {@link StreamingResponseBodyReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-19 21:28
 */
@Controller
@RequestMapping("/returnValue")
public class StreamingResponseBodyController {

	/**
	 * StreamingResponseBody类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/streamingResponseBody")
	public StreamingResponseBody streamingResponseBody() {
		ResourceLoader resourceLoader = new ClassRelativeResourceLoader(StreamingResponseBodyController.class);
		Resource resource = resourceLoader.getResource("classpath:/image/flower.jpg");
		return outputStream -> {
			try (InputStream inputStream = resource.getInputStream()) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
			}
		};
	}

	/**
	 * ResponseEntity<StreamingResponseBody>类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/responseEntity/streamingResponseBody")
	public ResponseEntity<StreamingResponseBody> exportExcel() {
		// 设置响应头
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", new String("TestExcel.xlsx".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

		// 返回 StreamingResponseBody
		ResourceLoader resourceLoader = new ClassRelativeResourceLoader(StreamingResponseBodyController.class);
		Resource resource = resourceLoader.getResource("classpath:/static/TestExcel.xlsx");
		StreamingResponseBody stream = outputStream -> {
			try (InputStream inputStream = resource.getInputStream();
				 OutputStream out = outputStream) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}
		};

		return ResponseEntity.ok()
				.headers(headers)
				.body(stream);
	}
}
