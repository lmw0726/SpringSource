package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.method.annotation.SessionStatusMethodArgumentResolver;

import javax.servlet.http.HttpSession;

/**
 * SessionStatus参数解析示例
 * 这个控制器是 {@link SessionStatusMethodArgumentResolver} 参数解析器
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-09 22:56
 */
@Controller
@RequestMapping("/argument")
@SessionAttributes("name")
public class SessionStatusMethodController {
	/**
	 * 设置Session属性值
	 *
	 * @param name  名称
	 * @return 是否设置了Session
	 */
	@GetMapping("/setName")
	public ResponseEntity<String> setName(String name, HttpSession httpSession) {
		httpSession.setAttribute("name", name);
		return ResponseEntity.ok("设置成功，Session中的名称属性值为" + name);
	}

	/**
	 * SessionStatus使用示例
	 * 调用SessionStatus#setComplete后，所有的Session属性都会被清空
	 *
	 * @param model  session属性
	 * @param status Session状态
	 * @return session是否被清除
	 */
	@GetMapping("/sessionStatus")
	public ResponseEntity<Boolean> sessionStatus(ModelMap model, SessionStatus status) {
		System.out.println("SessionStatus的类型为" + status.getClass().getName());
		Object name = model.getAttribute("name");
		System.out.println("调用SessionStatus#setComplete前，名称为" + name);
		status.setComplete();
		return ResponseEntity.ok(status.isComplete());
	}

	/**
	 * 获取Session名称
	 *
	 * @param model  session属性
	 * @param status Session状态
	 * @return session是否被清除
	 */
	@GetMapping("/getSessionName")
	public ResponseEntity<Object> getSessionName(ModelMap model, SessionStatus status) {
		Object name = model.getAttribute("name");
		System.out.println("调用SessionStatus#setComplete后，名称为" + name);
		return ResponseEntity.ok(status.isComplete());
	}
}
