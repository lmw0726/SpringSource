package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 带有@ModelAttribute，从Session和闪存中取值的示例
 * 这个控制器是 {@link ServletModelAttributeMethodProcessor} 参数解析器
 * attribute = mavContainer.getModel().get(name);
 * 分支的示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-08 23:15
 */
@Controller
@SessionAttributes("name")
@RequestMapping("/argument")
public class ServletModelAttributeMethodSessionController {
	//<editor-fold desc="在会话场景下，使用带有@ModelAttribute接收指定名称的会话属性（需要在当前类上添加 @SessionAttributes 注解）">

	/**
	 * 向Session中设置Attribute属性
	 *
	 * @param session 模型参数
	 * @return 设置成功的消息
	 */
	@GetMapping("/setSessionName")
	public ResponseEntity<String> setSessionName(HttpSession session, String name) {
		session.setAttribute("name", name);
		return ResponseEntity.ok("OK");
	}


	/**
	 * 带有@ModelAttribute ，从Session中获取属性名称的示例
	 * 先访问/argument/setSessionName?name=lmw
	 * 再访问 /argument/modelAttributeSession?name=abc，可以接收到sesion的name属性值lmw
	 *
	 * @param name 名称
	 * @return 解析结果
	 */
	@GetMapping("/modelAttributeSession")
	public ResponseEntity<String> modelAttributeName(@ModelAttribute("name") String name) {
		return ResponseEntity.ok(name);
	}
	//</editor-fold>


	//<editor-fold desc="重定向场景下使用Map接收数据">

	/**
	 * 重定向场景中使用@ModelAttribute("name")接收参数
	 * 访问：/argument/redirect/modelAttribute/name?name=lmw
	 *
	 * @param request 请求参数
	 * @param name    名称
	 * @return 重定向到图片URL
	 */
	@GetMapping("/redirect/modelAttribute/name")
	public String redirectModelAttributeName(HttpServletRequest request, String name) {
		FlashMap flashMap = (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		flashMap.put("name", name);
		return "redirect:/argument/receive/modelAttribute/name";
	}

	/**
	 * 使用@ModelAttribute接收指定名称的重定向属性值
	 * 这里将会接受到 name=lmw
	 *
	 * @param name 重定向属性值
	 * @return 接收到的重定向属性值
	 */
	@GetMapping("/receive/modelAttribute/name")
	public ResponseEntity<String> receiveModelAttributeName(@ModelAttribute("name") String name) {
		return ResponseEntity.ok(name);
	}
	//</editor-fold>
}
