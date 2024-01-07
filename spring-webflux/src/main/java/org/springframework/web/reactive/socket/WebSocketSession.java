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

package org.springframework.web.reactive.socket;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * 表示一个 WebSocket 会话。
 *
 * <p>使用 {@link WebSocketSession#receive() session.receive()} 来组合入站消息流，
 * 并使用 {@link WebSocketSession#send(Publisher) session.send(publisher)} 来提供出站消息流。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketSession {

	/**
	 * 返回会话的ID。
	 */
	String getId();

	/**
	 * 返回握手请求的信息。
	 */
	HandshakeInfo getHandshakeInfo();

	/**
	 * 返回一个{@code DataBuffer}工厂来创建消息载荷。
	 *
	 * @return 会话的缓冲区工厂
	 */
	DataBufferFactory bufferFactory();

	/**
	 * 返回与WebSocket会话关联的属性映射。
	 *
	 * @return 带有会话属性的Map（永远不为{@code null}）
	 * @since 5.1
	 */
	Map<String, Object> getAttributes();

	/**
	 * 提供对入站消息流的访问。
	 * <p>当连接关闭时，此流接收完成或错误信号。在典型的{@link WebSocketHandler}实现中，此流被组合到整体处理流程中，因此当连接关闭时，处理也将结束。
	 * <p>有关如何处理会话的更多详细信息和示例，请参阅{@link WebSocketHandler}的类级别文档和参考资料。
	 */
	Flux<WebSocketMessage> receive();

	/**
	 * 给出一个传出消息的源，写入消息并返回一个{@code Mono<Void>}，当源完成并且写入完成时完成。
	 * <p>有关如何处理会话的更多详细信息和示例，请参阅{@link WebSocketHandler}的类级别文档和参考资料。
	 */
	Mono<Void> send(Publisher<WebSocketMessage> messages);

	/**
	 * 底层连接是否打开。
	 *
	 * @since 5.3.1
	 */
	boolean isOpen();

	/**
	 * 使用{@link CloseStatus#NORMAL}关闭WebSocket会话。
	 */
	default Mono<Void> close() {
		return close(CloseStatus.NORMAL);
	}

	/**
	 * 使用给定状态关闭WebSocket会话。
	 *
	 * @param status 关闭状态
	 */
	Mono<Void> close(CloseStatus status);

	/**
	 * 提供{@code CloseStatus}，该状态用于关闭会话（无论是本地还是远程关闭），如果会话没有状态结束，则返回空。
	 *
	 * @since 5.3
	 */
	Mono<CloseStatus> closeStatus();

	// WebSocketMessage工厂方法

	/**
	 * 使用{@link #bufferFactory()}创建文本{@link WebSocketMessage}的工厂方法。
	 */
	WebSocketMessage textMessage(String payload);

	/**
	 * 使用{@link #bufferFactory()}创建二进制WebSocketMessage的工厂方法。
	 */
	WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * 使用{@link #bufferFactory()}创建ping WebSocketMessage的工厂方法。
	 */
	WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * 使用{@link #bufferFactory()}创建pong WebSocketMessage的工厂方法。
	 */
	WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

}
