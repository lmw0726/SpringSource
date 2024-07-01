package com.lmw.springmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 以注解启动的SpringMVC项目启动类
 *
 * @author LMW
 * @version 1.0
 * @since 2024-06-30 21:49
 */
@ServletComponentScan
@SpringBootApplication
public class SpringMvcApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringMvcApplication.class, args);
	}
}
