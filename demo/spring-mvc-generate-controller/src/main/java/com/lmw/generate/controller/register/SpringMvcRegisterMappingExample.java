package com.lmw.generate.controller.register;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;

/**
 * SpringMVC注册映射信息流程示例
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-29 23:29
 */
public class SpringMvcRegisterMappingExample {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
		// 创建 RequestMappingHandlerMapping 实例
		RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
		StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
		handlerMapping.setPatternParser(new PathPatternParser());
		handlerMapping.setApplicationContext(wac1);

		// 获取控制器类和方法
		Class<?> controllerClass = HelloController.class;
		Method method = controllerClass.getMethod("hello");

		// 获取 @RequestMapping 注解

		RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

		// 创建 RequestMappingInfo
		RequestMappingInfo requestMappingInfo = RequestMappingInfo.paths(requestMapping.value())
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.build();

		// 注册映射
		handlerMapping.registerMapping(requestMappingInfo, controllerClass.newInstance(), method);

		// 打印映射信息
		handlerMapping.getHandlerMethods().forEach((key, value) -> System.out.println(key + " -> " + value));
	}
}


@RestController
class HelloController {
	@RequestMapping("/hello")
	public String hello() {
		return "Hello, World!";
	}
}