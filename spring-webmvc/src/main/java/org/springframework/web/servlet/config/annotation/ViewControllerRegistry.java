/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 辅助注册简单自动化控制器，预配置了状态码和/或视图。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistry {
	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 视图控制器注册器列表
	 */
	private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

	/**
	 * 重定向视图控制器注册器列表
	 */
	private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

	/**
	 * 排序值
	 */
	private int order = 1;


	/**
	 * 使用 {@link ApplicationContext} 的类构造函数。
	 *
	 * @since 4.3.12
	 */
	public ViewControllerRegistry(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * 将 URL 路径或模式映射到视图控制器，以使用配置的状态码和视图渲染响应。
	 * <p>支持模式，例如 {@code "/admin/**"} 或 {@code "/articles/{articlename:\\w+}"}。
	 * 对于模式语法，请参阅 {@link PathPattern}，当启用 {@link PathMatchConfigurer#setPatternParser} 时，将解析模式，
	 * 否则使用 {@link AntPathMatcher}。语法基本相同，{@link PathPattern} 更适用于 Web 用途且更高效。
	 * <p><strong>注意：</strong>如果 {@code @RequestMapping} 方法映射到任何 HTTP 方法的 URL，则视图控制器无法处理相同的 URL。
	 * 因此，建议避免在注释控制器和视图控制器之间拆分 URL 处理。
	 */
	public ViewControllerRegistration addViewController(String urlPathOrPattern) {
		// 使用提供的 URL 路径或模式，创建视图控制器注册对象
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPathOrPattern);
		// 设置应用程序上下文
		registration.setApplicationContext(this.applicationContext);
		// 将注册对象添加到注册列表中
		this.registrations.add(registration);
		// 返回注册对象
		return registration;
	}

	/**
	 * 将视图控制器映射到给定的 URL 路径或模式，以重定向到另一个 URL。
	 * <p>对于模式语法，请参阅 {@link PathPattern}，当启用 {@link PathMatchConfigurer#setPatternParser} 时，将解析模式，
	 * 否则使用 {@link AntPathMatcher}。语法基本相同，{@link PathPattern} 更适用于 Web 用途且更高效。
	 * <p>默认情况下，重定向 URL 应相对于当前 ServletContext，即相对于 Web 应用根目录。
	 *
	 * @since 4.1
	 */
	public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
		// 使用提供的 URL 路径和重定向 URL，创建重定向视图控制器注册对象
		RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
		// 设置应用程序上下文
		registration.setApplicationContext(this.applicationContext);
		// 将注册对象添加到重定向注册列表中
		this.redirectRegistrations.add(registration);
		// 返回注册对象
		return registration;
	}

	/**
	 * 将简单控制器映射到给定的 URL 路径（或模式），以设置响应状态为给定代码，而无需渲染主体。
	 * <p>对于模式语法，请参阅 {@link PathPattern}，当启用 {@link PathMatchConfigurer#setPatternParser} 时，将解析模式，
	 * 否则使用 {@link AntPathMatcher}。语法基本相同，{@link PathPattern} 更适用于 Web 用途且更高效。
	 *
	 * @since 4.1
	 */
	public void addStatusController(String urlPath, HttpStatus statusCode) {
		// 使用提供的 URL 路径，创建一个视图控制器注册对象
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		// 设置应用程序上下文
		registration.setApplicationContext(this.applicationContext);
		// 设置状态码
		registration.setStatusCode(statusCode);
		// 设置仅设置状态的视图控制器
		registration.getViewController().setStatusOnly(true);
		// 将注册对象添加到注册列表中
		this.registrations.add(registration);
	}

	/**
	 * 指定要在相对于 Spring MVC 中配置的其他处理程序映射之前使用的顺序。
	 * <p>默认情况下，设置为 1，即在注释控制器之后，注释控制器的顺序为 0。
	 */
	public void setOrder(int order) {
		this.order = order;
	}


	/**
	 * 返回包含已注册的视图控制器映射的 {@code HandlerMapping}，如果没有注册则返回 {@code null}。
	 *
	 * @since 4.3.12
	 */
	@Nullable
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		// 如果注册列表和重定向注册列表均为空，则返回 null
		if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
			return null;
		}

		// 创建一个链接映射的有序映射
		Map<String, Object> urlMap = new LinkedHashMap<>();
		// 将 注册列表中的视图控制器 添加到链接映射中
		for (ViewControllerRegistration registration : this.registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		// 将 重定向注册列表中的视图控制器 添加到链接映射中
		for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}

		// 返回一个简单 URL 处理映射对象，使用链接映射和指定的顺序
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

}
