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

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * 处理预检请求的 WebFilter，通过 {@link PreFlightRequestHandler} 处理，并绕过其余的链。
 *
 * <p>WebFlux 应用程序可以简单地注入 PreFlightRequestHandler 并使用它来创建此 WebFilter 的实例，
 * 因为 {@code @EnableWebFlux} 声明 {@code DispatcherHandler} 为一个 bean，它就是一个 PreFlightRequestHandler。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.7
 */
public class PreFlightRequestWebFilter implements WebFilter {
	/**
	 * 预检请求处理器
	 */
	private final PreFlightRequestHandler handler;


	/**
	 * 创建一个实例，将委托给给定的处理程序。
	 */
	public PreFlightRequestWebFilter(PreFlightRequestHandler handler) {
		Assert.notNull(handler, "PreFlightRequestHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return (CorsUtils.isPreFlightRequest(exchange.getRequest()) ?
				// 如果是预检请求，则调用处理预检请求的方法
				this.handler.handlePreFlight(exchange) :
				// 否则，将请求传递给下一个过滤器链处理
				chain.filter(exchange));
	}

}
