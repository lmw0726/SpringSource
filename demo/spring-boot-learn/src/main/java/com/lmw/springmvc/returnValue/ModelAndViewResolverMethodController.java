package com.lmw.springmvc.returnValue;

import com.lmw.springmvc.resolver.MySpecialArg;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ModelAndViewResolverMethodReturnValueHandler;

/**
 * 自定义ModelAndView类型解析器
 * 这个控制器是 {@link ModelAndViewResolverMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-25 21:22
 */
@Controller
@RequestMapping("/returnValue")
public class ModelAndViewResolverMethodController {

	/**
	 * 自定义ModelAndView类型示例
	 * 通过ModelAndViewResolver解析返回值
	 *
	 * @return 解析结果
	 */
	@GetMapping("/custom/modelAndView")
	public MySpecialArg customModeAndView() {
		return new MySpecialArg("abc");
	}
}
