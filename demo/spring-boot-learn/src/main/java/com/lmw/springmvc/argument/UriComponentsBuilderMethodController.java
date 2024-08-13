package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.UriComponentsBuilderMethodArgumentResolver;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * UriComponentsBuilder参数示例
 * 这个控制器是 {@link UriComponentsBuilderMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-13 22:02
 */
@Controller
@RequestMapping("/argument")
public class UriComponentsBuilderMethodController {
	/**
	 * UriComponentsBuilder参数示例
	 * 解析结果为：http://127.0.0.1:8080/uri
	 *
	 * @param builder UriComponentsBuilder
	 * @return 解析结果
	 */
	@GetMapping("/uriComponentsBuilder")
	public ResponseEntity<String> uriComponentsBuilder(UriComponentsBuilder builder) {
		// 构建一个指向 "/uri" 的 URI
		UriComponents uriComponents = builder.path("/uri").build();
		// 将 URI 转换为字符串
		String uri = uriComponents.toUriString();
		return ResponseEntity.ok(uri);
	}
}
