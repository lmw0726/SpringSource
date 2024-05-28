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

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;


/**
 * {@link WebFilter} 处理 CORS 预检请求，并拦截 CORS 简单和实际请求，
 * 借助 {@link CorsProcessor} 实现（默认为 {@link DefaultCorsProcessor}），
 * 以添加相关的 CORS 响应头（如 {@code Access-Control-Allow-Origin}），
 * 使用提供的 {@link CorsConfigurationSource}（例如 {@link UrlBasedCorsConfigurationSource} 实例）。
 *
 * <p>这是 Spring WebFlux 的另一种方式进行 CORS 配置，对于使用功能 API 的应用程序非常有用。
 *
 * @author Sebastien Deleuze
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @since 5.0
 */
public class CorsWebFilter implements WebFilter {
	/**
	 * 跨域配置源
	 */
	private final CorsConfigurationSource configSource;

	/**
	 * 跨域配置处理器
	 */
	private final CorsProcessor processor;


	/**
	 * 构造函数接受一个 {@link CorsConfigurationSource}，用于由过滤器查找每个传入请求使用的 {@link CorsConfiguration}。
	 *
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource) {
		this(configSource, new DefaultCorsProcessor());
	}

	/**
	 * 构造函数接受一个 {@link CorsConfigurationSource}，用于由过滤器查找每个传入请求使用的 {@link CorsConfiguration}，
	 * 以及一个自定义的 {@link CorsProcessor}，用于应用匹配的 {@link CorsConfiguration}。
	 *
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource, CorsProcessor processor) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.configSource = configSource;
		this.processor = processor;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		// 获取当前交换对象的跨域配置
		CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(exchange);
		// 处理跨域请求，并返回是否有效的布尔值
		boolean isValid = this.processor.process(corsConfiguration, exchange);

		if (!isValid || CorsUtils.isPreFlightRequest(request)) {
			// 如果跨域请求无效，或者是预检请求（Preflight Request），则直接返回空Mono
			return Mono.empty();
		}

		// 否则，将请求传递给下一个过滤器链处理
		return chain.filter(exchange);
	}

}
