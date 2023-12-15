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

package org.springframework.web.reactive.socket.adapter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;

import javax.websocket.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Java WebSocket API (JSR-356) 适配器，用于将事件委托给响应式 {@link WebSocketHandler} 及其会话。
 * 标准WebSocket处理器适配器，继承自Endpoint。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */

public class StandardWebSocketHandlerAdapter extends Endpoint {

	/**
	 * WebSocket处理器的委托对象。
	 */
	private final WebSocketHandler delegateHandler;

	/**
	 * 用于创建StandardWebSocketSession的工厂函数。
	 */
	private Function<Session, StandardWebSocketSession> sessionFactory;

	/**
	 * 委托的StandardWebSocketSession对象，可能为null。
	 */
	@Nullable
	private StandardWebSocketSession delegateSession;


	/**
	 * 构造一个StandardWebSocketHandlerAdapter对象。
	 *
	 * @param handler        WebSocket处理器
	 * @param sessionFactory 用于创建StandardWebSocketSession的工厂函数
	 */
	public StandardWebSocketHandlerAdapter(WebSocketHandler handler,
										   Function<Session, StandardWebSocketSession> sessionFactory) {
		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(sessionFactory, "'sessionFactory' is required");
		this.delegateHandler = handler;
		this.sessionFactory = sessionFactory;
	}

	/**
	 * WebSocket连接建立时触发的方法。
	 *
	 * @param session WebSocket连接的会话
	 * @param config  终端配置
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		this.delegateSession = this.sessionFactory.apply(session);
		Assert.state(this.delegateSession != null, "No delegate session");

		// 添加处理String类型消息的MessageHandler
		session.addMessageHandler(String.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		// 添加处理ByteBuffer类型消息的MessageHandler
		session.addMessageHandler(ByteBuffer.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		// 添加处理PongMessage类型消息的MessageHandler
		session.addMessageHandler(PongMessage.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});

		// 调用WebSocket处理器的handle方法，并订阅StandardWebSocketSession
		this.delegateHandler.handle(this.delegateSession)
				.checkpoint(session.getRequestURI() + " [StandardWebSocketHandlerAdapter]")
				.subscribe(this.delegateSession);
	}


	/**
	 * 将WebSocket消息转换为WebSocketMessage。
	 *
	 * @param message WebSocket消息
	 * @param <T>     消息类型
	 * @return WebSocketMessage对象
	 */
	private <T> WebSocketMessage toMessage(T message) {
		WebSocketSession session = this.delegateSession;
		Assert.state(session != null, "Cannot create message without a session");
		if (message instanceof String) {
			// 如果消息类型为String，则创建TEXT类型的WebSocketMessage
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, session.bufferFactory().wrap(bytes));
		} else if (message instanceof ByteBuffer) {
			// 如果消息类型为ByteBuffer，则创建BINARY类型的WebSocketMessage
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		} else if (message instanceof PongMessage) {
			// 如果消息类型为PongMessage，则创建PONG类型的WebSocketMessage
			DataBuffer buffer = session.bufferFactory().wrap(((PongMessage) message).getApplicationData());
			return new WebSocketMessage(Type.PONG, buffer);
		} else {
			// 如果消息类型未知，则抛出IllegalArgumentException异常
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

	/**
	 * WebSocket连接关闭时触发的方法。
	 *
	 * @param session WebSocket连接的会话
	 * @param reason  关闭原因
	 */
	@Override
	public void onClose(Session session, CloseReason reason) {
		if (this.delegateSession != null) {
			int code = reason.getCloseCode().getCode();
			// 处理WebSocket连接关闭
			this.delegateSession.handleClose(CloseStatus.create(code, reason.getReasonPhrase()));
		}
	}

	/**
	 * WebSocket连接发生错误时触发的方法。
	 *
	 * @param session   WebSocket连接的会话
	 * @param exception 异常信息
	 */
	@Override
	public void onError(Session session, Throwable exception) {
		if (this.delegateSession != null) {
			// 处理WebSocket连接发生错误
			this.delegateSession.handleError(exception);
		}
	}

}
