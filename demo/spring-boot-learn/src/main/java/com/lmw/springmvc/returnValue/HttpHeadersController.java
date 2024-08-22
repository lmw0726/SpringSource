package com.lmw.springmvc.returnValue;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.HttpHeadersReturnValueHandler;

/**
 * HttpHeaders类型的返回值解析示例
 * 这个控制器是 {@link HttpHeadersReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class HttpHeadersController {

	/**
	 * HttpHeaders类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/httpHeaders")
	public HttpHeaders httpHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		return httpHeaders;
	}
}