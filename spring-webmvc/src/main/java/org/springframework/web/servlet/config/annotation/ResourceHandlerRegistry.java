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

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * 存储资源处理器的注册信息，用于通过 Spring MVC 提供静态资源（如图像、CSS 文件等），
 * 包括设置优化的缓存头以实现高效的 Web 浏览器加载。资源可以从 Web 应用程序根目录下的位置、类路径等位置提供。
 *
 * <p>要创建资源处理器，请使用 {@link #addResourceHandler(String...)} 提供应调用处理器
 * 以提供静态资源的 URL 路径模式（例如 {@code "/resources/**"}）。
 *
 * <p>然后使用返回的 {@link ResourceHandlerRegistration} 上的附加方法，
 * 添加一个或多个提供静态内容的位置（例如 {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}）
 * 或指定要提供的资源的缓存期限。
 *
 * @author Rossen Stoyanchev
 * @see DefaultServletHandlerConfigurer
 * @since 3.1
 */
public class ResourceHandlerRegistry {

	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;

	/**
	 * 应用上下文
	 */
	private final ApplicationContext applicationContext;

	/**
	 * 内容协商管理器
	 */
	@Nullable
	private final ContentNegotiationManager contentNegotiationManager;

	/**
	 * URL 路径助手
	 */
	@Nullable
	private final UrlPathHelper pathHelper;

	/**
	 * 资源处理注册器列表
	 */
	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	/**
	 * 排序值
	 */
	private int order = Ordered.LOWEST_PRECEDENCE - 1;


	/**
	 * 为给定的应用程序上下文创建一个新的资源处理器注册表。
	 *
	 * @param applicationContext Spring 应用程序上下文
	 * @param servletContext     对应的 Servlet 上下文
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
		this(applicationContext, servletContext, null);
	}

	/**
	 * 为给定的应用程序上下文创建一个新的资源处理器注册表。
	 *
	 * @param applicationContext        Spring 应用程序上下文
	 * @param servletContext            对应的 Servlet 上下文
	 * @param contentNegotiationManager 要使用的内容协商管理器
	 * @since 4.3
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
								   @Nullable ContentNegotiationManager contentNegotiationManager) {

		this(applicationContext, servletContext, contentNegotiationManager, null);
	}

	/**
	 * {@link #ResourceHandlerRegistry(ApplicationContext, ServletContext, ContentNegotiationManager)} 的变体，
	 * 还接受用于将请求映射到静态资源的 {@link UrlPathHelper}。
	 *
	 * @since 4.3.13
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
								   @Nullable ContentNegotiationManager contentNegotiationManager, @Nullable UrlPathHelper pathHelper) {

		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
		this.contentNegotiationManager = contentNegotiationManager;
		this.pathHelper = pathHelper;
	}


	/**
	 * 添加资源处理器以提供静态资源。处理器被调用以处理匹配指定 URL 路径模式的请求。
	 * <p>支持诸如 {@code "/static/**"} 或 {@code "/css/{filename:\\w+\\.css}"} 的模式。
	 * <p>有关模式语法，请参见 {@link PathPattern} 当解析的模式 {@link PathMatchConfigurer#setPatternParser enabled} 或
	 * 否则参见 {@link AntPathMatcher}。语法基本相同，{@link PathPattern} 更适合 Web 使用且更高效。
	 */
	public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 是否已经为给定的路径模式注册了资源处理器。
	 */
	public boolean hasMappingForPattern(String pathPattern) {
		// 遍历所有资源处理器注册
		for (ResourceHandlerRegistration registration : this.registrations) {
			// 如果注册的路径模式包含指定的路径模式
			if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
				// 返回true表示已找到匹配的路径模式
				return true;
			}
		}
		// 返回false表示未找到匹配的路径模式
		return false;
	}

	/**
	 * 指定资源处理相对于其他 {@link HandlerMapping HandlerMappings} 配置在 Spring MVC 应用程序上下文中的顺序。
	 * <p>默认值为 {@code Integer.MAX_VALUE-1}。
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回具有映射资源处理器的处理器映射；如果没有注册，则返回 {@code null}。
	 */
	@Nullable
	protected AbstractHandlerMapping getHandlerMapping() {
		// 如果没有资源处理器注册
		if (this.registrations.isEmpty()) {
			// 返回null表示没有处理器映射
			return null;
		}

		// 创建一个有序的URL映射
		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<>();

		// 遍历所有资源处理器注册
		for (ResourceHandlerRegistration registration : this.registrations) {
			// 获取资源请求处理器
			ResourceHttpRequestHandler handler = getRequestHandler(registration);

			// 遍历注册的路径模式
			for (String pathPattern : registration.getPathPatterns()) {
				// 将路径模式和处理器添加到URL映射中
				urlMap.put(pathPattern, handler);
			}
		}

		// 返回一个新的简单URL处理器映射
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

	@SuppressWarnings("deprecation")
	private ResourceHttpRequestHandler getRequestHandler(ResourceHandlerRegistration registration) {
		// 获取资源请求处理器
		ResourceHttpRequestHandler handler = registration.getRequestHandler();

		// 如果路径助手不为空，设置URL路径助手
		if (this.pathHelper != null) {
			handler.setUrlPathHelper(this.pathHelper);
		}

		// 如果内容协商管理器不为空，设置内容协商管理器
		if (this.contentNegotiationManager != null) {
			handler.setContentNegotiationManager(this.contentNegotiationManager);
		}

		// 设置Servlet上下文
		handler.setServletContext(this.servletContext);
		// 设置应用程序上下文
		handler.setApplicationContext(this.applicationContext);

		try {
			// 初始化资源请求处理器的属性
			handler.afterPropertiesSet();
		} catch (Throwable ex) {
			// 抛出初始化异常
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}

		// 返回资源请求处理器
		return handler;
	}

}
