package com.lmw.springmvc.returnValue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ModelAndViewMethodReturnValueHandler;

/**
 * ModelAndView返回值解析示例
 * 这个控制器是 {@link ModelAndViewMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-24 21:47
 */
@Controller
@RequestMapping("/returnValue")
public class ModelAndViewMethodController {
	/**
	 * ModelAndView返回值示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/modelAndView")
	public ModelAndView modelAndView() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("/image/flower.jpg");
		return modelAndView;
	}
}
