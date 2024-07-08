package com.lmw.springmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-08 21:58
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/image/**")
				.addResourceLocations("classpath:/image/");
	}
}
