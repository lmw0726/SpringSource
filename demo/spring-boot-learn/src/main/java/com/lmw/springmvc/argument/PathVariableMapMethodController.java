package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMapMethodArgumentResolver;

import java.util.Map;

/**
 * 带有@PathVariable的Map类型示例
 * 这个控制器是 {@link PathVariableMapMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-04 14:49
 */
@Controller
@RequestMapping("/argument")
public class PathVariableMapMethodController {

	/**
	 * 带有@PathVariable注解的Map参数示例
	 *
	 * @param map Map参数
	 * @return 解析结果
	 */
	@GetMapping("/pathVariableMap/{name}/{age}")
	public ResponseEntity<Map<String, Object>> pathVariableMap(@PathVariable Map<String, Object> map) {
		return ResponseEntity.ok(map);
	}
}
