package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Part;
import java.util.HashMap;
import java.util.Map;

/**
 * RequestParam Map参数解析器示例，其中 RequestParam的name属性不为空
 *这个控制器是 {@link RequestParamMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-28 21:36
 */
@Controller
@RequestMapping("/argument")
public class RequestParamMethodController {
	/**
	 * 带有@RequestParam修饰的Map参数解析示例
	 * 这种写法会抛出类型转换失败异常
	 *
	 * @param map 带有@RequestParam修饰的Map
	 * @return 解析好的Map参数
	 */
	@ResponseBody
	@GetMapping("/requestParamNameMap")
	public Map<String, Object> requestParamNameMap(@RequestParam(name = "name") Map<String, Object> map) {
		return map;
	}

	/**
	 * 带有@RequestParam修饰的String的参数解析示例
	 *
	 * @param s 带有@RequestParam 字符串
	 * @return 解析好的结果
	 */
	@ResponseBody
	@GetMapping("/requestParamString")
	public Map<String, Object> requestParamString(@RequestParam(name = "name") String s) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", s);
		return map;
	}

	/**
	 * 带有@RequestParam修饰的String的参数解析示例，其中 @RequestParam的 name 为空
	 *
	 * @param s 带有@RequestParam 字符串
	 * @return 解析好的结果
	 */
	@ResponseBody
	@GetMapping("/requestParamString2")
	public Map<String, Object> requestParamString2(@RequestParam String s) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", s);
		return map;
	}

	/**
	 * 带有@RequestPart注解的参数示例
	 *
	 * @param part 带有@RequestPart注解的Part
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/requestPart")
	public ResponseEntity<String> requestPart(Part part) {
		return ResponseEntity.ok(part.toString());
	}

	/**
	 * MultipartFile 的参数解析示例
	 *
	 * @param file 多部分文件
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/multipartFile")
	public ResponseEntity<String> multipartFile(MultipartFile file) {
		return ResponseEntity.ok(file.toString());
	}

	/**
	 * 简单参数解析示例
	 *
	 * @param s 字符串
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/string")
	public ResponseEntity<String> string(String s) {
		return ResponseEntity.ok(s);
	}
}
