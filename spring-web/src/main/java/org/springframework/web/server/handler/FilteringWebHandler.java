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

package org.springframework.web.server.handler;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * {@link WebHandlerDecorator} 调用委托 {@link WebHandler} 前，调用一系列 {@link WebFilter WebFilters} 的装饰器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FilteringWebHandler extends WebHandlerDecorator {
	/**
	 * 默认的Web过滤器链
	 */
	private final DefaultWebFilterChain chain;


	/**
	 * 构造方法。
	 * @param handler 要处理的 WebHandler
	 * @param filters 过滤器链
	 */
	public FilteringWebHandler(WebHandler handler, List<WebFilter> filters) {
		super(handler);
		this.chain = new DefaultWebFilterChain(handler, filters);
	}


	/**
	 * 返回配置的过滤器的只读列表。
	 */
	public List<WebFilter> getFilters() {
		return this.chain.getFilters();
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return this.chain.filter(exchange);
	}

}
