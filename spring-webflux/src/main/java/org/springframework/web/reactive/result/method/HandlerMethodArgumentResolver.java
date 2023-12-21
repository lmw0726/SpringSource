/*
 * Copyright 2002-2016 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于在当前HTTP请求上下文中解析方法参数的策略。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 此解析器是否支持给定的方法参数。
	 *
	 * @param parameter 方法参数
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 解析方法参数的值。
	 *
	 * @param parameter      方法参数
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange       当前交换
	 * @return 参数值的{@code Mono}，可能为空
	 */
	Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange);

}
