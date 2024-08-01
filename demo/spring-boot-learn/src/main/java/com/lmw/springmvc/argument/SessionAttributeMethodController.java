package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.method.annotation.SessionAttributeMethodArgumentResolver;

/**
 * 在方法参数标注@SessionAttribute的解析器示例
 * 这个控制器是 {@link SessionAttributeMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-01 21:04
 */
@Controller
@RequestMapping("/argument")
public class SessionAttributeMethodController {

	/**
	 * 获取设置的@SessionAttribute
	 *
	 * @param name 名称
	 * @return 解析结果
	 */
	@GetMapping("/sessionAttribute")
	public ResponseEntity<String> sessionAttribute(@SessionAttribute String name) {
		return ResponseEntity.ok(name);
	}

	/**
	 * 获取设置的带有属性值的@SessionAttribute
	 *
	 * @param s 名称
	 * @return 解析结果
	 */
	@GetMapping("/sessionAttributeName")
	public ResponseEntity<String> sessionAttributeName(@SessionAttribute("name") String s) {
		return ResponseEntity.ok(s);
	}
}
