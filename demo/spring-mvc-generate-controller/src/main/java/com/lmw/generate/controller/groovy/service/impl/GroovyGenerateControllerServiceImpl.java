package com.lmw.generate.controller.groovy.service.impl;

import com.lmw.generate.controller.groovy.service.GenerateControllerService;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Groovy生成Controller实现类
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-28 22:04
 */
@SuppressWarnings("rawtypes")
@Service("groovyGenerateControllerService")
public class GroovyGenerateControllerServiceImpl implements GenerateControllerService {

	private final ApplicationContext applicationContext;
	private final DefaultListableBeanFactory defaultListableBeanFactory;
	private final RequestMappingHandlerMapping requestMappingHandlerMapping;
	private final Object mappingRegistry;
	private final Method getMappingsByDirectPath;

	public GroovyGenerateControllerServiceImpl(ApplicationContext applicationContext,
											   DefaultListableBeanFactory defaultListableBeanFactory,
											   RequestMappingHandlerMapping requestMappingHandlerMapping) {
		this.applicationContext = applicationContext;
		this.defaultListableBeanFactory = defaultListableBeanFactory;
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
		// 获取映射注册对象
		this.mappingRegistry = getMappingRegistry();
		// MappingRegistry#getMappingsByDirectPath 方法，用于获取对应路径的请求映射信息
		Method getMappingsByDirectPath = ReflectionUtils.findMethod(mappingRegistry.getClass(), "getMappingsByDirectPath", String.class);
		getMappingsByDirectPath.setAccessible(true);
		this.getMappingsByDirectPath = getMappingsByDirectPath;
	}


