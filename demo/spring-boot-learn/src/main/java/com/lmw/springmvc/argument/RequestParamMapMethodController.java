package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Part;
import java.util.Map;

/**
 * RequestParam Map参数解析器示例
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-24 21:49
 */
@Controller
@RequestMapping("/argument")
public class RequestParamMapMethodController {
	/**
	 * 带有@RequestParam修饰的Map参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的Map
	 * @return 解析好的Map参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMap")
	public Map<String, Object> requestParamMap(@RequestParam Map<String, Object> map) {
		return map;
	}

	/**
	 * 带有@RequestParam修饰的上传文件的Map参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的Map
	 * @return 解析好的Map参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMapFile")
	public ResponseEntity<String> requestParamMapFile(@RequestParam Map<String, MultipartFile> map) {
		return ResponseEntity.ok(map.toString());
	}

	/**
	 * 带有@RequestParam修饰的上传Part文件的Map参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的Map
	 * @return 解析好的Map参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMapPart")
	public ResponseEntity<String> requestParamMapPart(@RequestParam Map<String, Part> map) {
		return ResponseEntity.ok(map.toString());
	}


	/**
	 * 带有@RequestParam修饰的MultiValueMap参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的MultiValueMap
	 * @return 解析好的MultiValueMap参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMultiValueMap")
	public MultiValueMap<String, Object> requestParamMap(@RequestParam MultiValueMap<String, Object> map) {
		return map;
	}

	/**
	 * 带有@RequestParam修饰的上传文件的MultiValueMap参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的MultiValueMap
	 * @return 解析好的MultiValueMap参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMultiValueMapFile")
	public ResponseEntity<String> requestParamMapFile(@RequestParam MultiValueMap<String, MultipartFile> map) {
		return ResponseEntity.ok(map.toString());
	}

	/**
	 * 带有@RequestParam修饰的上传Part文件的MultiValueMap参数解析器示例
	 *
	 * @param map 带有@RequestParam修饰的MultiValueMap
	 * @return 解析好的MultiValueMap参数
	 */
	@ResponseBody
	@GetMapping("/requestParamMultiValueMapPart")
	public ResponseEntity<String> requestParamMapPart(@RequestParam MultiValueMap<String, Part> map) {
		return ResponseEntity.ok(map.toString());
	}
}
