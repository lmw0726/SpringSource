package com.lmw.springmvc.argument;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * 带有@ModelAttribute以及简单类型的参数解析器示例
 * 这个控制器是 {@link ServletModelAttributeMethodProcessor} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-04 15:45
 */
@Controller
@RequestMapping("/argument")
public class ServletModelAttributeMethodController {

	/**
	 * 带有@ModelAttribute 的示例
	 * 访问 /argument/modelAttributeName?name=lmw 时，解析到的name为空字符串""
	 * 访问 /argument/modelAttributeName?string=lmw 时,可以解析到name=lmw
	 *
	 * @param name 名称
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeName")
	public ResponseEntity<String> modelAttributeName(@ModelAttribute String name) {
		return ResponseEntity.ok(name);
	}

	/**
	 * 带有@ModelAttribute 的示例，从模板变量中取值
	 * 要想从模板变量中取到值，@ModelAttribute的属性为空时，模板变量的名称{string} 必须与类的首字母小写一致。
	 * 如：/argument/modelAttribute/uriTemplateVariables/lmw12?s=lmw，此时，解析到的参数值为lmw12
	 *
	 * @param s 字符串参数
	 * @return 解析结果
	 */
	@GetMapping("/modelAttribute/uriTemplateVariables/{string}")
	public ResponseEntity<String> modelAttributeUriTemplateVariables(@ModelAttribute String s) {
		return ResponseEntity.ok(s);
	}


	/**
	 * 带有@ModelAttribute 的示例，并且@ModelAttribute的value有属性值
	 * 访问：/argument/modelAttributeNameString?name=lmw
	 * 将会获取到属性值
	 *
	 * @param s 名称
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeNameString")
	public ResponseEntity<String> modelAttributeNameString(@ModelAttribute("name") String s) {
		return ResponseEntity.ok(s);
	}

	/**
	 * 带有@ModelAttribute并指定其属性值，模板变量和@ModelAttribute的属性值相同时，可以从模板变量中获取到数据。
	 * 访问：/argument/modelAttribute/uriTemplateVariables/name/abc?name=456
	 * 将会获取到属性值abc
	 *
	 * @param s 字符串参数
	 * @return 解析结果
	 */
	@GetMapping("/modelAttribute/uriTemplateVariables/name/{name}")
	public ResponseEntity<String> modelAttributeUriTemplateVariablesName(@ModelAttribute("name") String s) {
		return ResponseEntity.ok(s);
	}

	/**
	 * 带有@ModelAttribute注解的实体类示例，其中@ModelAttribute的属性为空
	 * 有没有@ModelAttribute都一样。
	 * 都将会通过 ModelAttributeMethodProcessor#bindRequestParameters(binder, webRequest);
	 * 绑定参数，设置实体参数值。
	 * 可通过两种形式传参：
	 * 1、/argument/modelAttributeEntity?name=abc&age=18
	 * 2、/argument/modelAttributeEntity + form-data的形式。form-data中需要添加name和age参数名和参数值
	 *
	 * @param dto 用户请求模型
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeEntity")
	public ResponseEntity<String> modelAttributeEntity(@ModelAttribute UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}

	/**
	 * 没有@ModelAttribute注解的实体类示例
	 * 解析过程同 modelAttributeEntity
	 *
	 * @param dto 用户请求模型
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeDto")
	public ResponseEntity<String> modelAttributeDto(UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}

	/**
	 * 带有@ModelAttribute注解的实体类示例，带有@Validated注解
	 * 将name或age传入空值，将会提示400错误。
	 * 这是由 ModelAttributeMethodProcessor#validateIfApplicable(binder, parameter);
	 * 进行校验的
	 *
	 * @param dto 用户请求模型
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeValidatedEntity")
	public ResponseEntity<String> modelAttributeValidatedEntity(@Validated @ModelAttribute UserDto dto) {
		return ResponseEntity.ok(dto.toString());
	}
}
