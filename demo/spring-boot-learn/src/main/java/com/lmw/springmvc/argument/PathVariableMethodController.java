package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;

import java.util.Map;

/**
 * 参数解析器
 * 带有@PathVariable的方法参数解析器示例
 * 这个控制器是 {@link PathVariableMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-15 23:17
 */
@Controller
@RequestMapping("/argument")
public class PathVariableMethodController {
	/**
	 * 带有@PathVariable 路径变量的示例
	 *
	 * @param name 变量名称
	 * @return 含有路径变量名称的结果
	 */
	@GetMapping("/path/variable/{name}")
	public ResponseEntity<String> pathVariableName(@PathVariable("name") String name) {
		return ResponseEntity.ok(name);
	}

	/**
	 * 带有@PathVariable 路径变量，且使用Map参数进行接收的示例
	 *
	 * @param map 变量名称和值
	 * @return 含有路径变量Map的结果
	 */
	@GetMapping("/path/variable/map/{name}/{value}")
	public ResponseEntity<Map<String, Object>> pathVariableMap(@PathVariable Map<String, Object> map) {
		return ResponseEntity.ok(map);
	}

}
