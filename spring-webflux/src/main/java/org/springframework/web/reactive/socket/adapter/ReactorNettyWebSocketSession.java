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

import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.function.Consumer;

/**
 * {@link WebSocketSession} implementation for use with the Reactor Netty's
 * {@link NettyInbound} and {@link NettyOutbound}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketSession
		extends NettyWebSocketSessionSupport<ReactorNettyWebSocketSession.WebSocketConnection> {

	/**
	 * 最大帧有效负载长度
	 */
	private final int maxFramePayloadLength;

	/**
	 * 通道编号
	 */
	private final ChannelId channelId;


	/**
	 * 会话的构造方法，使用 {@link #DEFAULT_FRAME_MAX_SIZE} 的值。
	 *
	 * @param inbound       WebSocket 入站对象
	 * @param outbound      WebSocket 出站对象
	 * @param info          握手信息
	 * @param bufferFactory NettyDataBufferFactory 实例
	 */
	public ReactorNettyWebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound,
										HandshakeInfo info, NettyDataBufferFactory bufferFactory) {

		this(inbound, outbound, info, bufferFactory, DEFAULT_FRAME_MAX_SIZE);
	}

	/**
	 * 带有额外 maxFramePayloadLength 参数的构造方法。
	 *
	 * @param inbound               WebSocket 入站对象
	 * @param outbound              WebSocket 出站对象
	 * @param info                  握手信息
	 * @param bufferFactory         NettyDataBufferFactory 实例
	 * @param maxFramePayloadLength 最大帧负载长度
	 * @since 5.1
	 */
	@SuppressWarnings("rawtypes")
	public ReactorNettyWebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound,
										HandshakeInfo info, NettyDataBufferFactory bufferFactory,
										int maxFramePayloadLength) {

		super(new WebSocketConnection(inbound, outbound), info, bufferFactory);
		this.maxFramePayloadLength = maxFramePayloadLength;
		this.channelId = ((ChannelOperations) inbound).channel().id();
	}


	/**
	 * 返回底层 Netty 通道的 ID。
	 *
	 * @return 底层 Netty 通道的 ID
	 * @since 5.3.4
	 */
	public ChannelId getChannelId() {
		return this.channelId;
	}


	/**
	 * 接收 WebSocket 消息。
	 *
	 * @return WebSocket 消息的 Flux
	 */
	@Override
	public Flux<WebSocketMessage> receive() {
		return getDelegate().getInbound()
				.aggregateFrames(this.maxFramePayloadLength)
				.receiveFrames()
				.map(super::toMessage)
				.doOnNext(message -> {
					if (logger.isTraceEnabled()) {
						logger.trace(getLogPrefix() + "Received " + message);
					}
				});
	}

	/**
	 * 发送 WebSocket 消息。
	 *
	 * @param messages WebSocket 消息的 Publisher
	 * @return 发送操作的 Mono
	 */
	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		Flux<WebSocketFrame> frames = Flux.from(messages)
				.doOnNext(message -> {
					if (logger.isTraceEnabled()) {
						logger.trace(getLogPrefix() + "Sending " + message);
					}
				})
				.map(this::toFrame);
		return getDelegate().getOutbound()
				.sendObject(frames)
				.then();
	}

	/**
	 * 检查会话是否打开。
	 *
	 * @return 若会话未关闭，则为 true
	 */
	@Override
	public boolean isOpen() {
		DisposedCallback callback = new DisposedCallback();
		getDelegate().getInbound().withConnection(callback);
		return !callback.isDisposed();
	}

	/**
	 * 关闭会话。
	 *
	 * @param status 关闭状态
	 * @return 关闭操作的 Mono
	 */
	@Override
	public Mono<Void> close(CloseStatus status) {
		// 这将通知 WebSocketInbound.receiveCloseStatus()
		return getDelegate().getOutbound().sendClose(status.getCode(), status.getReason());
	}

	/**
	 * 获取关闭状态。
	 *
	 * @return 关闭状态的 Mono
	 */
	@Override
	public Mono<CloseStatus> closeStatus() {
		return getDelegate().getInbound().receiveCloseStatus()
				.map(status -> CloseStatus.create(status.code(), status.reasonText()));
	}


	/**
	 * {@link NettyInbound} 和 {@link NettyOutbound} 的简单容器。
	 */
	public static class WebSocketConnection {
		/**
		 * WebSocket 入站对象
		 */
		private final WebsocketInbound inbound;

		/**
		 * WebSocket 出站对象
		 */
		private final WebsocketOutbound outbound;


		public WebSocketConnection(WebsocketInbound inbound, WebsocketOutbound outbound) {
			this.inbound = inbound;
			this.outbound = outbound;
		}

		public WebsocketInbound getInbound() {
			return this.inbound;
		}

		public WebsocketOutbound getOutbound() {
			return this.outbound;
		}
	}

	/**
	 * 被释放的回调函数。
	 */
	private static class DisposedCallback implements Consumer<Connection> {

		/**
		 * 是否已被释放
		 */
		private boolean disposed;

		public boolean isDisposed() {
			return this.disposed;
		}

		@Override
		public void accept(Connection connection) {
			this.disposed = connection.isDisposed();
		}
	}

}
