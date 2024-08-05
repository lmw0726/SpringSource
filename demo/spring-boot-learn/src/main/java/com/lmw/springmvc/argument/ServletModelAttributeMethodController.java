package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * 带有@ModelAttribute以及简单类型的参数解析器示例
 * 这个控制器是 {@link ServletModelAttributeMethodProcessor} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-04 15:45
 */
@Controller
@RequestMapping("/argument")
public class ServletModelAttributeMethodController {

	/**
	 * 带有@ModelAttribute 的示例
	 *
	 * @param name 名称
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeName")
	public ResponseEntity<String> modelAttributeName(@ModelAttribute String name) {
		return ResponseEntity.ok(name);
	}

	/**
	 * 带有@ModelAttribute注解的实体类示例，其中@ModelAttribute的属性为空
	 *
	 * @param dto 用户请求模型
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeEntity")
	public ResponseEntity<String> modelAttributeEntity(@ModelAttribute UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}
}
