package com.lmw.springmvc.returnValue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.ViewMethodReturnValueHandler;
import org.springframework.web.servlet.view.RedirectView;

/**
 * View类型的返回值解析示例
 * 这个控制器是 {@link ViewMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class ViewMethodController {

	/**
	 * View类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/redirectView")
	public View view() {
		return new RedirectView("/image/flower.jpg");
	}
}
