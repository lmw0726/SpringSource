package com.lmw.springmvc.returnValue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.MapMethodProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Map类型的返回值解析示例
 * 这个控制器是 {@link MapMethodProcessor} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class MapMethodController {

	/**
	 * Map类型返回值解析示例
	 * 无法通过 MapMethodProcessor 跳转到响应的视图
	 * 会返回 404 错误。
	 *
	 * @return 解析结果
	 */
	@GetMapping("/map")
	public Map<String, Object> map() {
		HashMap<String, Object> map = new HashMap<>();
		map.put("name", "lmw");
		map.put("age", 18);
		return map;
	}
}
