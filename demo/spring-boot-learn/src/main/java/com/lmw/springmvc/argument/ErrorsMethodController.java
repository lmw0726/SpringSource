package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.ErrorsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver;

/**
 * 解析Errors参数的示例
 * 这个控制器是 {@link ErrorsMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-02 22:50
 */
@Controller
@RequestMapping("/argument")
public class ErrorsMethodController {

	/**
	 * 通过传递一个空值，触发Errors接收到异常
	 * BindingResult 和 Errors 接收到的实际上是同一个对象。
	 * 因为 BindingResult是Errors的子接口
	 *
	 * @param dto           请求体
	 * @param bindingResult 绑定结果
	 * @param errors        错误
	 * @return 解析结果
	 */
	@PostMapping("/errors")
	public ResponseEntity<String> errors(@Validated @RequestBody ErrorsDto dto, BindingResult bindingResult, Errors errors) {
		if (errors.hasErrors()) {
			// 如果有验证错误，返回错误信息
			return ResponseEntity.ok("errorPage");
		}
		// 处理成功的用户逻辑
		return ResponseEntity.ok("successPage");
	}

}
