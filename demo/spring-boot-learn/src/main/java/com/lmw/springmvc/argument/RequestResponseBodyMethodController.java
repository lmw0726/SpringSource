package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * 带有@RequestBody的参数解析示例
 * 这个控制器是 {@link RequestResponseBodyMethodProcessor} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-12 21:14
 */
@Controller
@RequestMapping("/argument")
public class RequestResponseBodyMethodController {

	/**
	 * 带有@RequestBody的实体类参数示例
	 *
	 * @param dto 实体请求参数
	 * @return 解析结果
	 */
	@PostMapping("/requestBody/entity")
	public ResponseEntity<String> requestBodyEntity(@RequestBody UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}

	/**
	 * 带有@RequestBody、需要校验的实体类参数示例
	 *
	 * @param dto 实体请求参数
	 * @return 解析结果
	 */
	@PostMapping("/requestBody/entity/validated")
	public ResponseEntity<String> requestBodyEntityValidated(@Validated @RequestBody UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}


	/**
	 * 带有@ResponseBody的字符串参数示例
	 *
	 * @param json JSON字符串
	 * @return 解析结果
	 */
	@PostMapping("/requestBody/string")
	public ResponseEntity<String> requestBodyString(@RequestBody String json) {
		return ResponseEntity.ok(json);
	}
}
