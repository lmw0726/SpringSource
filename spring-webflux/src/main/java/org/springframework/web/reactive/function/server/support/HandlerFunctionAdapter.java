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

package org.springframework.web.reactive.function.server.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * {@code HandlerAdapter} 实现，支持 {@link HandlerFunction HandlerFunctions}。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class HandlerFunctionAdapter implements HandlerAdapter {
	/**
	 * 方法参数
	 */
	private static final MethodParameter HANDLER_FUNCTION_RETURN_TYPE;

	static {
		// 在静态初始化块中尝试获取 HandlerFunction 接口中的 handle 方法
		try {
			Method method = HandlerFunction.class.getMethod("handle", ServerRequest.class);
			// 将 handle 方法信息存储在 HANDLER_FUNCTION_RETURN_TYPE 中
			HANDLER_FUNCTION_RETURN_TYPE = new MethodParameter(method, -1);
		} catch (NoSuchMethodException ex) {
			// 如果未找到方法，则抛出 IllegalStateException 异常
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerFunction;
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		// 将 handler 转换为 HandlerFunction 类型
		HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;

		// 从交换器中获取请求对象
		ServerRequest request = exchange.getRequiredAttribute(RouterFunctions.REQUEST_ATTRIBUTE);

		// 使用处理函数处理请求，并将处理的响应映射为 HandlerResult 对象
		return handlerFunction.handle(request)
				.map(response -> new HandlerResult(handlerFunction, response, HANDLER_FUNCTION_RETURN_TYPE));
	}
}
