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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * 解析类型为 {@link WebSession} 的方法参数值。
 * 该类用于解析方法参数，继承自 HandlerMethodArgumentResolverSupport。
 *
 * @author Rossen Stoyanchev
 * @see ServerWebExchangeMethodArgumentResolver
 * @since 5.2
 */
public class WebSessionMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	// 我们需要这个解析器与实现了 SyncHandlerMethodArgumentResolver 的 ServerWebExchangeArgumentResolver 分开。

	/**
	 * 构造函数，使用 ReactiveAdapterRegistry 参数。
	 *
	 * @param adapterRegistry 适配器注册表
	 */
	public WebSessionMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}

	/**
	 * 检查方法参数是否支持。
	 *
	 * @param parameter 方法参数
	 * @return 是否支持参数类型
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//参数类型或者它的嵌套类型（不能是Reactive类型）是 WebSession
		return checkParameterType(parameter, WebSession.class::isAssignableFrom);
	}

	/**
	 * 解析方法参数。
	 *
	 * @param parameter 方法参数
	 * @param context   绑定上下文
	 * @param exchange  服务器Web交换对象
	 * @return 参数解析结果的Mono
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {
		//获取 WebSession
		Mono<WebSession> session = exchange.getSession();
		// 获取Reactive适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		// 如果适配器为空，则从 上面的WebSession 中获取；否则通过适配器转换WebSession
		return (adapter != null ? Mono.just(adapter.fromPublisher(session)) : Mono.from(session));
	}

}
