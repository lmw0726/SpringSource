package com.lmw.springmvc.config;

import com.lmw.springmvc.entity.UserDto;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;

import java.util.List;

/**
 * 自定义WebArgumentResolver配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-13 21:10
 */
@Configuration
public class WebArgumentResolverConfig implements WebMvcConfigurer {
	/**
	 * 添加解析器以支持自定义控制器方法参数类型。
	 * <p>这不会覆盖内置的处理程序方法参数解析支持。要自定义内置的参数解析支持，请直接配置 {@link RequestMappingHandlerAdapter}。
	 *
	 * @param resolvers 初始为空列表
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new ServletWebArgumentResolverAdapter((methodParameter, webRequest) -> {
			UserDto userDto = new UserDto();
			String aaa = webRequest.getParameter("AAA");
			if (aaa != null) {
				try {
					int age = Integer.parseInt(aaa);
					userDto.setAge(age);
				} catch (NumberFormatException e) {
				}
			}
			userDto.setName(webRequest.getParameter("BBB"));
			return userDto;
		}));
	}
}
