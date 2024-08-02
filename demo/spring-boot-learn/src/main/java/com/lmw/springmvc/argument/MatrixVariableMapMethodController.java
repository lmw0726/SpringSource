package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.MatrixVariableMapMethodArgumentResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 带有@MatrixVariable注解的Map的示例
 * 这个控制器是 {@link MatrixVariableMapMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-01 23:04
 */
@Controller
@RequestMapping("/argument")
public class MatrixVariableMapMethodController {


	/**
	 * 使用@MatrixVariable 修饰  Map<String, Object> ，并且@MatrixVariable的name属性为空
	 * 访问 /argument/matrixVariable/map/map;name=LMW;age=18
	 * 可以成功接收到数据
	 *
	 * @param map 矩阵变量映射
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/map/{map}")
	public ResponseEntity<String> matrixVariableMap(@PathVariable String map, @MatrixVariable(pathVar = "map") Map<String, Object> names) {
		return ResponseEntity.ok(names.toString());
	}

	/**
	 * 多个路径变量
	 * 访问 /argument/matrixVariable/first;a=1;b=2/second;c=3;d=4
	 * 可以接收到结果
	 *
	 * @param first     第一个路径变量
	 * @param firstMap  第一个矩阵变量
	 * @param second    第二个路径变量
	 * @param secondMap 第二个矩阵变量
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/{first}/{second}")
	public ResponseEntity<Map<String, Object>> matrixMulti(
			@PathVariable("first") String first,
			@MatrixVariable(pathVar = "first") MultiValueMap<String, String> firstMap,
			@PathVariable("second") String second,
			@MatrixVariable(pathVar = "second") MultiValueMap<String, String> secondMap) {
		Map<String, Object> map = new HashMap<>();
		map.put("first", firstMap);
		map.put("second", secondMap);
		return ResponseEntity.ok(map);
	}
}
