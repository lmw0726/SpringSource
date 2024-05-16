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

package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}，将初始化和销毁回调应用于实现了
 * {@link javax.servlet.Servlet} 接口的bean。
 *
 * <p>在 bean 实例初始化之后，将调用 Servlet 的 {@code init} 方法，并提供一个包含 Servlet 名称和它所在的
 * ServletContext 的 ServletConfig。
 *
 * <p>在 bean 实例销毁之前，将调用 Servlet 的 {@code destroy} 方法。
 *
 * <p><b>请注意，此后处理器不支持 Servlet 初始化参数。</b> 实现 Servlet 接口的 Bean 实例应该像任何其他
 * Spring Bean 一样进行配置，即通过构造函数参数或 Bean 属性。
 *
 * <p>对于在普通 Servlet 容器中重用 Servlet 实现以及在 Spring 上下文中作为 Bean 使用，请考虑从 Spring 的
 * {@link org.springframework.web.servlet.HttpServletBean} 基类派生，该基类将 Servlet 初始化参数
 * 应用为 Bean 属性，支持标准 Servlet 和 Spring Bean 初始化风格。
 *
 * <p><b>或者，考虑使用 Spring 的 {@link org.springframework.web.servlet.mvc.ServletWrappingController}
 * 对 Servlet 进行包装。</b> 这对于现有 Servlet 类特别合适，允许指定 Servlet 初始化参数等。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
 * @see javax.servlet.Servlet#destroy()
 * @see SimpleServletHandlerAdapter
 * @since 1.1.5
 */
public class SimpleServletPostProcessor implements
		DestructionAwareBeanPostProcessor, ServletContextAware, ServletConfigAware {

	/**
	 * 设置是否使用通过 {@code setServletConfig} 传入的共享 ServletConfig 对象（如果可用）。
	 */
	private boolean useSharedServletConfig = true;

	/**
	 * Servlet 上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * Servlet 配置
	 */
	@Nullable
	private ServletConfig servletConfig;


	/**
	 * 设置是否使用通过 {@code setServletConfig} 传入的共享 ServletConfig 对象（如果可用）。
	 * <p>默认为 "true"。将此设置为 "false"，以传入一个模拟的 ServletConfig 对象，其中包含 bean 名称作为
	 * Servlet 名称，持有当前的 ServletContext。
	 *
	 * @see #setServletConfig
	 */
	public void setUseSharedServletConfig(boolean useSharedServletConfig) {
		this.useSharedServletConfig = useSharedServletConfig;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			ServletConfig config = this.servletConfig;
			if (config == null || !this.useSharedServletConfig) {
				config = new DelegatingServletConfig(beanName, this.servletContext);
			}
			try {
				((Servlet) bean).init(config);
			} catch (ServletException ex) {
				throw new BeanInitializationException("Servlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			((Servlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Servlet);
	}


	/**
	 * ServletConfig 接口的内部实现，传递给包装的 Servlet。
	 */
	private static class DelegatingServletConfig implements ServletConfig {

		private final String servletName;

		@Nullable
		private final ServletContext servletContext;

		public DelegatingServletConfig(String servletName, @Nullable ServletContext servletContext) {
			this.servletName = servletName;
			this.servletContext = servletContext;
		}

		@Override
		public String getServletName() {
			return this.servletName;
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return this.servletContext;
		}

		@Override
		@Nullable
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.emptySet());
		}
	}

}