	@Override
	public ResponseEntity<String> generate() {
		//定义Groovy脚本引擎的根路径
		Resource resource = applicationContext.getResource("classpath:/groovy/GenerateController.groovy");

		URL[] urls = new URL[0];
		try {
			urls = new URL[]{resource.getURL()};
		} catch (IOException e) {
			throw new RuntimeException("获取Groovy文件发生异常", e);
		}
		GroovyScriptEngine engine = new GroovyScriptEngine(urls);
		Class javaClazz = null;
		try {
			javaClazz = engine.loadScriptByName("GenerateController.groovy");
		} catch (ResourceException e) {
			throw new RuntimeException("GenerateController.groovy 脚本文件不存在", e);
		} catch (ScriptException e) {
			throw new RuntimeException("GenerateController.groovy 脚本解析异常", e);
		}

		GroovyClassLoader groovyLoader = engine.getGroovyClassLoader();

		//使用类名作为bean名称
		String beanName = javaClazz.getSimpleName();
		if (defaultListableBeanFactory.containsBeanDefinition(beanName)) {
			return ResponseEntity.ok("already generated!");
		}
		//创建bean定义构建器.
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(javaClazz);

		//动态注册bean.
		defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
		try {
			// 启动注册Controller，使用反射调用原生内部方法，虽然spring提供了开放接口，但是接口参数比较复杂，直接反射省事
			Method method = RequestMappingHandlerMapping.class.getSuperclass().getSuperclass().getDeclaredMethod("detectHandlerMethods", Object.class);
			//将private改为可使用
			method.setAccessible(true);
			method.invoke(requestMappingHandlerMapping, beanName);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		// 清空Groovy脚本缓存
		groovyLoader.clearCache();

		return ResponseEntity.ok("generate");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<String> unload() throws IOException, ScriptException, ResourceException {
		//定义Groovy脚本引擎的根路径
		Resource resource = applicationContext.getResource("classpath:/groovy/GenerateController.groovy");
		URL[] urls = new URL[]{resource.getURL()};
		GroovyScriptEngine engine = new GroovyScriptEngine(urls);
		Class javaClazz = engine.loadScriptByName("GenerateController.groovy");
		// 获取bean名称
		String beanName = javaClazz.getSimpleName();
		List<RequestMappingInfo> requestMappingInfosFromControllerClass = getRequestMappingInfosFromControllerClass(javaClazz);

		// 移除 /groovy/hello 路径
		String path = requestMappingInfosFromControllerClass.get(0).getPatternValues().iterator().next();
		List<RequestMappingInfo> requestMappingInfoList = getRequestMappingInfos(path);
		if (requestMappingInfoList == null) {
			return ResponseEntity.ok("Url not found!");
		}
		GroovyClassLoader groovyLoader = engine.getGroovyClassLoader();
		// 锁定 请求映射信息，防止删除时，访问报错。
		synchronized (requestMappingInfoList) {

			if (defaultListableBeanFactory.containsBean(beanName)) {
				// 销毁bean定义
				defaultListableBeanFactory.removeBeanDefinition(beanName);
				// 销毁单例
				defaultListableBeanFactory.destroySingleton(beanName);
			}

			// 遍历 请求映射信息 列表
			// 复制一个映射信息表，防止遍历时的并发删除异常
			List<RequestMappingInfo> requestMappingInfos = new ArrayList<>(requestMappingInfoList);
			Iterator<RequestMappingInfo> iterator = requestMappingInfos.iterator();
			// 不能使用 for循环删除，会报并发删除错误。
			while (iterator.hasNext()) {
				RequestMappingInfo requestMappingInfo = iterator.next();
				// 这里可以 匹配最佳的请求映射，再进行删除
				// 销毁映射关系
				requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
				iterator.remove();
			}

		}
		// 清空Groovy脚本缓存
		groovyLoader.clearCache();

		return ResponseEntity.ok("Unload success!");
	}

	/**
	 * 从控制器类中获取映射信息
	 *
	 * @param controllerClazz 控制器类
	 * @return 控制器类的映射信息
	 */
	private List<RequestMappingInfo> getRequestMappingInfosFromControllerClass(Class<?> controllerClazz) {
		// 1.类上的注解获取url
		Annotation annotation = controllerClazz.getAnnotation(RequestMapping.class);
		RequestMapping requestMapping = (RequestMapping) annotation;
		String prefix = "";
		if (requestMapping != null) {
			String[] value = requestMapping.value();
			prefix = value[0];
		}
		// 请求信息列表
		List<RequestMappingInfo> requestMappingInfoList = new ArrayList<>();
		//查询所有方法
		Method[] declaredMethods = controllerClazz.getDeclaredMethods();
		/**
		 * 2.方法上的注解获取url
		 * 最终拼接到一快，防止出现//，最后统一替换一次
		 */
		for (Method method : declaredMethods) {

			// 获取方法上的PostMapping注解
			RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (mapping != null) {
				String[] value = mapping.value();
				String url = ((prefix + value[0]).replaceAll("//", "/"));
				RequestMappingInfo requestMappingInfo = RequestMappingInfo.paths(url).methods(mapping.method()).build();
				requestMappingInfoList.add(requestMappingInfo);
			}
		}
		return requestMappingInfoList;
	}

	/**
	 * 调用 org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.MappingRegistry#getMappingsByDirectPath 方法，用于获取对应路径的请求映射信息
	 *
	 * @param path URL路径
	 * @return 映射信息列表
	 */
	@SuppressWarnings("unchecked")
	private List<RequestMappingInfo> getRequestMappingInfos(String path) {
		Object mappings = null;
		try {
			mappings = getMappingsByDirectPath.invoke(mappingRegistry, path);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		if (mappings == null) {
			return null;

		}
		List<RequestMappingInfo> requestMappingInfoList = (List<RequestMappingInfo>) mappings;
		return requestMappingInfoList;
	}

	/**
	 * 获取映射注册表
	 *
	 * @return 映射注册表
	 */
	private Object getMappingRegistry() {
		Method[] methods = ReflectionUtils.getDeclaredMethods(requestMappingHandlerMapping.getClass().getSuperclass().getSuperclass());
		Object invoke = null;
		for (Method method : methods) {
			if (method.getName().equals("getMappingRegistry")) {
				method.setAccessible(true);
				try {
					invoke = method.invoke(requestMappingHandlerMapping, new Object[]{});
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				}
				break;
			}
		}
		return invoke;
	}
}
