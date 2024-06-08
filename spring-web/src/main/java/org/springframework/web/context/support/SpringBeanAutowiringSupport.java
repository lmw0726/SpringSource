/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * 一个方便的基类，用于在基于 Spring 的 Web 应用程序中自动装配类。解析端点类中的 {@code @Autowired} 注解与当前 Spring 根 Web 应用程序上下文中的 bean 相匹配（由当前线程的上下文 ClassLoader 决定，该 ClassLoader 需要是 Web 应用程序的 ClassLoader）。
 * 可以作为基类或委托的方式使用。
 *
 * <p>这个基类的典型用法是 JAX-WS 端点类：这样一个基于 Spring 的 JAX-WS 端点实现将遵循端点类的标准 JAX-WS 合同，但会是“轻量级”的，因为它将实际工作委托给一个或多个 Spring 管理的服务 bean - 通常是使用 {@code @Autowired} 获得的。
 * 这种端点实例的生命周期将由 JAX-WS 运行时管理，因此需要此基类基于当前 Spring 上下文提供 {@code @Autowired} 处理。
 *
 * <p><b>注意：</b>如果有明确的方法来访问 ServletContext，请优先使用该方法。{@link WebApplicationContextUtils} 类允许根据 ServletContext 轻松访问 Spring 根 Web 应用程序上下文。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see WebApplicationObjectSupport
 */
public abstract class SpringBeanAutowiringSupport {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(SpringBeanAutowiringSupport.class);


	/**
	 * 此构造函数对此实例执行注入，基于当前 Web 应用程序上下文。
	 * <p>用作基类。
	 * @see #processInjectionBasedOnCurrentContext
	 */
	public SpringBeanAutowiringSupport() {
		processInjectionBasedOnCurrentContext(this);
	}


	/**
	 * 根据当前 Web 应用程序上下文处理给定目标对象的 {@code @Autowired} 注入。
	 * <p>用作委托。
	 * @param target 要处理的目标对象
	 * @see org.springframework.web.context.ContextLoader#getCurrentWebApplicationContext()
	 */
	public static void processInjectionBasedOnCurrentContext(Object target) {
		Assert.notNull(target, "Target object must not be null");
		// 获取当前的 Web应用程序上下文
		WebApplicationContext cc = ContextLoader.getCurrentWebApplicationContext();
		if (cc != null) {
			// 如果当前 Web应用程序上下文 不为空，则创建 自动装配注解Bean后处理器 对象
			AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
			// 设置 自动装配注解Bean后处理器 的 Bean工厂
			bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());
			// 处理目标对象的注入
			bpp.processInjection(target);
		} else {
			// 如果当前 Web应用程序上下文 为空，则记录警告日志
			if (logger.isWarnEnabled()) {
				logger.warn("Current WebApplicationContext is not available for processing of " +
						ClassUtils.getShortName(target.getClass()) + ": " +
						"Make sure this class gets constructed in a Spring web application after the " +
						"Spring WebApplicationContext has been initialized. Proceeding without injection.");
			}
		}
	}


	/**
	 * 根据存储在 ServletContext 中的当前根 Web 应用程序上下文，处理给定目标对象的 {@code @Autowired} 注入。
	 * <p>用作委托。
	 * @param target 要处理的目标对象
	 * @param servletContext 查找 Spring Web 应用程序上下文的 ServletContext
	 * @see WebApplicationContextUtils#getWebApplicationContext(javax.servlet.ServletContext)
	 */
	public static void processInjectionBasedOnServletContext(Object target, ServletContext servletContext) {
		Assert.notNull(target, "Target object must not be null");
		// 获取要求的 Web 应用程序上下文
		WebApplicationContext cc = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

		// 创建 自动装配注解Bean后置处理器 对象
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();

		// 设置 自动装配注解Bean后置处理器 的 Bean工厂
		bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());

		// 处理目标对象的注入
		bpp.processInjection(target);
	}

}
