/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * 实现 {@link org.springframework.beans.factory.config.BeanPostProcessor} 的类，用于将 ServletContext 传递给实现了 {@link ServletContextAware} 接口的 bean。
 *
 * <p>Web 应用程序上下文将自动将此类注册到其底层 bean 工厂。应用程序不直接使用此类。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.XmlWebApplicationContext#postProcessBeanFactory
 * @since 12.03.2004
 */
public class ServletContextAwareProcessor implements BeanPostProcessor {
	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * Servlet配置
	 */
	@Nullable
	private ServletConfig servletConfig;


	/**
	 * 创建一个没有初始上下文或配置的新 ServletContextAwareProcessor。
	 * 当使用此构造函数时，应重写 {@link #getServletContext()} 和/或 {@link #getServletConfig()} 方法。
	 */
	protected ServletContextAwareProcessor() {
	}

	/**
	 * 为给定的上下文创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * 为给定的配置创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(ServletConfig servletConfig) {
		this(null, servletConfig);
	}

	/**
	 * 为给定的上下文和配置创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
		this.servletContext = servletContext;
		this.servletConfig = servletConfig;
	}


	/**
	 * 返回要注入的 {@link ServletContext} 或 {@code null}。当在注册后获取上下文时，子类可以覆盖此方法。
	 */
	@Nullable
	protected ServletContext getServletContext() {
		// 如果 Servlet 上下文为 null，并且 Servlet 配置不为 null
		if (this.servletContext == null && getServletConfig() != null) {
			// 返回 Servlet 配置的 Servlet 上下文
			return getServletConfig().getServletContext();
		}
		// 否则返回当前的 Servlet 上下文
		return this.servletContext;
	}

	/**
	 * 返回要注入的 {@link ServletConfig} 或 {@code null}。当在注册后获取上下文时，子类可以覆盖此方法。
	 */
	@Nullable
	protected ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// 如果 Servlet上下文 不为 null，并且 bean 实现了 Servlet上下文感知 接口
		if (getServletContext() != null && bean instanceof ServletContextAware) {
			// 将 Servlet 上下文设置给 bean
			((ServletContextAware) bean).setServletContext(getServletContext());
		}
		// 如果 Servlet配置 不为 null，并且 bean 实现了 Servlet配置感知 接口
		if (getServletConfig() != null && bean instanceof ServletConfigAware) {
			// 将 Servlet 配置设置给 bean
			((ServletConfigAware) bean).setServletConfig(getServletConfig());
		}
		// 返回 bean
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
