package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;

import javax.servlet.http.Cookie;

/**
 * 带有@Cookie注解使用示例控制器
 * 这个控制器是 {@link ServletCookieValueMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-31 22:10
 */
@Controller
@RequestMapping("/argument")
public class ServletCookieValueMethodController {

	/**
	 * CookieValue没有属性名的解析实例
	 *
	 * @param name Cookie的名称
	 * @return 解析结果
	 */
	@GetMapping("/cookie")
	public ResponseEntity<String> cookie(@CookieValue Cookie name) {
		return ResponseEntity.ok(name.getValue());
	}

	/**
	 * CookieValue带有属性名的解析实例
	 *
	 * @param cookie Cookie的名称
	 * @return 解析结果
	 */
	@GetMapping("/cookieName")
	public ResponseEntity<String> cookieName(@CookieValue("name") Cookie cookie) {
		return ResponseEntity.ok(cookie.getValue());
	}

	/**
	 * CookieValue没有属性名，并且参数是字符串的解析实例
	 *
	 * @param name 属性名称作为Cookie名称的字符串参数
	 * @return 解析结果
	 */
	@GetMapping("/cookieString")
	public ResponseEntity<String> cookieName(@CookieValue String name) {
		return ResponseEntity.ok(name);
	}
}
