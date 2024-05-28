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

package org.springframework.web.filter.reactive;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * 在 Reactor {@link Context} 中插入一个属性，使得当前 {@link ServerWebExchange}
 * 可以在属性名称 {@link #EXCHANGE_CONTEXT_ATTRIBUTE} 下使用。
 * 这对于在参与请求处理的组件中访问交换机而不需要显式传递它是有用的。
 *
 * <p>方便的方法 {@link #get(Context)} 用于查找交换机。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ServerWebExchangeContextFilter implements WebFilter {

	/**
	 * 保存交换机的上下文中的属性名称。
	 */
	public static final String EXCHANGE_CONTEXT_ATTRIBUTE =
			ServerWebExchangeContextFilter.class.getName() + ".EXCHANGE_CONTEXT";


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange)
				// 将当前交换对象作为上下文属性放入上下文，并将上下文写回
				.contextWrite(cxt -> cxt.put(EXCHANGE_CONTEXT_ATTRIBUTE, exchange));
	}


	/**
	 * 如果可用，从 Reactor Context 中访问 {@link ServerWebExchange}，
	 * 这在 {@link ServerWebExchangeContextFilter} 配置为使用时，
	 * 并且给定的上下文是从请求处理链获取时有效。
	 *
	 * @param context 要访问交换机的上下文
	 * @return 交换机
	 */
	public static Optional<ServerWebExchange> get(Context context) {
		return context.getOrEmpty(EXCHANGE_CONTEXT_ATTRIBUTE);
	}

}
