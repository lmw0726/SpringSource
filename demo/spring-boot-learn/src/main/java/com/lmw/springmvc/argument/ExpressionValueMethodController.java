package com.lmw.springmvc.argument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 带有@Value参数名称的使用示例
 * 这个控制器是 {@link ExpressionValueMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-31 22:39
 */
@Controller
@RequestMapping("/argument")
public class ExpressionValueMethodController {
	/**
	 * 生成一个单例的Bean
	 *
	 * @return 对应映射Bean
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	/**
	 * 在带有@Value中设置Bean引用，注入一个ObjectMapper对象
	 *
	 * @param objectMapper 对象映射器
	 * @return 如果注入了对象映射器，则返回序列化后的字符串。否则返回 “ObjectMapper is null!”；
	 */
	@GetMapping("/valueBean")
	public ResponseEntity<String> valueBean(@Value("#{@objectMapper}") ObjectMapper objectMapper) {
		if (objectMapper == null) {
			return ResponseEntity.ok("ObjectMapper is null!");
		}
		Map<String, Object> map = new HashMap<>();
		map.put("name", "WMW");
		try {
			String json = objectMapper.writeValueAsString(map);
			return ResponseEntity.ok(json);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
