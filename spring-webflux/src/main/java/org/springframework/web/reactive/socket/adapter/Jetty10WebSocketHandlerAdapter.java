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

package org.springframework.web.reactive.socket.adapter;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;


/**
 * 与 {@link org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter} 相同，
 * 只是不包括 {@code onWebSocketFrame} 方法，因为 {@link Frame} 参数已移动到Jetty 10中的其他包。
 * WebSocket处理适配器，用于将Jetty 10 WebSocket事件转发给Spring框架的WebSocket处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
@WebSocket
public class Jetty10WebSocketHandlerAdapter {

	/**
	 * WebSocket处理适配器的委托处理器。
	 */
	private final WebSocketHandler delegateHandler;

	/**
	 * 用于创建JettyWebSocketSession的会话工厂函数。
	 */
	private final Function<Session, JettyWebSocketSession> sessionFactory;

	/**
	 * 委托会话，可为空，用于保持JettyWebSocketSession的引用。
	 */
	@Nullable
	private JettyWebSocketSession delegateSession;


	/**
	 * 构造方法，接受WebSocket处理器和会话工厂函数。
	 *
	 * @param handler        WebSocket处理器
	 * @param sessionFactory 会话工厂函数
	 */
	public Jetty10WebSocketHandlerAdapter(WebSocketHandler handler,
										  Function<Session, JettyWebSocketSession> sessionFactory) {
		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(sessionFactory, "'sessionFactory' is required");
		this.delegateHandler = handler;
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 处理WebSocket连接建立事件。
	 *
	 * @param session Jetty WebSocket会话
	 */
	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		// 使用会话工厂函数创建委托会话，并将其赋值给delegateSession
		this.delegateSession = this.sessionFactory.apply(session);

		// 使用delegateHandler处理delegateSession，并进行一系列操作，包括添加检查点、订阅委托会话。
		this.delegateHandler.handle(this.delegateSession)
				.checkpoint(session.getUpgradeRequest().getRequestURI() + " [JettyWebSocketHandlerAdapter]")
				.subscribe(this.delegateSession);
	}

	/**
	 * 处理WebSocket文本消息事件。
	 *
	 * @param message 文本消息
	 */
	@OnWebSocketMessage
	public void onWebSocketText(String message) {
		if (this.delegateSession != null) {
			WebSocketMessage webSocketMessage = toMessage(Type.TEXT, message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}
	}

	/**
	 * 处理WebSocket二进制消息事件。
	 *
	 * @param message 二进制消息
	 * @param offset  偏移量
	 * @param length  长度
	 */
	@OnWebSocketMessage
	public void onWebSocketBinary(byte[] message, int offset, int length) {
		if (this.delegateSession != null) {
			// 如果委托会话不为空，使用ByteBuffer包装给定的消息数据，并调用toMessage方法创建WebSocketMessage对象
			ByteBuffer buffer = ByteBuffer.wrap(message, offset, length);
			WebSocketMessage webSocketMessage = toMessage(Type.BINARY, buffer);
			// 将创建的WebSocketMessage传递给委托会话的handleMessage方法处理
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}

	}

	/**
	 * 将WebSocket消息转换为Spring的WebSocket消息。
	 *
	 * @param type    消息类型
	 * @param message 消息内容
	 * @param <T>     消息类型泛型
	 * @return WebSocket消息
	 */
	private <T> WebSocketMessage toMessage(Type type, T message) {
		WebSocketSession session = this.delegateSession;
		Assert.state(session != null, "Cannot create message without a session");
		if (Type.TEXT.equals(type)) {
			// 如果消息类型为文本，将文本转换为UTF-8字节数组，并使用session的bufferFactory创建DataBuffer
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = session.bufferFactory().wrap(bytes);
			return new WebSocketMessage(Type.TEXT, buffer);
		} else if (Type.BINARY.equals(type)) {
			// 如果消息类型为二进制，直接使用session的bufferFactory将ByteBuffer包装为DataBuffer
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		} else if (Type.PONG.equals(type)) {
			// 如果消息类型为PONG，同样使用session的bufferFactory将ByteBuffer包装为DataBuffer
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.PONG, buffer);
		} else {
			// 如果消息类型未知，则抛出IllegalArgumentException异常
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}

	}

	/**
	 * 处理WebSocket连接关闭事件。
	 *
	 * @param statusCode 关闭状态码
	 * @param reason     关闭原因
	 */
	@OnWebSocketClose
	public void onWebSocketClose(int statusCode, String reason) {
		if (this.delegateSession != null) {
			this.delegateSession.handleClose(CloseStatus.create(statusCode, reason));
		}
	}

	/**
	 * 处理WebSocket错误事件。
	 *
	 * @param cause 错误原因
	 */
	@OnWebSocketError
	public void onWebSocketError(Throwable cause) {
		if (this.delegateSession != null) {
			this.delegateSession.handleError(cause);
		}
	}
}

