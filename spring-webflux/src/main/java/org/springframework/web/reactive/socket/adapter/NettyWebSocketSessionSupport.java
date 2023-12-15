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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Netty-based {@link WebSocketSession} adapters that provides
 * convenience methods to convert Netty {@link WebSocketFrame WebSocketFrames} to and from
 * {@link WebSocketMessage WebSocketMessages}.
 *
 * @param <T> the native delegate type
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class NettyWebSocketSessionSupport<T> extends AbstractWebSocketSession<T> {

	/**
	 * 入站 WebSocket 帧的默认最大大小。
	 */
	public static final int DEFAULT_FRAME_MAX_SIZE = 64 * 1024;


	/**
	 * 存储WebSocket消息类型的映射。
	 */
	private static final Map<Class<?>, WebSocketMessage.Type> messageTypes;

	static {
		// 初始化messageTypes映射，包含不同WebSocketFrame类对应的WebSocketMessage类型
		messageTypes = new HashMap<>(8);
		messageTypes.put(TextWebSocketFrame.class, WebSocketMessage.Type.TEXT);
		messageTypes.put(BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);
		messageTypes.put(PingWebSocketFrame.class, WebSocketMessage.Type.PING);
		messageTypes.put(PongWebSocketFrame.class, WebSocketMessage.Type.PONG);
	}


	/**
	 * 构造方法。
	 *
	 * @param delegate 代表原生 WebSocket 会话、通道或连接的对象
	 * @param info     握手信息
	 * @param factory  NettyDataBufferFactory 实例
	 */
	protected NettyWebSocketSessionSupport(T delegate, HandshakeInfo info, NettyDataBufferFactory factory) {
		super(delegate, ObjectUtils.getIdentityHexString(delegate), info, factory);
	}


	/**
	 * 获取 NettyDataBufferFactory 实例。
	 *
	 * @return NettyDataBufferFactory 实例
	 */
	@Override
	public NettyDataBufferFactory bufferFactory() {
		return (NettyDataBufferFactory) super.bufferFactory();
	}


	/**
	 * 将 WebSocket 帧转换为 WebSocket 消息。
	 *
	 * @param frame WebSocket 帧对象
	 * @return 转换后的 WebSocket 消息
	 */
	protected WebSocketMessage toMessage(WebSocketFrame frame) {
		DataBuffer payload = bufferFactory().wrap(frame.content());
		return new WebSocketMessage(messageTypes.get(frame.getClass()), payload, frame);
	}

	/**
	 * 将 WebSocket 消息转换为 WebSocket 帧。
	 *
	 * @param message WebSocket 消息对象
	 * @return 转换后的 WebSocket 帧
	 * @throws IllegalArgumentException 如果消息类型不符合预期
	 */
	protected WebSocketFrame toFrame(WebSocketMessage message) {
		if (message.getNativeMessage() != null) {
			// 如果WebSocketMessage的底层原生消息不为null，则直接返回底层原生消息
			return message.getNativeMessage();
		}
		ByteBuf byteBuf = NettyDataBufferFactory.toByteBuf(message.getPayload());
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			// 如果消息类型为TEXT，则创建TextWebSocketFrame
			return new TextWebSocketFrame(byteBuf);
		} else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			// 如果消息类型为BINARY，则创建BinaryWebSocketFrame
			return new BinaryWebSocketFrame(byteBuf);
		} else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			// 如果消息类型为PING，则创建PingWebSocketFrame
			return new PingWebSocketFrame(byteBuf);
		} else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			// 如果消息类型为PONG，则创建PongWebSocketFrame
			return new PongWebSocketFrame(byteBuf);
		} else {
			// 如果消息类型未知，则抛出IllegalArgumentException异常
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
	}

}
