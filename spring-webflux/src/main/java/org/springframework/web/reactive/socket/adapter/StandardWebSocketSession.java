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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} adapter for a standard Java (JSR 356)
 * {@link javax.websocket.Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StandardWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	/**
	 * 构造方法
	 *
	 * @param session 会话
	 * @param info    握手信息
	 * @param factory 数据缓冲区工厂
	 */
	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, (Sinks.Empty<Void>) null);
	}


	/**
	 * 构造方法
	 *
	 * @param session          会话
	 * @param info             握手信息
	 * @param factory          数据缓冲区工厂
	 * @param completionSink   完成信号
	 */
	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable Sinks.Empty<Void> completionSink) {

		super(session, session.getId(), info, factory, completionSink);
	}


	@Deprecated
	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable reactor.core.publisher.MonoProcessor<Void> completionMono) {

		super(session, session.getId(), info, factory, completionMono);
	}

	/**
	 * 是否可以暂停接收
	 *
	 * @return boolean
	 */
	@Override
	protected boolean canSuspendReceiving() {
		return false;
	}

	/**
	 * 暂停接收
	 */
	@Override
	protected void suspendReceiving() {
		// no-op
	}

	/**
	 * 恢复接收
	 */
	@Override
	protected void resumeReceiving() {
		// no-op
	}

	/**
	 * 发送消息
	 *
	 * @param message 消息
	 * @return boolean
	 * @throws IOException 异常
	 */
	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		//获取字节缓冲区
		ByteBuffer buffer = message.getPayload().asByteBuffer();
		//如果是文本消息
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			//将发送处理器发送标志设置为未准备
			getSendProcessor().setReadyToSend(false);
			//得到文本消息
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			//发送文本消息
			getDelegate().getAsyncRemote().sendText(text, new SendProcessorCallback());
		}
		//如果是二进制消息
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			//将发送处理器发送标志设置为未准备
			getSendProcessor().setReadyToSend(false);
			//发送二进制消息
			getDelegate().getAsyncRemote().sendBinary(buffer, new SendProcessorCallback());
		}
		//如果是ping消息
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			//发送ping消息
			getDelegate().getAsyncRemote().sendPing(buffer);
		}
		//如果是pong消息
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			//发送pong消息
			getDelegate().getAsyncRemote().sendPong(buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	/**
	 * 会话是否打开
	 *
	 * @return boolean
	 */
	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	/**
	 * 关闭会话
	 *
	 * @param status 状态
	 * @return Mono<Void>
	 */
	@Override
	public Mono<Void> close(CloseStatus status) {
		try {
			CloseReason.CloseCode code = CloseCodes.getCloseCode(status.getCode());
			getDelegate().close(new CloseReason(code, status.getReason()));
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
		return Mono.empty();
	}

	/**
	 * 发送处理器回调
	 */
	private final class SendProcessorCallback implements SendHandler {

		/**
		 * 结果
		 *
		 * @param result 结果
		 */
		@Override
		public void onResult(SendResult result) {
			if (result.isOK()) {
				getSendProcessor().setReadyToSend(true);
				getSendProcessor().onWritePossible();
			}
			else {
				getSendProcessor().cancel();
				getSendProcessor().onError(result.getException());
			}
		}

	}

}
