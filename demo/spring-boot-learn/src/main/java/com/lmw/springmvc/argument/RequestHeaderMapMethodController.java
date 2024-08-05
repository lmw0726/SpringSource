package com.lmw.springmvc.argument;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver;

import java.util.Map;

/**
 * 解析带有@RequestHeader的Map参数示例
 * 这个控制器是 {@link RequestHeaderMapMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-04 15:08
 */
@Controller
@RequestMapping("/argument")
public class RequestHeaderMapMethodController {
	/**
	 * 带有@RequestHeader的Map参数并指定请求头名称的示例
	 * 该示例中@RequestHeader的value属性无效。
	 * 并不会获取指定名称的请求头——请求头值映射，而是会获取到所有Header值。
	 *
	 * @param map Map参数
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderNameMap")
	public ResponseEntity<String> requestHeaderNameMap(@RequestHeader("name") Map<String, Object> map) {
		return ResponseEntity.ok(map.toString());
	}

	/**
	 * 带有@RequestHeader的Map参数的示例
	 * 会获取到所有的Header值映射
	 *
	 * @param map Map参数
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderMap")
	public ResponseEntity<String> requestHeaderMap(@RequestHeader Map<String, Object> map) {
		return ResponseEntity.ok(map.toString());
	}

	/**
	 * 带有@RequestHeader的MultiValueMap参数的示例
	 * 获取多个相同名称的Header值。
	 *
	 * @param map MultiValueMap参数
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderMultiValueMap")
	public ResponseEntity<String> requestHeaderMultiValueMap(@RequestHeader MultiValueMap<String, Object> map) {
		return ResponseEntity.ok(map.toString());
	}

	/**
	 * 带有@RequestHeader的HttpHeaders参数的示例
	 * 获取多个相同名称的Header值。
	 *
	 * @param headers 请求头数据
	 * @return 解析结果
	 */
	@GetMapping("/requestHeaderHttpHeaders")
	public ResponseEntity<String> requestHeaderHttpHeaders(@RequestHeader HttpHeaders headers) {
		return ResponseEntity.ok(headers.toString());
	}
}
