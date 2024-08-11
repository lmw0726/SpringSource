package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.RequestPartMethodArgumentResolver;

import javax.servlet.http.Part;

/**
 * MultipartFile以及Part等参数示例
 * 这个控制器是 {@link RequestPartMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-11 21:49
 */
@Validated
@Controller
@RequestMapping("/argument")
public class RequestPartMethodController {
	/**
	 * MultipartFile示例，带有@RequestPart注解，并指定上传的文件参数
	 * form-data 输入f= 上传的文件，可解析到数据
	 *
	 * @param file 多部分文件
	 * @return 解析结果
	 */
	@ResponseBody
	@GetMapping("/requestPart/multipartFile")
	public ResponseEntity<String> requestPartMultipartFile(@RequestPart("f") MultipartFile file) {
		return ResponseEntity.ok(file.toString());
	}

	/**
	 * 使用@RequestPart修饰的字符串示例
	 * 访问 /argument/requestPart/string  form-data 输入string=123
	 * 可以解析到数据
	 *
	 * @param s 要解析的字符串
	 * @return 解析结果
	 */
	@GetMapping("/requestPart/string")
	public ResponseEntity<String> requestPartString(@RequestPart("string") String s) {
		return ResponseEntity.ok(s);
	}

	/**
	 * Part示例
	 * 该分支已被 {@link RequestParamMethodArgumentResolver} 提前处理了
	 *
	 * @param part 上传的文件
	 * @return 解析结果
	 */
	@GetMapping("/part")
	public ResponseEntity<String> part(Part part) {
		return ResponseEntity.ok(part.getName());
	}
}
