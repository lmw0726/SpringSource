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

package org.springframework.web.reactive.socket.server.support;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.BiPredicate;

/**
 * 用于与
 * {@link org.springframework.web.reactive.handler.AbstractUrlHandlerMapping#setHandlerPredicate}
 * 一起使用的断言，以确保只有 WebSocket 握手请求与类型为
 * {@link WebSocketHandler} 的处理程序匹配。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.5
 */
public class WebSocketUpgradeHandlerPredicate implements BiPredicate<Object, ServerWebExchange> {

	@Override
	public boolean test(Object handler, ServerWebExchange exchange) {
		if (handler instanceof WebSocketHandler) {
			// 获取请求方法和 Upgrade 头部信息
			String method = exchange.getRequest().getMethodValue();
			String header = exchange.getRequest().getHeaders().getUpgrade();
			// 检查请求方法是否为 GET 且 Upgrade 头部信息为 websocket
			return (method.equals("GET") && header != null && header.equalsIgnoreCase("websocket"));
		}
		// 如果处理程序不是 WebSocketHandler 类型，则始终返回 true
		return true;
	}
}

