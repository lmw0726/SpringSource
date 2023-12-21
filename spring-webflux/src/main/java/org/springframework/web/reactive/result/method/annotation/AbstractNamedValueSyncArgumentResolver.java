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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link AbstractNamedValueArgumentResolver} 的扩展，用于具有同步但非阻塞的命名值解析器。
 * 子类实现同步的 {@link #resolveNamedValue} 方法，默认情况下由异步的 {@link #resolveName} 方法代理。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractNamedValueSyncArgumentResolver extends AbstractNamedValueArgumentResolver
		implements SyncHandlerMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link AbstractNamedValueSyncArgumentResolver}。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                如果不希望默认值具有表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	protected AbstractNamedValueSyncArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// 将默认实现从 SyncHandlerMethodArgumentResolver 中翻转：
		// 不再委托给 (sync) resolveArgumentValue，
		// 而是调用 (async) super.resolveArgument，与非阻塞解析器共享；
		// 下面的实际解析仍然是同步的...

		return super.resolveArgument(parameter, bindingContext, exchange);
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		// 这不会阻塞，因为下面的 resolveName 不会阻塞
		return resolveArgument(parameter, context, exchange).block();
	}

	@Override
	protected final Mono<Object> resolveName(String name, MethodParameter param, ServerWebExchange exchange) {
		return Mono.justOrEmpty(resolveNamedValue(name, param, exchange));
	}

	/**
	 * 实际同步解析值的方法。
	 */
	@Nullable
	protected abstract Object resolveNamedValue(String name, MethodParameter param, ServerWebExchange exchange);

}
