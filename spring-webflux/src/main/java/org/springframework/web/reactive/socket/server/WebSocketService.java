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

package org.springframework.web.reactive.socket.server;

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于委派WebSocket相关的HTTP请求的服务。
 *
 * <p>对于WebSocket端点，这意味着处理初始的WebSocket HTTP握手请求。
 * 对于SockJS端点，它可以意味着处理SockJS协议中定义的所有HTTP请求。
 *
 * @author Rossen Stoyanchev
 * @see org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
 * @since 5.0
 */
public interface WebSocketService {

	/**
	 * 使用给定的{@link WebSocketHandler}处理请求。
	 *
	 * @param exchange         当前交换信息
	 * @param webSocketHandler WebSocket会话的处理程序
	 * @return 一个{@code Mono<Void>}，在WebSocket会话的应用程序处理完成时完成。
	 */
	Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler);

}
