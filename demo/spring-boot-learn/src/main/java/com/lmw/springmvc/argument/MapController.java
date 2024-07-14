package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Map;

/**
 * Map参数解析控制器
 * 这个控制器是 {@link MapMethodProcessor} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-12 22:36
 */
@Controller
@SessionAttributes("name")
@RequestMapping("/argument")
public class MapController {

	//<editor-fold desc="在会话场景下，使用Map接收指定名称的会话属性（需要在当前类上添加 @SessionAttributes 注解）">

	/**
	 * 向Session中设置Attribute属性
	 *
	 * @param session 模型参数
	 * @return 设置成功的消息
	 */
	@GetMapping("/setAttribute")
	public ResponseEntity<String> setAttribute(HttpSession session, String name) {
		session.setAttribute("name", name);
		return ResponseEntity.ok("OK");
	}


	/**
	 * Map参数解析
	 *
	 * @param map map参数
	 * @return 解析结果
	 */
	@GetMapping("/map")
	public ResponseEntity<Map<String, Object>> map(Map<String, Object> map) {
		return ResponseEntity.ok(map);
	}
	//</editor-fold>


	//<editor-fold desc="重定向场景下使用Map接收数据">

	/**
	 * 重定向场景中使用Map接收数据
	 *
	 * @param request 请求参数
	 * @return 重定向到图片URL
	 */
	@GetMapping("/redirect/map")
	public String redirectMap(HttpServletRequest request) {
		FlashMap flashMap = (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String paramName = parameterNames.nextElement();
			flashMap.put(paramName, request.getParameter(paramName));
		}
		return "redirect:/argument/receive/map";
	}

	/**
	 * 使用Map接收重定向参数
	 *
	 * @param map map参数
	 * @return 接收到的Map参数
	 */
	@GetMapping("/receive/map")
	public ResponseEntity<Map<String, Object>> receiveMap(Map<String, Object> map) {
		System.out.println(map);
		return ResponseEntity.ok(map);
	}
	//</editor-fold>
}
