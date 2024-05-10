/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过委托给注册的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolver} 列表来解析方法参数。
 * 先前已解析的方法参数被缓存以加快查找速度。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {

	/**
	 * 参数解析器列表
	 */
	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	/**
	 * 方法参数 - 参数解析器映射
	 */
	private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<>(256);


	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver}。
	 */
	public HandlerMethodArgumentResolverComposite addResolver(HandlerMethodArgumentResolver resolver) {
		this.argumentResolvers.add(resolver);
		return this;
	}

	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 *
	 * @since 4.3
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(
			@Nullable HandlerMethodArgumentResolver... resolvers) {

		if (resolvers != null) {
			Collections.addAll(this.argumentResolvers, resolvers);
		}
		return this;
	}

	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(
			@Nullable List<? extends HandlerMethodArgumentResolver> resolvers) {

		if (resolvers != null) {
			this.argumentResolvers.addAll(resolvers);
		}
		return this;
	}

	/**
	 * 返回包含的解析器的只读列表，或空列表。
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.argumentResolvers);
	}

	/**
	 * 清除已配置的解析器列表和解析器缓存。
	 *
	 * @since 4.3
	 */
	public void clear() {
		this.argumentResolvers.clear();
		this.argumentResolverCache.clear();
	}


	/**
	 * 检查任何注册的 {@link HandlerMethodArgumentResolver} 是否支持给定的 {@linkplain MethodParameter 方法参数}。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * 遍历注册的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 并调用支持它的解析器。
	 *
	 * @throws IllegalArgumentException 如果找不到适合的参数解析器
	 */
	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		// 获取方法参数上对应的参数解析器
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		if (resolver == null) {
			// 如果参数解析器不为空，则抛出异常
			throw new IllegalArgumentException("Unsupported parameter type [" +
					parameter.getParameterType().getName() + "]. supportsParameter should be called first.");
		}
		// 解析参数值
		return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
	}

	/**
	 * 查找注册的 {@link HandlerMethodArgumentResolver}，支持给定的方法参数。
	 */
	@Nullable
	private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		// 从缓存中获取对应的解析器
		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		// 如果缓存中不存在对应的解析器
		if (result == null) {
			// 遍历所有的参数解析器
			for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
				// 如果当前解析器支持解析该参数
				if (resolver.supportsParameter(parameter)) {
					// 将当前解析器作为结果，并将其加入缓存中
					result = resolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

}
