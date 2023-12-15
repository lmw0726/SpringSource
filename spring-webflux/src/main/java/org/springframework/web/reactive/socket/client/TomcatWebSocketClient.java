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

package org.springframework.web.reactive.socket.client;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;
import reactor.core.publisher.Sinks;

import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * 用于Java WebSocket API的 {@link WebSocketClient} 实现。
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketClient extends StandardWebSocketClient {

	/**
	 * 默认构造函数，使用默认的 WsWebSocketContainer。
	 */
	public TomcatWebSocketClient() {
		this(new WsWebSocketContainer());
	}

	/**
	 * 构造函数，接受一个 WebSocketContainer 实例。
	 *
	 * @param webSocketContainer WebSocketContainer 实例
	 */
	public TomcatWebSocketClient(WebSocketContainer webSocketContainer) {
		super(webSocketContainer);
	}

	/**
	 * 创建 Tomcat 特定的 WebSocket 会话。
	 *
	 * @param session        WebSocket 会话
	 * @param info           握手信息
	 * @param completionSink 完成信号 Sink
	 * @return 创建的 TomcatWebSocketSession 实例
	 */
	@Override
	protected StandardWebSocketSession createWebSocketSession(
			Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {

		return new TomcatWebSocketSession(session, info, bufferFactory(), completionSink);
	}

}
