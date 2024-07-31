package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestAttributeMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 带有@RequestAttribute注解的参数解析器示例
 * 这个控制器是 {@link RequestAttributeMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-31 12:46
 */
@Controller
@RequestMapping("/argument")
public class RequestAttributeMethodController {

	/**
	 * 转发到/argument/requestAttributeMap
	 *
	 * @param map 请求参数
	 * @return 转发地址
	 */
	@GetMapping("/forwardRequestAttributeMap")
	public String forwardRequestAttributeMap(@RequestParam Map<String, Object> map) {
		HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
		map.forEach((s, o) -> request.setAttribute(s, o));
		return "forward:/argument/requestAttributeMap";
	}

	/**
	 * 带有@RequestAttribute注解的Map参数示例
	 * 不能使用Map接收，因为没有String转Map的转换器，会提示400错误
	 *
	 * @param map Map参数
	 * @return 解析结果
	 */
	@GetMapping("/requestAttributeMap")
	public ResponseEntity<Map<String, Object>> requestAttributeMap(@RequestAttribute Map<String, Object> map) {
		return ResponseEntity.ok(map);
	}

	/**
	 * 转发到/argument/requestAttributeString
	 *
	 * @param map 请求参数
	 * @return 转发地址
	 */
	@GetMapping("/forwardRequestAttributeString")
	public String forwardRequestAttributeString(@RequestParam Map<String, Object> map) {
		HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
		map.forEach((s, o) -> request.setAttribute(s, o));
		return "forward:/argument/requestAttributeString";
	}

	/**
	 * 带有@RequestAttribute注解的字符串参数示例
	 *
	 * @param s 字符串参数
	 * @return 解析结果
	 */
	@GetMapping("/requestAttributeString")
	public ResponseEntity<String> requestAttributeString(@RequestAttribute("name") String s) {
		return ResponseEntity.ok(s);
	}
}
