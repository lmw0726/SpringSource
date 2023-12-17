/*
 * Copyright 2002-2020 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerAdapter} 实现，允许
 * {@link org.springframework.web.reactive.DispatcherHandler} 支持
 * 类型为 {@link WebSocketHandler} 的处理程序，并通过
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping}
 * 将这些处理程序映射到 URL 模式。
 *
 * <p>通过委托给 {@link WebSocketService} 处理请求，默认为 {@link HandshakeWebSocketService}，
 * 该服务检查 WebSocket 握手请求参数，升级到 WebSocket 交互，并使用 {@link WebSocketHandler} 处理它。
 *
 * <p>从 5.3 版本开始，WebFlux 的 Java 配置（通过 {@code @EnableWebFlux} 导入）包括对此适配器的声明，
 * 因此它不再需要出现在应用程序配置中。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketHandlerAdapter implements HandlerAdapter, Ordered {
	/**
	 * WebSocket服务
	 */
	private final WebSocketService webSocketService;
	/**
	 * 定义该Bean的排序值为2
	 */
	private int order = 2;

	/**
	 * 默认构造函数，创建并使用 {@link HandshakeWebSocketService}。
	 */
	public WebSocketHandlerAdapter() {
		this(new HandshakeWebSocketService());
	}

	/**
	 * 另一种构造函数，使用指定的 {@link WebSocketService}。
	 */
	public WebSocketHandlerAdapter(WebSocketService webSocketService) {
		Assert.notNull(webSocketService, "'webSocketService' is required");
		this.webSocketService = webSocketService;
	}

	/**
	 * 设置此适配器的排序值。
	 * <p>默认为 2。
	 *
	 * @param order 要设置的值
	 * @since 5.3
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * 返回此实例的配置顺序 {@link #setOrder(int)}。
	 *
	 * @since 5.3
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 返回配置的 {@code WebSocketService} 以处理请求。
	 *
	 * @return WebSocket服务
	 */
	public WebSocketService getWebSocketService() {
		return this.webSocketService;
	}

	/**
	 * 检查处理程序是否为 WebSocketHandler 类型
	 *
	 * @param handler 要检查的处理程序对象
	 * @return 如果处理程序是 WebSocketHandler 类型，则返回 true；否则返回 false
	 */
	@Override
	public boolean supports(Object handler) {
		//如果仅支持WebSocketHandler的实现类
		return WebSocketHandler.class.isAssignableFrom(handler.getClass());
	}

	/**
	 * 处理 WebSocket 请求
	 *
	 * @param exchange WebSocket 服务器交换对象
	 * @param handler  处理程序对象
	 * @return 返回一个 Mono，表示处理请求的结果
	 */
	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebSocketHandler webSocketHandler = (WebSocketHandler) handler;
		return getWebSocketService().handleRequest(exchange, webSocketHandler).then(Mono.empty());
	}

}
