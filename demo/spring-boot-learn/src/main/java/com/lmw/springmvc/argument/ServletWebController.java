package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;

/**
 * WebArgumentResolver示例
 * 这个控制器是 {@link ServletWebArgumentResolverAdapter} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-13 21:04
 */
@Controller
@RequestMapping("/argument")
public class ServletWebController {
	/**
	 * 自定义ServeletWeb参数示例
	 * 访问：/argument/servletWebArgument?AAA=10&BBB=lmw
	 *
	 * @param dto 自定义参数
	 * @return 解析结果
	 */
	@GetMapping("/servletWebArgument")
	public ResponseEntity<String> servletWebArgument(UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}
}
