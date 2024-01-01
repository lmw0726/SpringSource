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

package org.springframework.web.reactive.config;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.ResourceTransformer;
import org.springframework.web.reactive.resource.ResourceTransformerSupport;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.server.WebHandler;

import java.util.*;

/**
 * 存储资源处理程序的注册信息，用于通过 Spring WebFlux 提供静态资源（如图像、CSS 文件等），包括设置针对在 Web 浏览器中高效加载的缓存头。资源可以从 Web 应用程序根目录、类路径和其他位置提供。
 *
 * <p>要创建资源处理程序，请使用 {@link #addResourceHandler(String...)}，提供要调用处理程序以提供静态资源的 URL 路径模式（例如 {@code "/resources/**"}）。
 *
 * <p>然后，使用返回的 {@link ResourceHandlerRegistration} 上的其他方法，添加一个或多个位置，从这些位置提供静态内容（例如 {@code "/"}、{@code "classpath:/META-INF/public-web-resources/"}），或者指定所提供资源的缓存期限。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceHandlerRegistry {

	/**
	 * 资源加载器
	 */
	private final ResourceLoader resourceLoader;

	/**
	 * 注册资源处理程序的列表
	 */
	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	/**
	 * 处理程序顺序，默认为 LOWEST_PRECEDENCE - 1
	 */
	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	/**
	 * 资源 URL 提供程序
	 */
	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 为给定的资源加载器（通常是应用程序上下文）创建一个新的资源处理程序注册表。
	 *
	 * @param resourceLoader 要使用的资源加载器
	 */
	public ResourceHandlerRegistry(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 配置可被 {@link org.springframework.web.reactive.resource.ResourceTransformer} 实例使用的 {@link ResourceUrlProvider}。
	 *
	 * @param resourceUrlProvider 要使用的资源 URL 提供程序
	 * @since 5.0.11
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}



	/**
	 * 添加一个资源处理程序，用于基于指定的 URL 路径模式提供静态资源服务。
	 * 对于匹配指定路径模式之一的每个传入请求，都会调用处理程序。
	 * <p>允许类似 {@code "/static/**"} 或 {@code "/css/{filename:\\w+\\.css}"} 的模式。
	 * 有关语法的更多详细信息，请参阅 {@link org.springframework.web.util.pattern.PathPattern}。
	 *
	 * @param patterns 要匹配的 URL 路径模式数组
	 * @return {@link ResourceHandlerRegistration} 实例，用于进一步配置注册的资源处理程序
	 */
	public ResourceHandlerRegistration addResourceHandler(String... patterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(this.resourceLoader, patterns);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 检查给定路径模式是否已经注册了资源处理程序。
	 *
	 * @param pathPattern 要检查的路径模式
	 * @return 如果已注册资源处理程序返回 true，否则返回 false
	 */
	public boolean hasMappingForPattern(String pathPattern) {
		// 遍历所有注册信息
		for (ResourceHandlerRegistration registration : this.registrations) {
			// 如果某个注册的路径模式列表包含指定的路径模式
			if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
				return true;
			}
		}

		// 如果没有匹配的路径模式，则返回false
		return false;
	}

	/**
	 * 设置资源处理的顺序，相对于 Spring 配置中配置的其他 {@code HandlerMapping}。
	 * 默认值为 {@code Integer.MAX_VALUE-1}。
	 *
	 * @param order 要设置的顺序值
	 * @return {@link ResourceHandlerRegistry} 实例
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回具有映射资源处理程序的处理程序映射；如果没有注册，则返回 {@code null}。
	 *
	 * @return {@code AbstractUrlHandlerMapping} 实例，或者在没有注册的情况下返回 {@code null}
	 */
	@Nullable
	protected AbstractUrlHandlerMapping getHandlerMapping() {
		// 如果没有注册信息，则返回null
		if (this.registrations.isEmpty()) {
			return null;
		}

		// 创建 URL 映射的 Map
		Map<String, WebHandler> urlMap = new LinkedHashMap<>();

		// 遍历所有注册信息
		for (ResourceHandlerRegistration registration : this.registrations) {
			// 获取请求处理程序
			ResourceWebHandler handler = getRequestHandler(registration);

			// 针对每个注册的路径模式
			for (String pathPattern : registration.getPathPatterns()) {
				// 将路径模式和处理程序放入 URL 映射中
				urlMap.put(pathPattern, handler);
			}
		}

		// 返回基于路径模式的简单 URL 处理程序映射
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}


	/**
	 * 获取 {@link ResourceWebHandler} 实例。
	 *
	 * @param registration 资源处理程序注册
	 * @return {@link ResourceWebHandler} 实例
	 */
	private ResourceWebHandler getRequestHandler(ResourceHandlerRegistration registration) {
		// 获取注册的请求处理程序
		ResourceWebHandler handler = registration.getRequestHandler();

		// 遍历处理程序的资源转换器
		for (ResourceTransformer transformer : handler.getResourceTransformers()) {
			// 如果转换器是 ResourceTransformerSupport 类型
			if (transformer instanceof ResourceTransformerSupport) {
				// 设置资源 URL 提供程序给转换器
				((ResourceTransformerSupport) transformer).setResourceUrlProvider(this.resourceUrlProvider);
			}
		}

		try {
			// 初始化资源请求处理程序
			handler.afterPropertiesSet();
		} catch (Throwable ex) {
			// 抛出 Bean 初始化异常，指示初始化资源请求处理程序失败
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}

		// 返回已配置完成的处理程序
		return handler;
	}

}
