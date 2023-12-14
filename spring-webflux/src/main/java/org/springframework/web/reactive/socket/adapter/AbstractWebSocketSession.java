/*
 * Copyright 2002-2018 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Convenient base class for {@link WebSocketSession} implementations that
 * holds common fields and exposes accessors. Also implements the
 * {@code WebSocketMessage} factory methods.
 *
 * @param <T> the native delegate type
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractWebSocketSession<T> implements WebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());

	// 实际的 WebSocket 会话代理
	private final T delegate;

	// 会话的唯一标识符
	private final String id;

	// 握手信息
	private final HandshakeInfo handshakeInfo;

	// 数据缓冲区工厂
	private final DataBufferFactory bufferFactory;

	// 会话的属性集合
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	// 日志前缀
	private final String logPrefix;


	/**
	 * 创建一个新的 WebSocket 会话。
	 *
	 * @param delegate      实际的 WebSocket 会话代理
	 * @param id            会话的唯一标识符
	 * @param info          握手信息
	 * @param bufferFactory 数据缓冲区工厂
	 */
	protected AbstractWebSocketSession(T delegate, String id, HandshakeInfo info, DataBufferFactory bufferFactory) {
		Assert.notNull(delegate, "Native session is required.");
		Assert.notNull(id, "Session id is required.");
		Assert.notNull(info, "HandshakeInfo is required.");
		Assert.notNull(bufferFactory, "DataBuffer factory is required.");

		this.delegate = delegate;
		this.id = id;
		this.handshakeInfo = info;
		this.bufferFactory = bufferFactory;
		this.attributes.putAll(info.getAttributes());
		this.logPrefix = initLogPrefix(info, id);

		if (logger.isDebugEnabled()) {
			logger.debug(getLogPrefix() + "Session id \"" + getId() + "\" for " + getHandshakeInfo().getUri());
		}
	}

	/**
	 * 初始化日志前缀
	 *
	 * @param info 握手信息
	 * @param id   会话唯一编号
	 * @return 日志前缀
	 */
	private static String initLogPrefix(HandshakeInfo info, String id) {
		return info.getLogPrefix() != null ? info.getLogPrefix() : "[" + id + "] ";
	}

	/**
	 * 获取实际的 WebSocket 会话代理。
	 *
	 * @return 实际的 WebSocket 会话代理
	 */
	protected T getDelegate() {
		return this.delegate;
	}

	/**
	 * 获取会话编号
	 *
	 * @return 会话编号
	 */
	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * 获取握手信息。
	 *
	 * @return 握手信息
	 */
	@Override
	public HandshakeInfo getHandshakeInfo() {
		return this.handshakeInfo;
	}

	/**
	 * 获取数据缓冲区工厂。
	 *
	 * @return 数据缓冲区工厂
	 */
	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	/**
	 * 获取会话的属性集合。
	 *
	 * @return 会话的属性集合
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * 获取日志前缀。
	 *
	 * @return 日志前缀
	 */
	protected String getLogPrefix() {
		return this.logPrefix;
	}

	/**
	 * 接收 WebSocket 消息的 Flux。
	 *
	 * @return WebSocket 消息的 Flux
	 */
	@Override
	public abstract Flux<WebSocketMessage> receive();

	/**
	 * 发送 WebSocket 消息的 Mono。
	 *
	 * @param messages 要发送的 WebSocket 消息
	 * @return Mono 表示发送状态
	 */
	@Override
	public abstract Mono<Void> send(Publisher<WebSocketMessage> messages);


	// WebSocketMessage 工厂方法

	/**
	 * 创建文本消息的 WebSocketMessage。
	 *
	 * @param payload 消息负载
	 * @return 文本消息的 WebSocketMessage
	 */
	@Override
	public WebSocketMessage textMessage(String payload) {
		byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = bufferFactory().wrap(bytes);
		return new WebSocketMessage(WebSocketMessage.Type.TEXT, buffer);
	}

	/**
	 * 创建二进制消息的 WebSocketMessage。
	 *
	 * @param payloadFactory 负载工厂函数
	 * @return 二进制消息的 WebSocketMessage
	 */
	@Override
	public WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.BINARY, payload);
	}

	/**
	 * 创建 Ping 消息的 WebSocketMessage。
	 *
	 * @param payloadFactory 负载工厂函数
	 * @return Ping 消息的 WebSocketMessage
	 */
	@Override
	public WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.PING, payload);
	}


	/**
	 * 创建 Pong 消息的 WebSocketMessage。
	 *
	 * @param payloadFactory 负载工厂函数
	 * @return Pong 消息的 WebSocketMessage
	 */
	@Override
	public WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.PONG, payload);
	}

	/**
	 * 返回此对象的字符串表示形式，包括 URI 信息。
	 *
	 * @return 对象的字符串表示形式
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + getId() + ", uri=" + getHandshakeInfo().getUri() + "]";
	}

}
