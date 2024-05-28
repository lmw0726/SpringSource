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

package org.springframework.web.filter.reactive;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * 从 "Forwarded" 和 "X-Forwarded-*" 标头中提取值，以覆盖请求 URI（即 {@link ServerHttpRequest#getURI()}），
 * 使其反映客户端发起的协议和地址。
 *
 * <p>或者，如果 {@link #setRemoveOnly removeOnly} 设置为 "true"，则只会删除 "Forwarded" 和 "X-Forwarded-*" 标头，而不会使用它们。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 5.0
 * @deprecated 自 5.1 起，此过滤器已弃用，建议改用 {@link ForwardedHeaderTransformer}，
 * 可将其声明为名为 "forwardedHeaderTransformer" 的 bean，
 * 或在 {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder WebHttpHandlerBuilder} 中显式注册。
 */
@Deprecated
public class ForwardedHeaderFilter extends ForwardedHeaderTransformer implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (hasForwardedHeaders(request)) {
			// 如果请求中包含转发头部信息，则将转发头部信息应用到交换对象中
			exchange = exchange.mutate().request(apply(request)).build();
		}
		// 将请求传递给下一个过滤器链处理
		return chain.filter(exchange);
	}

}
