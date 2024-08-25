package com.lmw.springmvc.config;

import com.lmw.springmvc.resolver.MyModelAndViewResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Arrays;

/**
 * ModelAndViewResolver配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-25 21:10
 */
@Configuration
public class ModelAndViewResolverConfig extends WebMvcConfigurationSupport {

	/**
	 * 用于插入 {@link RequestMappingHandlerAdapter} 的自定义子类的受保护方法。
	 *
	 * @since 4.3
	 */
	@Override
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = super.createRequestMappingHandlerAdapter();
		requestMappingHandlerAdapter.setModelAndViewResolvers(Arrays.asList(new MyModelAndViewResolver()));
		return requestMappingHandlerAdapter;
	}

}
