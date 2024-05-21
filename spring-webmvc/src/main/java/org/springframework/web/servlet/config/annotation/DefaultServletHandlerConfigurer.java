/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import javax.servlet.ServletContext;
import java.util.Collections;

/**
 * 配置请求处理器，通过将请求转发到Servlet容器的“默认”Servlet来服务静态资源。
 * 当Spring MVC的 {@link DispatcherServlet} 映射到“/”时使用，这样就会覆盖Servlet容器默认的静态资源处理。
 *
 * <p>由于此处理器配置在最低优先级，因此它实际上允许所有其他处理器映射处理请求，
 * 如果没有一个处理请求，则此处理器可以将请求转发到“默认”Servlet。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see DefaultServletHttpRequestHandler
 * @since 3.1
 */
public class DefaultServletHandlerConfigurer {
	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;

	/**
	 * 默认ServletHttp请求处理器
	 */
	@Nullable
	private DefaultServletHttpRequestHandler handler;


	/**
	 * 创建一个 {@link DefaultServletHandlerConfigurer} 实例。
	 *
	 * @param servletContext 要使用的 ServletContext。
	 */
	public DefaultServletHandlerConfigurer(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 启用转发到“默认”Servlet。
	 * <p>使用此方法时，{@link DefaultServletHttpRequestHandler} 将尝试自动检测“默认”Servlet名称。
	 * 或者，您可以通过 {@link #enable(String)} 指定默认Servlet的名称。
	 *
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable() {
		enable(null);
	}

	/**
	 * 启用转发到由给定名称标识的“默认”Servlet。
	 * <p>当默认Servlet无法自动检测时，例如手动配置时，此方法很有用。
	 *
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable(@Nullable String defaultServletName) {
		// 创建默认的 Servlet HTTP 请求处理器
		this.handler = new DefaultServletHttpRequestHandler();
		// 如果默认的 Servlet 名称不为空
		if (defaultServletName != null) {
			// 设置默认的 Servlet 名称
			this.handler.setDefaultServletName(defaultServletName);
		}
		// 设置 Servlet 上下文
		this.handler.setServletContext(this.servletContext);
	}


	/**
	 * 返回一个在 {@link Ordered#LOWEST_PRECEDENCE} 优先级下的处理器映射实例，
	 * 包含映射到 {@code "/**"} 的 {@link DefaultServletHttpRequestHandler} 实例；
	 * 如果未启用默认Servlet处理，则返回 {@code null}。
	 *
	 * @since 4.3.12
	 */
	@Nullable
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		// 如果处理器为空，则返回空
		if (this.handler == null) {
			return null;
		}
		// 创建简单 URL 处理器映射，将 "/**" 映射到处理器
		return new SimpleUrlHandlerMapping(Collections.singletonMap("/**", this.handler),
				Ordered.LOWEST_PRECEDENCE);
	}

}
