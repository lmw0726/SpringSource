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

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * 解析类型为 {@link java.security.Principal} 的方法参数值。
 *
 * @author Rossen Stoyanchev
 * @see ServerWebExchangeMethodArgumentResolver
 * @since 5.2
 */
public class PrincipalMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	public PrincipalMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	/**
	 * 判断是否支持解析指定类型的方法参数。
	 *
	 * @param parameter 要判断的方法参数
	 * @return 如果是 {@link Principal} 类型的参数则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果类型是 Principal，则返回 true
		return checkParameterType(parameter, Principal.class::isAssignableFrom);
	}

	/**
	 * 解析参数并返回处理后的结果。
	 *
	 * @param parameter        方法参数
	 * @param context          绑定上下文
	 * @param exchange         当前的服务器Web交换
	 * @return 解析后的参数结果
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		// 获取当前的 Principal
		Mono<Principal> principal = exchange.getPrincipal();

		// 获取参数类型对应的适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());

		// 如果存在适配器，则使用适配器将 Publisher 转换为对应类型的 Mono；否则直接返回当前的 Principal
		return (adapter != null ? Mono.just(adapter.fromPublisher(principal)) : Mono.from(principal));
	}

}
