package com.lmw.springmvc.argument;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestMethodArgumentResolver;

import javax.servlet.ServletRequest;
import javax.servlet.http.PushBuilder;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Servlet请求参数示例
 * 这个控制器是 {@link ServletRequestMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-13 22:17
 */
@Controller
@RequestMapping("/argument")
public class ServletRequestMethodController {

	/**
	 * WebRequest参数示例
	 *
	 * @param webRequest webRequest
	 * @return 解析结果
	 */
	@GetMapping("/webRequest")
	public ResponseEntity<String> webRequest(WebRequest webRequest) {
		return ResponseEntity.ok(webRequest.getClass().getName());
	}

	/**
	 * ServletRequest参数示例
	 *
	 * @param servletRequest servlet请求
	 * @return 解析结果
	 */
	@GetMapping("/servletRequest")
	public ResponseEntity<String> servletRequest(ServletRequest servletRequest) {
		return ResponseEntity.ok(servletRequest.getClass().getName());
	}

	/**
	 * HttpMethod参数示例
	 *
	 * @param httpMethod Http方法
	 * @return 解析结果
	 */
	@GetMapping("/httpMethod")
	public ResponseEntity<String> locale(HttpMethod httpMethod) {
		return ResponseEntity.ok(httpMethod.name());
	}

	/**
	 * Locale参数示例
	 *
	 * @param locale 区域设置
	 * @return 解析结果
	 */
	@GetMapping("/locale")
	public ResponseEntity<String> locale(Locale locale) {
		return ResponseEntity.ok(locale.toLanguageTag());
	}

	/**
	 * PushBuilder参数示例
	 *
	 * @param pushBuilder 发布构建器
	 * @return 解析结果
	 */
	@GetMapping("/pushBuilder")
	public ResponseEntity<String> pushBuilder(PushBuilder pushBuilder) {
		if (pushBuilder == null) {
			return ResponseEntity.ok("PushBuilder为null");
		}
		return ResponseEntity.ok(pushBuilder.getSessionId());
	}

	/**
	 * TimeZone参数示例
	 *
	 * @param timeZone 发布构建器
	 * @return 解析结果
	 */
	@GetMapping("/timeZone")
	public ResponseEntity<String> timeZone(TimeZone timeZone) {
		return ResponseEntity.ok(timeZone.getDisplayName());
	}

	/**
	 * ZoneId参数示例
	 *
	 * @param zoneId 发布构建器
	 * @return 解析结果
	 */
	@GetMapping("/zoneId")
	public ResponseEntity<String> zoneId(ZoneId zoneId) {
		return ResponseEntity.ok(zoneId.getId());
	}
}
