package com.lmw.springmvc.argument;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RedirectAttributesMethodArgumentResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;

/**
 * RedirectAttributes 参数示例
 * 这个控制器是 {@link RedirectAttributesMethodArgumentResolver} 参数解析器
 *
 * @author LMW
 * @version 1.0
 * @date 2024-08-13 22:04
 */
@Controller
@RequestMapping("/argument")
public class RedirectAttributesMethodController {

	/**
	 * RedirectAttributes 参数示例
	 *
	 * @param redirectAttributes 重定向属性
	 * @param s                  字符串
	 * @return 重定向到URL /cancel/success
	 */
	@PostMapping("/redirectAttributes")
	public String redirectAttributes(RedirectAttributes redirectAttributes, String s, HttpServletResponse response) {
		redirectAttributes.addFlashAttribute("s", s);
		if (s == null) {
			redirectAttributes.addAttribute("notExist", true);
		}
		return "redirect:/argument/redirectAttributes/success";
	}

	/**
	 * 重定向成功，将会从 /redirectAttributes 获取到闪存属性 s值
	 *
	 * @param redirectAttributes 模型
	 * @return 返回取消订单成功的消息，并且返回订单编号
	 */
	@GetMapping("/redirectAttributes/success")
	public ResponseEntity<String> redirectAttributesSuccess(Model model, RedirectAttributes redirectAttributes, Boolean notExist) {
		String s = (String) model.getAttribute("s");
		if (s == null) {
			return ResponseEntity.ok(String.valueOf(notExist));
		}
		return ResponseEntity.ok(s);
	}

}
