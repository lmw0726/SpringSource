package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.Gender;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;

/**
 * 带有RequestHeader注解的参数解析器示例
 * 这个控制器是 {@link RequestHeaderMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-30 21:46
 */
@Controller
@RequestMapping("/argument")
public class RequestHeaderMethodController {

	/**
	 * 带有@RequestHeader请求头的示例
	 *
	 * @param name 参数名
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderString")
	public ResponseEntity<String> requestHeaderString(@RequestHeader String name) {
		return ResponseEntity.ok(name);
	}

	/**
	 * 带有@RequestHeader并指定请求头名称的示例
	 *
	 * @param s 字符串参数
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderNameString")
	public ResponseEntity<String> requestHeaderNameString(@RequestHeader("name") String s) {
		return ResponseEntity.ok(s);
	}

	/**
	 * 带有@RequestHeader请求头，并且使用字符串数组接收的示例
	 *
	 * @param names 字符串数组参数
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderArrayString")
	public ResponseEntity<String[]> requestHeaderArrayString(@RequestHeader String[] names) {
		return ResponseEntity.ok(names);
	}

	/**
	 * 带有@RequestHeader修饰的枚举参数接收示例
	 * 在Header输入 MALE 和 FEMALE 都可以，输入其他值会提示 400 异常
	 *
	 * @param gender 枚举
	 * @return 解析结果
	 * @see org.springframework.core.convert.support.StringToEnumConverterFactory.StringToEnum
	 */
	@GetMapping("/requestHeaderEnum")
	public ResponseEntity<String> requestHeaderEnum(@RequestHeader Gender gender) {
		return ResponseEntity.ok(gender.getSource());
	}

}
