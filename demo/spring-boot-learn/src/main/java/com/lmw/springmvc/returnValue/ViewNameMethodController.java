package com.lmw.springmvc.returnValue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;

/**
 * 字符串类型的返回值解析示例
 * 这个控制器是 {@link ViewNameMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class ViewNameMethodController {

	/**
	 * 字符串类型返回值解析示例
	 *
	 * @return 实体类
	 */
	@GetMapping("/string")
	public String string() {
		return "redirect:/image/flower.jpg";
	}
}
