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

import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
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
 * Spring {@link WebSocketSession} implementation that adapts to an Undertow
 * {@link io.undertow.websockets.core.WebSocketChannel}.
 * UndertowWebSocketSession类是AbstractListenerWebSocketSession<WebSocketChannel>的子类，
 * 用于处理Undertow的WebSocket会话。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketSession extends AbstractListenerWebSocketSession<WebSocketChannel> {

	/**
	 * 构造函数，接受WebSocket通道、握手信息、数据缓冲工厂参数。
	 *
	 * @param channel WebSocket通道
	 * @param info    握手信息
	 * @param factory 数据缓冲工厂
	 */
	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo info, DataBufferFactory factory) {
		this(channel, info, factory, (Sinks.Empty<Void>) null);
	}

	/**
	 * 构造函数，接受WebSocket通道、握手信息、数据缓冲工厂和完成Sink参数。
	 *
	 * @param channel        WebSocket通道
	 * @param info           握手信息
	 * @param factory        数据缓冲工厂
	 * @param completionSink 完成Sink
	 */
	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo info,
									DataBufferFactory factory, @Nullable Sinks.Empty<Void> completionSink) {

		super(channel, ObjectUtils.getIdentityHexString(channel), info, factory, completionSink);
		suspendReceiving();
	}

	/**
	 * 用于兼容性目的的已弃用构造函数，
	 * 接受WebSocket通道、握手信息、数据缓冲工厂和完成Mono参数。
	 *
	 * @param channel        WebSocket通道
	 * @param info           握手信息
	 * @param factory        数据缓冲工厂
	 * @param completionMono 完成Mono
	 */
	@Deprecated
	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo info,
									DataBufferFactory factory, @Nullable reactor.core.publisher.MonoProcessor<Void> completionMono) {

		super(channel, ObjectUtils.getIdentityHexString(channel), info, factory, completionMono);
		suspendReceiving();
	}

	/**
	 * 判断是否可以暂停接收消息。
	 *
	 * @return 如果可以暂停接收返回true，否则返回false。
	 */
	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	/**
	 * 暂停接收消息。
	 */
	@Override
	protected void suspendReceiving() {
		getDelegate().suspendReceives();
	}

	/**
	 * 恢复接收消息。
	 */
	@Override
	protected void resumeReceiving() {
		getDelegate().resumeReceives();
	}

	/**
	 * 发送WebSocket消息的方法。
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
			// 设置发送处理器为不可发送状态
			getSendProcessor().setReadyToSend(false);
			// 将ByteBuffer转换为UTF-8编码的文本，并发送
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			WebSockets.sendText(text, getDelegate(), new SendProcessorCallback(message.getPayload()));
		} else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			// 设置发送处理器为不可发送状态，并发送二进制数据
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendBinary(buffer, getDelegate(), new SendProcessorCallback(message.getPayload()));
		} else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			// 设置发送处理器为不可发送状态，并发送Ping消息
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendPing(buffer, getDelegate(), new SendProcessorCallback(message.getPayload()));
		} else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			// 设置发送处理器为不可发送状态，并发送Pong消息
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendPong(buffer, getDelegate(), new SendProcessorCallback(message.getPayload()));
		} else {
			// 如果消息类型未知，则抛出异常
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}

		// 返回发送成功
		return true;
	}


	/**
	 * 判断WebSocket会话是否打开。
	 *
	 * @return 如果会话打开返回true，否则返回false。
	 */
	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	/**
	 * 关闭WebSocket会话。
	 *
	 * @param status 关闭的状态信息
	 * @return 返回一个空的Mono
	 */
	@Override
	public Mono<Void> close(CloseStatus status) {
		CloseMessage cm = new CloseMessage(status.getCode(), status.getReason());
		if (!getDelegate().isCloseFrameSent()) {
			WebSockets.sendClose(cm, getDelegate(), null);
		}
		return Mono.empty();
	}

	/**
	 * WebSocket消息发送回调处理。
	 */
	private final class SendProcessorCallback implements WebSocketCallback<Void> {

		private final DataBuffer payload;

		SendProcessorCallback(DataBuffer payload) {
			this.payload = payload;
		}

		/**
		 * WebSocket消息发送成功时的回调。
		 *
		 * @param channel WebSocket通道
		 * @param context 上下文信息
		 */
		@Override
		public void complete(WebSocketChannel channel, Void context) {
			// 释放DataBuffer资源
			DataBufferUtils.release(this.payload);
			// 设置发送处理器为可发送状态，并触发可写入事件
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();

		}

		/**
		 * WebSocket消息发送失败时的回调。
		 *
		 * @param channel   WebSocket通道
		 * @param context   上下文信息
		 * @param throwable 发送过程中的异常
		 */
		@Override
		public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
			DataBufferUtils.release(this.payload);
			getSendProcessor().cancel();
			getSendProcessor().onError(throwable);
		}
	}
}
