package com.lmw.springmvc.flashmap;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * FlashMap使用
 *
 * @author LMW
 * @version 1.0
 * @date 2024-04-24 21:59
 */
@Controller
public class FlashMapController {
	@PostMapping("/order")
	public String getOrder(HttpServletRequest request) {
		FlashMap flashMap = (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		flashMap.put("states", "Success");
		return "redirect:/submit/success";
	}

	@GetMapping("/submit/success")
	@ResponseBody
	public ResponseEntity<String> submitSuccess(Model model) {
		String states = (String) model.getAttribute("states");
		System.out.println(states);
		return ResponseEntity.ok(states);
	}
}
