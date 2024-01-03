/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 表示过滤 ExchangeFunction 的函数。
 * <p>当 {@code Subscriber} 订阅 {@code WebClient} 返回的 {@code Publisher} 时执行过滤器。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFilterFunction {

	/**
	 * 将该过滤器应用于给定的请求和 ExchangeFunction。
	 * <p>给定的 {@linkplain ExchangeFunction} 表示链中的下一个实体，
	 * 可通过 {@linkplain ExchangeFunction#exchange(ClientRequest) 调用} 以继续交换，或者不调用以快捷链。
	 * <p><strong>注意：</strong> 当过滤器在调用 {@link ExchangeFunction#exchange} 后处理响应时，必须特别小心，
	 * 始终消耗其内容或以其他方式将其传播到下游以进行进一步处理，例如通过 {@link WebClient}。
	 * 请参阅参考文档以获取更多详情。
	 *
	 * @param request 当前请求
	 * @param next    链中的下一个 ExchangeFunction
	 * @return 过滤后的响应
	 */
	Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);

	/**
	 * 返回一个组合的过滤器函数，首先应用此过滤器，然后应用给定的 {@code "after"} 过滤器。
	 *
	 * @param afterFilter 此过滤器之后应用的过滤器
	 * @return 组合的过滤器
	 */
	default ExchangeFilterFunction andThen(ExchangeFilterFunction afterFilter) {
		Assert.notNull(afterFilter, "ExchangeFilterFunction must not be null");
		return (request, next) ->
				filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
	}

	/**
	 * 将此过滤器应用于给定的 {@linkplain ExchangeFunction}，生成过滤后的 exchange function。
	 *
	 * @param exchange 要过滤的 ExchangeFunction
	 * @return 过滤后的 exchange function
	 */
	default ExchangeFunction apply(ExchangeFunction exchange) {
		Assert.notNull(exchange, "ExchangeFunction must not be null");
		return request -> this.filter(request, exchange);
	}

	/**
	 * 将给定的请求处理函数适配为仅在 {@code ClientRequest} 上操作的过滤器函数。
	 *
	 * @param processor 请求处理器
	 * @return 结果的过滤器适配器
	 */
	static ExchangeFilterFunction ofRequestProcessor(Function<ClientRequest, Mono<ClientRequest>> processor) {
		Assert.notNull(processor, "ClientRequest Function must not be null");
		return (request, next) -> processor.apply(request).flatMap(next::exchange);
	}

	/**
	 * 将给定的响应处理函数适配为仅在 {@code ClientResponse} 上操作的过滤器函数。
	 *
	 * @param processor 响应处理器
	 * @return 结果的过滤器适配器
	 */
	static ExchangeFilterFunction ofResponseProcessor(Function<ClientResponse, Mono<ClientResponse>> processor) {
		Assert.notNull(processor, "ClientResponse Function must not be null");
		return (request, next) -> next.exchange(request).flatMap(processor);
	}

}
