package com.lmw.springmvc.returnValue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.CallableMethodReturnValueHandler;

import java.util.concurrent.Callable;

/**
 * Callable类型返回值解析示例
 * 这个控制器是 {@link CallableMethodReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-22 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class CallableMethodController {
	/**
	 * Callable类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/callable")
	public Callable<String> callable() {
		return () -> "Hello Callable!";
	}
}
