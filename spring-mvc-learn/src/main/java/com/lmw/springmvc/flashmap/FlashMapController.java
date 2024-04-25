package com.lmw.springmvc.flashmap;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * FlashMap使用
 *
 * @author LMW
 * @version 1.0
 * @date 2024-04-24 21:59
 */
@Controller
public class FlashMapController {
	/**
	 * 添加订单
	 *
	 * @param request 请求
	 * @return 重定向到URL /submit/success
	 */
	@PostMapping("/order")
	public String getOrder(HttpServletRequest request) {
		FlashMap flashMap = (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		flashMap.put("states", "Success");
		return "redirect:/submit/success";
	}

	/**
	 * 提交订单成功，并从闪存属性中获取到订单状态值
	 *
	 * @param model 模型
	 * @return 订单状态
	 */
	@GetMapping("/submit/success")
	@ResponseBody
	public ResponseEntity<String> submitSuccess(Model model) {
		String states = (String) model.getAttribute("states");
		System.out.println(states);
		return ResponseEntity.ok(states);
	}

	/**
	 * 取消订单
	 *
	 * @param redirectAttributes 重定向属性
	 * @param orderId            订单编号
	 * @return 重定向到URL /cancel/success
	 */
	@PostMapping("/cancelOrder")
	public String cancelOrder(RedirectAttributes redirectAttributes, String orderId, HttpServletResponse response) {
		redirectAttributes.addFlashAttribute("orderId", orderId);
		if (orderId == null) {
			redirectAttributes.addFlashAttribute("notExistOrder", true);
		}
		return "redirect:/cancel/success";
	}

	/**
	 * 取消订单成功，将会从 /cancelOrder 获取到闪存属性 orderId值
	 *
	 * @param redirectAttributes 模型
	 * @return 返回取消订单成功的消息，并且返回订单编号
	 */
	@GetMapping("/cancel/success")
	public ResponseEntity<String> cancelSuccess(Model model,RedirectAttributes redirectAttributes, HttpServletResponse response) {
		String orderId = (String) model.getAttribute("orderId");
		if (orderId == null) {
			redirectAttributes.addFlashAttribute("notExistOrder", true);
			response.addHeader("location", "/cancel/fail");
			return ResponseEntity.status(300).build();
		}
		return ResponseEntity.ok("Cancel order success! OrderId is " + orderId);
	}

	/**
	 * 取消订单失败
	 *
	 * @param model 模型
	 * @return 返回取消订单失败的消息
	 */
	@GetMapping("/cancel/fail")
	public ResponseEntity<String> cancelFail(Model model) {
		Object notExistOrder = model.getAttribute("notExistOrder");
		return ResponseEntity.status(400).body("Cancel order fail,because orderId is not exist:" + notExistOrder);
	}
}
