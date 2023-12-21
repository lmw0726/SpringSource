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

package org.springframework.web.reactive.result.method;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过委托给一系列注册的{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolver}解析方法参数。
 * 先前解析的方法参数被缓存，以加快查找速度。
 *
 * @author Rossen Stoyanchev
 * @since 5.1.3
 */
class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {

	/**
	 * 用于存储参数解析器的列表
	 */
	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	/**
	 * 用于缓存方法参数解析器的Map，以提高解析速度
	 */
	private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<>(256);


	/**
	 * 添加给定的{@link HandlerMethodArgumentResolver}。
	 */
	public HandlerMethodArgumentResolverComposite addResolver(HandlerMethodArgumentResolver resolver) {
		this.argumentResolvers.add(resolver);
		return this;
	}

	/**
	 * 添加给定的{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(@Nullable HandlerMethodArgumentResolver... resolvers) {
		if (resolvers != null) {
			Collections.addAll(this.argumentResolvers, resolvers);
		}
		return this;
	}

	/**
	 * 添加给定的{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
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
	 * 清除配置的解析器列表和解析器缓存。
	 */
	public void clear() {
		this.argumentResolvers.clear();
		this.argumentResolverCache.clear();
	}


	/**
	 * 给定的 {@linkplain MethodParameter 方法参数}是否被任何注册的{@link HandlerMethodArgumentResolver}支持。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * 遍历注册的{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}并调用支持它的解析器。
	 *
	 * @throws IllegalStateException 如果未找到适当的{@link HandlerMethodArgumentResolver}。
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// 获取方法参数解析器
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		if (resolver == null) {
			// 如果解析器为空，抛出不支持的参数类型异常
			throw new IllegalArgumentException("Unsupported parameter type [" +
					parameter.getParameterType().getName() + "]. supportsParameter should be called first.");
		}
		// 使用解析器解析参数
		return resolver.resolveArgument(parameter, bindingContext, exchange);
	}

	/**
	 * 查找支持给定方法参数的注册的{@link HandlerMethodArgumentResolver}。
	 */
	@Nullable
	private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		// 从缓存中获取参数对应的方法参数解析器
		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		if (result == null) {
			// 如果缓存中不存在对应的解析器，则遍历解析器列表，找到支持该参数的解析器
			for (HandlerMethodArgumentResolver methodArgumentResolver : this.argumentResolvers) {
				if (methodArgumentResolver.supportsParameter(parameter)) {
					// 找到支持的解析器后，放入缓存中
					result = methodArgumentResolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

}
