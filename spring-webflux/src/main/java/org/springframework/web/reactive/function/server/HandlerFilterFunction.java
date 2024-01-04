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

package org.springframework.web.reactive.function.server;

import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.support.ServerRequestWrapper;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 表示过滤{@linkplain HandlerFunction 处理函数}的函数。
 *
 * @param <T> 要过滤的{@linkplain HandlerFunction 处理函数}类型
 * @param <R> 函数的响应类型
 * @author Arjen Poutsma
 * @see RouterFunction#filter(HandlerFilterFunction)
 * @since 5.0
 */
@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {

	/**
	 * 将此过滤器应用于给定的处理函数。给定的{@linkplain HandlerFunction 处理函数}表示链中的下一个实体，
	 * 可以{@linkplain HandlerFunction#handle(ServerRequest) 调用}以继续到此实体，
	 * 或者不调用以阻止链。
	 *
	 * @param request 请求
	 * @param next    链中的下一个处理函数或过滤器函数
	 * @return 过滤后的响应
	 * @see ServerRequestWrapper
	 */
	Mono<R> filter(ServerRequest request, HandlerFunction<T> next);

	/**
	 * 返回一个组合过滤器函数，首先应用此过滤器，然后应用{@code after}过滤器。
	 *
	 * @param after 此过滤器之后要应用的过滤器
	 * @return 首先应用此函数然后应用{@code after}函数的组合过滤器
	 */
	default HandlerFilterFunction<T, R> andThen(HandlerFilterFunction<T, T> after) {
		Assert.notNull(after, "HandlerFilterFunction must not be null");
		return (request, next) -> {
			HandlerFunction<T> nextHandler = handlerRequest -> after.filter(handlerRequest, next);
			return filter(request, nextHandler);
		};
	}

	/**
	 * 将此过滤器应用于给定的处理函数，生成一个过滤后的处理函数。
	 *
	 * @param handler 要过滤的处理函数
	 * @return 过滤后的处理函数
	 */
	default HandlerFunction<R> apply(HandlerFunction<T> handler) {
		Assert.notNull(handler, "HandlerFunction must not be null");
		return request -> this.filter(request, handler);
	}

	/**
	 * 将给定的请求处理函数适配为仅对{@code ServerRequest}操作的过滤器函数。
	 *
	 * @param requestProcessor 请求处理函数
	 * @return 请求处理函数的过滤器适配
	 */
	static HandlerFilterFunction<?, ?> ofRequestProcessor(
			Function<ServerRequest, Mono<ServerRequest>> requestProcessor) {

		Assert.notNull(requestProcessor, "Function must not be null");
		return (request, next) -> requestProcessor.apply(request).flatMap(next::handle);
	}

	/**
	 * 将给定的响应处理函数适配为仅对{@code ServerResponse}操作的过滤器函数。
	 *
	 * @param responseProcessor 响应处理函数
	 * @return 响应处理函数的过滤器适配
	 */
	static <T extends ServerResponse, R extends ServerResponse> HandlerFilterFunction<T, R> ofResponseProcessor(
			Function<T, Mono<R>> responseProcessor) {

		Assert.notNull(responseProcessor, "Function must not be null");
		return (request, next) -> next.handle(request).flatMap(responseProcessor);
	}

}
