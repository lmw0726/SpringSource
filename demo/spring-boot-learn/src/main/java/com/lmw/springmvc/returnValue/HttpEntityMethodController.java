package com.lmw.springmvc.returnValue;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;

/**
 * HttpEntity和ResponseEntit返回值解析示例
 * 这个控制器是 {@link HttpEntityMethodProcessor} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-25 21:59
 */
@RequestMapping("/returnValue")
@Controller("httpEntityMethodReturnValueController")
public class HttpEntityMethodController {

	/**
	 * ResponseEntity返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/responseEntity")
	public ResponseEntity<String> responseEntity() {
		return ResponseEntity.ok("ResponseEntity");
	}


	/**
	 * HttpEntity返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/httpEntity")
	public HttpEntity<String> httpEntity() {
		HttpEntity<String> httpEntity = new ResponseEntity<String>("HttpEntity", HttpStatus.OK);
		return httpEntity;
	}
}
