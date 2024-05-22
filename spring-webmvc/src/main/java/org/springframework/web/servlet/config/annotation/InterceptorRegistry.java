/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帮助配置映射拦截器列表。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistry {

	/**
	 * 拦截器注册列表
	 */
	private final List<InterceptorRegistration> registrations = new ArrayList<>();


	/**
	 * 添加提供的 {@link HandlerInterceptor}。
	 *
	 * @param interceptor 要添加的拦截器
	 * @return 一个 {@link InterceptorRegistration}，允许您进一步配置已注册的拦截器，例如添加应应用的 URL 模式。
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 添加提供的 {@link WebRequestInterceptor}。
	 *
	 * @param interceptor 要添加的拦截器
	 * @return 一个 {@link InterceptorRegistration}，允许您进一步配置已注册的拦截器，例如添加应应用的 URL 模式。
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		// 创建 Web 请求处理器拦截器适配器
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		// 创建拦截器注册
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		// 将注册添加到注册列表中
		this.registrations.add(registration);
		// 返回拦截器注册
		return registration;
	}

	/**
	 * 返回所有已注册的拦截器。
	 */
	protected List<Object> getInterceptors() {
		return this.registrations.stream()
				.sorted(INTERCEPTOR_ORDER_COMPARATOR)
				.map(InterceptorRegistration::getInterceptor)
				.collect(Collectors.toList());
	}


	private static final Comparator<Object> INTERCEPTOR_ORDER_COMPARATOR =
			// 使用 OrderComparator 实例进行排序比较
			OrderComparator.INSTANCE.withSourceProvider(object -> {
				// 如果对象是 InterceptorRegistration 的实例
				if (object instanceof InterceptorRegistration) {
					// 返回 Ordered 实例，通过 InterceptorRegistration 的 getOrder 方法获取顺序
					return (Ordered) ((InterceptorRegistration) object)::getOrder;
				}
				// 否则返回空
				return null;
			});

}
