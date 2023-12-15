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
	 * 可空的易变暂停令牌。
	 */
	@Nullable
	private volatile SuspendToken suspendToken;


	/**
	 * 构造一个JettyWebSocketSession对象。
	 *
	 * @param session Jetty的会话
	 * @param info    握手信息
	 * @param factory 数据缓冲工厂
	 */
	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		// 调用另一个构造方法，并将completionSink参数设为null
		this(session, info, factory, (Sinks.Empty<Void>) null);
	}


	/**
	 * 构造一个JettyWebSocketSession对象。
	 *
	 * @param session        Jetty的会话
	 * @param info           握手信息
	 * @param factory        数据缓冲工厂
	 * @param completionSink 完成时的Sinks.Empty处理器
	 */
	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								 @Nullable Sinks.Empty<Void> completionSink) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionSink);
		// TODO: 如果在这个阶段调用suspend会导致失败
		// suspendReceiving();
	}


	/**
	 * 构造一个JettyWebSocketSession对象，已被弃用。
	 *
	 * @param session        Jetty的会话
	 * @param info           握手信息
	 * @param factory        数据缓冲工厂
	 * @param completionMono 完成时的Mono处理器
	 */
	@Deprecated
	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								 @Nullable reactor.core.publisher.MonoProcessor<Void> completionMono) {
		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionMono);
	}


	/**
	 * 检查是否可以暂停接收操作。
	 *
	 * @return 总是返回true，表示可以暂停接收操作
	 */
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
		// 清空暂停令牌
		this.suspendToken = null;
		if (tokenToUse != null) {
			//如果暂停令牌不为空，则恢复接收
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

	/**
	 * 检查当前会话是否打开。
	 *
	 * @return 如果会话打开，则返回true；否则返回false
	 */
	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	/**
	 * 关闭当前会话。
	 *
	 * @param status 关闭状态
	 * @return 一个表示异步关闭操作的Mono
	 */
	@Override
	public Mono<Void> close(CloseStatus status) {
		getDelegate().close(status.getCode(), status.getReason());
		return Mono.empty();
	}

	/**
	 * 发送处理器回调类，实现了WriteCallback接口。
	 */
	private final class SendProcessorCallback implements WriteCallback {

		/**
		 * 写操作失败时的回调方法。
		 *
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