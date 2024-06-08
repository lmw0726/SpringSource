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

package org.springframework.web.socket.sockjs.transport.handler;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSession;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * WebSocketTransportHandler 是基于 WebSocket 的 {@link TransportHandler}。
 * 使用 {@link SockJsWebSocketHandler} 和 {@link WebSocketServerSockJsSession} 来添加 SockJS 处理。
 *
 * <p>还实现了 {@link HandshakeHandler} 以支持在 SockJS URL "/websocket" 上的原始 WebSocket 通信。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketTransportHandler extends AbstractTransportHandler
		implements SockJsSessionFactory, HandshakeHandler, Lifecycle, ServletContextAware {
	/**
	 * 握手处理器
	 */
	private final HandshakeHandler handshakeHandler;

	/**
	 * 是否正在运行
	 */
	private volatile boolean running;


	public WebSocketTransportHandler(HandshakeHandler handshakeHandler) {
		Assert.notNull(handshakeHandler, "HandshakeHandler must not be null");
		this.handshakeHandler = handshakeHandler;
	}


	@Override
	public TransportType getTransportType() {
		return TransportType.WEBSOCKET;
	}

	public HandshakeHandler getHandshakeHandler() {
		return this.handshakeHandler;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		// 如果握手处理器实现了 Servlet上下文感知 接口
		if (this.handshakeHandler instanceof ServletContextAware) {
			// 将 servlet 上下文设置到握手处理器中
			((ServletContextAware) this.handshakeHandler).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		// 如果 WebSocket 处理器尚未运行
		if (!isRunning()) {
			// 设置 WebSocket 处理器为运行状态
			this.running = true;
			// 如果握手处理器实现了 Lifecycle 接口，则启动它
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).start();
			}
		}
	}

	@Override
	public void stop() {
		// 如果 WebSocket 处理器正在运行
		if (isRunning()) {
			// 设置 WebSocket 处理器为非运行状态
			this.running = false;
			// 如果握手处理器实现了 Lifecycle 接口，则停止它
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public boolean checkSessionType(SockJsSession session) {
		return (session instanceof WebSocketServerSockJsSession);
	}

	@Override
	public AbstractSockJsSession createSession(String id, WebSocketHandler handler, Map<String, Object> attrs) {
		return new WebSocketServerSockJsSession(id, getServiceConfig(), handler, attrs);
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
							  WebSocketHandler wsHandler, SockJsSession wsSession) throws SockJsException {

		// 将 WebSocket会话 转换为 WebSocket服务器SockJs会话
		WebSocketServerSockJsSession sockJsSession = (WebSocketServerSockJsSession) wsSession;

		try {
			// 创建 SockJsWebSocket处理器，用于处理 SockJS WebSocket 会话
			wsHandler = new SockJsWebSocketHandler(getServiceConfig(), wsHandler, sockJsSession);
			// 进行 WebSocket 握手
			this.handshakeHandler.doHandshake(request, response, wsHandler, sockJsSession.getAttributes());
		} catch (Exception ex) {
			// 如果发生异常，尝试使用 SockJS 传输错误关闭会话，并抛出异常
			sockJsSession.tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("WebSocket handshake failure", wsSession.getId(), ex);
		}
	}

	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
							   WebSocketHandler handler, Map<String, Object> attributes) throws HandshakeFailureException {

		return this.handshakeHandler.doHandshake(request, response, handler, attributes);
	}
}
