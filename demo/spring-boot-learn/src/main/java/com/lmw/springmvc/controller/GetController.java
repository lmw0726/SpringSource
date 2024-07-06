package com.lmw.springmvc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * GetMapping测试
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-30 23:06
 */
@Controller
public class GetController {
	@GetMapping("/index")
	public ResponseEntity<String> index(){
		return ResponseEntity.ok("index");
	}
}
