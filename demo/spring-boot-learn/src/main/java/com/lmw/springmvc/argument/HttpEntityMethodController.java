package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;

/**
 * HttpEntity和RequestEntity的参数示例
 * 这个控制器是 {@link HttpEntityMethodProcessor} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-12 21:38
 */
@Controller
@RequestMapping("/argument")
public class HttpEntityMethodController {

	/**
	 * HttpEntity示例
	 *
	 * @param httpEntity Http实体
	 * @return 解析结果
	 */
	@GetMapping("/httpEntity")
	public ResponseEntity<String> httpEntity(HttpEntity<UserDto> httpEntity) {
		UserDto body = httpEntity.getBody();
		return ResponseEntity.ok(body.toString());
	}


	/**
	 * RequestEntity示例
	 *
	 * @param requestEntity Http实体
	 * @return 解析结果
	 */
	@GetMapping("/requestEntity")
	public ResponseEntity<String> requestEntity(RequestEntity<UserDto> requestEntity) {
		UserDto body = requestEntity.getBody();
		return ResponseEntity.ok(body.toString());
	}
}
