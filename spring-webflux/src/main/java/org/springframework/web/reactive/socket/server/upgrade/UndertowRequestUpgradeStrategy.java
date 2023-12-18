/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.socket.server.upgrade;

import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 用于 Undertow 的 RequestUpgradeStrategy。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class UndertowRequestUpgradeStrategy implements RequestUpgradeStrategy {

	/**
	 * 将请求升级到WebSocket会话并使用给定的处理程序处理它。
	 *
	 * @param exchange             当前交换信息
	 * @param handler              WebSocket会话的处理程序
	 * @param subProtocol          处理程序选择的子协议
	 * @param handshakeInfoFactory 用于为WebSocket会话创建HandshakeInfo的工厂
	 * @return 完成的{@code Mono<Void>}，表示WebSocket会话处理的结果。
	 * @since 5.1
	 */
	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
							  @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		// 获取底层的 HttpServerExchange 对象
		HttpServerExchange httpExchange = ServerHttpRequestDecorator.getNativeRequest(exchange.getRequest());

		// 设置子协议
		Set<String> protocols = (subProtocol != null ? Collections.singleton(subProtocol) : Collections.emptySet());
		// 创建 Handshake 对象和 HandshakeInfo、DataBufferFactory
		Hybi13Handshake handshake = new Hybi13Handshake(protocols, false);
		List<Handshake> handshakes = Collections.singletonList(handshake);
		// 获取握手信息和数据缓冲工厂
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

		// 触发 WebFlux preCommit 操作并执行升级
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					// 创建 DefaultCallback 对象，并处理 WebSocket 升级
					DefaultCallback callback = new DefaultCallback(
							handshakeInfo,
							ContextWebSocketHandler.decorate(handler, contextView),
							bufferFactory);
					try {
						// 创建 WebSocketProtocolHandshakeHandler 并处理 WebSocket 升级请求
						new WebSocketProtocolHandshakeHandler(handshakes, callback).handleRequest(httpExchange);
					} catch (Exception ex) {
						return Mono.error(ex);
					}
					return Mono.empty();
				}));
	}

	/**
	 * 内部类 DefaultCallback 用于处理 WebSocket 连接回调
	 */
	private static class DefaultCallback implements WebSocketConnectionCallback {


		/**
		 * 握手信息
		 */
		private final HandshakeInfo handshakeInfo;
		/**
		 * WebSocket 处理器
		 */
		private final WebSocketHandler handler;
		/**
		 * 数据缓冲工厂
		 */
		private final DataBufferFactory bufferFactory;

		public DefaultCallback(HandshakeInfo handshakeInfo, WebSocketHandler handler, DataBufferFactory bufferFactory) {
			this.handshakeInfo = handshakeInfo;
			this.handler = handler;
			this.bufferFactory = bufferFactory;
		}

		/**
		 * 当 WebSocket 连接建立时的回调方法。
		 * 创建 UndertowWebSocketSession 实例，并使用 UndertowWebSocketHandlerAdapter 处理连接。
		 * 启用 WebSocketChannel 的接收设置，并恢复接收状态。
		 * 处理 WebSocket 会话并进行订阅操作。
		 *
		 * @param exchange WebSocketHttpExchange 对象，表示 WebSocket 的 HTTP 交换信息
		 * @param channel  WebSocketChannel 对象，表示 WebSocket 的通道信息
		 */
		@Override
		public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
			// 创建 UndertowWebSocketSession，并设置 UndertowWebSocketHandlerAdapter 处理连接
			UndertowWebSocketSession session = createSession(channel);
			UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

			channel.getReceiveSetter().set(adapter);
			channel.resumeReceives();

			// 处理 WebSocket 会话并订阅
			this.handler.handle(session)
					.checkpoint(exchange.getRequestURI() + " [UndertowRequestUpgradeStrategy]")
					.subscribe(session);
		}

		/**
		 * 创建 UndertowWebSocketSession 实例。
		 *
		 * @param channel WebSocketChannel 对象，表示 WebSocket 的通道信息
		 * @return 创建的 UndertowWebSocketSession 实例
		 */
		private UndertowWebSocketSession createSession(WebSocketChannel channel) {
			return new UndertowWebSocketSession(channel, this.handshakeInfo, this.bufferFactory);
		}
	}

}
