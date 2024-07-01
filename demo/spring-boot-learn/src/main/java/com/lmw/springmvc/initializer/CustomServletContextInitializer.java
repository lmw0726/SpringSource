package com.lmw.springmvc.initializer;

import com.lmw.springmvc.filter.HelloWorld3Filter;
import com.lmw.springmvc.servlet.HelloWorld3Servlet;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import java.util.EnumSet;

/**
 * 第三种注册Servlet的方法
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 21:51
 */
@Configuration
public class CustomServletContextInitializer implements ServletContextInitializer {

	private static final String HELLO_URL = "/hello3";

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		System.out.println("创建自定义Servlet");
		ServletRegistration.Dynamic servlet = servletContext.addServlet(
				HelloWorld3Servlet.class.getSimpleName(), new HelloWorld3Servlet());
		servlet.addMapping("/hello3");

		FilterRegistration.Dynamic filter = servletContext.addFilter(
				HelloWorld3Filter.class.getSimpleName(), new HelloWorld3Filter());
		EnumSet<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);
		dispatcherTypes.add(DispatcherType.REQUEST);
		dispatcherTypes.add(DispatcherType.FORWARD);
		filter.addMappingForServletNames(dispatcherTypes, true, HELLO_URL);
	}
}
