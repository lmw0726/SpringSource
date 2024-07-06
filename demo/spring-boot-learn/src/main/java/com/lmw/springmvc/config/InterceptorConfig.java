package com.lmw.springmvc.config;

import com.lmw.springmvc.interceptor.Hello2Interceptor;
import com.lmw.springmvc.interceptor.HelloInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * 拦截器配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-06 22:38
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		InterceptorRegistration registration = registry.addInterceptor(new HelloInterceptor());
		registration.addPathPatterns("/**");
		registration.excludePathPatterns("/hello");
		registration.order(1);

	}

	@Bean
	public MappedInterceptor mappedInterceptor() {
		// 这种方式无法定制拦截器的执行顺序，不如上面的灵活。
		Hello2Interceptor hello2Interceptor = new Hello2Interceptor();
		String[] includePatterns = {"/**"};
		String[] excludePatterns = {"/hello"};
		return new MappedInterceptor(includePatterns, excludePatterns, hello2Interceptor);
	}
}
