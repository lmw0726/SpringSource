package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver;

import java.security.Principal;

/**
 * Principal参数解析示例
 * 这个控制器是 {@link PrincipalMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-11 10:26
 */
@Controller
@RequestMapping("/argument")
public class PrincipalMethodController {
	/**
	 * Principal参数解析示例
	 *实际上，Principal的参数解析的逻辑已经被 ServletRequestMethodArgumentResolver 取代了。
	 *
	 * @param principal 用户信息
	 * @return 解析结果
	 */
	@GetMapping("/principal")
	public ResponseEntity<String> pricipal(Principal principal) {
		if (principal == null) {
			return ResponseEntity.ok("Principal为null");
		}
		return ResponseEntity.ok(principal.getClass().getName() + " " + principal.getName());
	}
}
