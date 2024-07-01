package com.lmw.springmvc.config;

import com.lmw.springmvc.filter.HelloWorld2Filter;
import com.lmw.springmvc.servlet.HelloWorld2Servlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.Servlet;

/**
 * Servlet第二种注册方式
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 20:25
 */
@Configuration
public class ServletConfig {
	@Bean
	public ServletRegistrationBean<Servlet> servletRegistrationBean() {
		ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(new HelloWorld2Servlet());
		bean.addUrlMappings("/hello2");
		return bean;
	}

	@Bean
	public FilterRegistrationBean<Filter> helloWorldFilter() {
		FilterRegistrationBean<Filter> helloWorldFilter = new FilterRegistrationBean<>();
		helloWorldFilter.addUrlPatterns("/hello2/*");
		helloWorldFilter.setFilter(new HelloWorld2Filter());
		return helloWorldFilter;
	}
}
