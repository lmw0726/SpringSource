package com.lmw.springaop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 启动类
 *
 * @author LaiMingWei
 * @version 1.1.0
 * @since 2026-05-02 22:21
 */

@SpringBootApplication
public class SpringAopLearnApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringAopLearnApplication.class, args);
	}

}
