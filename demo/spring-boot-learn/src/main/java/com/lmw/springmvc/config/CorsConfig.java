package com.lmw.springmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-07 22:26
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// 所有接口
		registry.addMapping("/**")
				// 是否发送 Cookie
				.allowCredentials(true)
				// 支持域
				.allowedOriginPatterns("*")
				// 支持方法
				.allowedMethods("GET","POST", "PUT", "DELETE")
				.allowedHeaders("*")
				.exposedHeaders("*");
	}
}
