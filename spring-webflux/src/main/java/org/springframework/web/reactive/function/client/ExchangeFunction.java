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

import reactor.core.publisher.Mono;

/**
 * 表示一个功能，用于交换{@linkplain ClientRequest 请求}以获取（延迟的）{@linkplain ClientResponse 响应}。可用作{@link WebClient}的替代方案。
 * <p>
 * 例如：
 * <pre class="code">
 * ExchangeFunction exchangeFunction =
 *         ExchangeFunctions.create(new ReactorClientHttpConnector());
 *
 * URI url = URI.create("https://example.com/resource");
 * ClientRequest request = ClientRequest.create(HttpMethod.GET, url).build();
 *
 * Mono&lt;String&gt; bodyMono = exchangeFunction
 *     .exchange(request)
 *     .flatMap(response -&gt; response.bodyToMono(String.class));
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFunction {

	/**
	 * 用给定的请求交换为{@link ClientResponse}的延迟响应。
	 *
	 * <p><strong>注意：</strong>当从处理响应的{@link ExchangeFilterFunction}中调用此方法时，
	 * 必须格外小心地始终消耗其内容，或以其他方式将其向下游传播以进行进一步处理，例如通过{@link WebClient}。
	 * 请参阅参考文档以了解更多详情。
	 *
	 * @param request 要交换的请求
	 * @return 延迟的响应
	 */
	Mono<ClientResponse> exchange(ClientRequest request);

	/**
	 * 使用给定的{@code ExchangeFilterFunction}对交换功能进行过滤，从而得到一个经过过滤的{@code ExchangeFunction}。
	 *
	 * @param filter 要应用于此交换的过滤器
	 * @return 经过过滤的交换
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	default ExchangeFunction filter(ExchangeFilterFunction filter) {
		return filter.apply(this);
	}

}
