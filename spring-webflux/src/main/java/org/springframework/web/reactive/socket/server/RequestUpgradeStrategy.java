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

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * 用于根据底层网络运行时将HTTP请求升级到WebSocket会话的策略。
 *
 * <p>通常，每个{@link ServerHttpRequest}和{@link ServerHttpResponse}类型都有一个这样的策略，
 * 除了在Servlet容器的情况下，其中标准的Java WebSocket API JSR-356未定义升级请求的方式，
 * 因此每个Servlet容器都需要一个自定义策略。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * 将请求升级到WebSocket会话并使用给定的处理程序处理它。
	 *
	 * @param exchange         当前交换信息
	 * @param webSocketHandler WebSocket会话的处理程序
	 * @param subProtocol      处理程序选择的子协议
	 * @return 完成的{@code Mono<Void>}，表示WebSocket会话处理的结果。
	 * @deprecated 自5.1起弃用，建议使用{@link #upgrade(ServerWebExchange, WebSocketHandler, String, Supplier)}
	 */
	@Deprecated
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
							   @Nullable String subProtocol) {

		return Mono.error(new UnsupportedOperationException());
	}

	/**
	 * 将请求升级到WebSocket会话并使用给定的处理程序处理它。
	 *
	 * @param exchange             当前交换信息
	 * @param webSocketHandler     WebSocket会话的处理程序
	 * @param subProtocol          处理程序选择的子协议
	 * @param handshakeInfoFactory 用于为WebSocket会话创建HandshakeInfo的工厂
	 * @return 完成的{@code Mono<Void>}，表示WebSocket会话处理的结果。
	 * @since 5.1
	 */
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
							   @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		return upgrade(exchange, webSocketHandler, subProtocol);
	}

}
