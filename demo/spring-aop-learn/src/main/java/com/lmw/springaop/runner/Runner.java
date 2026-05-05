package com.lmw.springaop.runner;

import com.lmw.springaop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 测试入口
 *
 * @author LaiMingWei
 * @version 1.1.0
 * @since 2026-05-02 22:30
 */
@Component
public class Runner implements CommandLineRunner {

	@Autowired
	private UserService userService;

	@Override
	public void run(String... args) {
		userService.test();
	}
}