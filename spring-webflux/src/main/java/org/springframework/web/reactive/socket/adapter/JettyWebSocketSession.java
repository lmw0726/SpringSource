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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Spring {@link WebSocketSession} implementation that adapts to a Jetty
 * WebSocket {@link org.eclipse.jetty.websocket.api.Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	/**
	 * 保存当前的suspendToken
	 */
	@Nullable
	private volatile SuspendToken suspendToken;


	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, (Sinks.Empty<Void>) null);
	}

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								 @Nullable Sinks.Empty<Void> completionSink) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionSink);
		// TODO: suspend causes failures if invoked at this stage
		// suspendReceiving();
	}

	@Deprecated
	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								 @Nullable reactor.core.publisher.MonoProcessor<Void> completionMono) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionMono);
	}


	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	/**
	 * 暂停接收消息。
	 *
	 * @throws IllegalStateException 如果当前已经暂停接收消息。
	 */
	@Override
	protected void suspendReceiving() {
		// 如果当前暂停令牌不为空，抛出异常，表示已经暂停
		Assert.state(this.suspendToken == null, "Already suspended");

		// 设置暂停令牌为委托对象生成的暂停令牌
		this.suspendToken = getDelegate().suspend();
	}

	/**
	 * 写操作回调的内部类。
	 */
	@Override
	protected void resumeReceiving() {
		SuspendToken tokenToUse = this.suspendToken;
		this.suspendToken = null;
		if (tokenToUse != null) {
			tokenToUse.resume();
		}
	}


	/**
	 * 发送WebSocket消息的方法。
	 *
	 * @param message 要发送的WebSocket消息
	 * @return 如果成功发送返回true，否则返回false。
	 * @throws IOException 发送消息过程中的IO异常
	 */
	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		// 将消息的负载转换为ByteBuffer
		ByteBuffer buffer = message.getPayload().asByteBuffer();

		// 根据消息类型执行相应的发送操作
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			// 设置发送处理器为不可发送状态，并发送文本消息
			getSendProcessor().setReadyToSend(false);
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			getDelegate().getRemote().sendString(text, new SendProcessorCallback());
		} else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			// 设置发送处理器为不可发送状态，并发送二进制数据消息
			getSendProcessor().setReadyToSend(false);
			getDelegate().getRemote().sendBytes(buffer, new SendProcessorCallback());
		} else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			// 发送Ping消息
			getDelegate().getRemote().sendPing(buffer);
		} else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			// 发送Pong消息
			getDelegate().getRemote().sendPong(buffer);
		} else {
			// 如果消息类型未知，则抛出异常
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}

		// 返回发送成功
		return true;
	}


	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		getDelegate().close(status.getCode(), status.getReason());
		return Mono.empty();
	}
	private final class SendProcessorCallback implements WriteCallback {


		/**
		 * 写操作失败时的回调方法。
		 * @param x 失败时的异常信息
		 */
		@Override
		public void writeFailed(Throwable x) {
			// 取消发送处理器并触发错误处理
			getSendProcessor().cancel();
			getSendProcessor().onError(x);
		}

		/**
		 * 写操作成功时的回调方法。
		 */
		@Override
		public void writeSuccess() {
			// 设置发送处理器为可发送状态，并触发可写入事件
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();
		}
	}


}