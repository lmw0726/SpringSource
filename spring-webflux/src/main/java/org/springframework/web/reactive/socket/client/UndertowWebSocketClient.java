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

package org.springframework.web.reactive.socket.client;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.WebSocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketSession;
import org.xnio.IoFuture;
import org.xnio.XnioWorker;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 基于Undertow的 {@link WebSocketClient} 实现。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketClient implements WebSocketClient {

	/**
	 * UndertowWebSocketClient 类，用于创建 WebSocket 客户端连接。
	 */
	private static final Log logger = LogFactory.getLog(UndertowWebSocketClient.class);

	// 默认的池缓冲区大小
	private static final int DEFAULT_POOL_BUFFER_SIZE = 8192;

	// XnioWorker 实例
	private final XnioWorker worker;

	// ByteBufferPool 实例
	private ByteBufferPool byteBufferPool;

	// 用于配置 ConnectionBuilder 的消费者
	private final Consumer<ConnectionBuilder> builderConsumer;


	/**
	 * 使用 XnioWorker 构造 UndertowWebSocketClient 的构造函数。
	 *
	 * @param worker XnioWorker 实例
	 */
	public UndertowWebSocketClient(XnioWorker worker) {
		this(worker, builder -> {
		});
	}

	/**
	 * 提供对每个 WebSocket 连接的 ConnectionBuilder 的额外控制的替代构造函数。
	 *
	 * @param worker          XnioWorker 实例
	 * @param builderConsumer 用于配置 ConnectionBuilder 的消费者
	 */
	public UndertowWebSocketClient(XnioWorker worker, Consumer<ConnectionBuilder> builderConsumer) {
		this(worker, new DefaultByteBufferPool(false, DEFAULT_POOL_BUFFER_SIZE), builderConsumer);
	}

	/**
	 * 提供对每个 WebSocket 连接的 ConnectionBuilder 的额外控制的替代构造函数。
	 *
	 * @param worker          XnioWorker 实例
	 * @param byteBufferPool  ByteBufferPool 实例
	 * @param builderConsumer 用于配置 ConnectionBuilder 的消费者
	 * @since 5.0.8
	 */
	public UndertowWebSocketClient(XnioWorker worker, ByteBufferPool byteBufferPool,
								   Consumer<ConnectionBuilder> builderConsumer) {

		Assert.notNull(worker, "XnioWorker must not be null");
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.worker = worker;
		this.byteBufferPool = byteBufferPool;
		this.builderConsumer = builderConsumer;
	}


	/**
	 * 返回配置的 XnioWorker。
	 *
	 * @return 配置的 XnioWorker 实例
	 */
	public XnioWorker getXnioWorker() {
		return this.worker;
	}

	/**
	 * 设置 {@link io.undertow.connector.ByteBufferPool ByteBufferPool} 以传递给
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder}。
	 * <p>默认情况下，使用间接的 {@link io.undertow.server.DefaultByteBufferPool}
	 * 以 8192 的缓冲区大小。
	 *
	 * @param byteBufferPool ByteBufferPool 实例
	 * @see #DEFAULT_POOL_BUFFER_SIZE
	 * @since 5.0.8
	 */
	public void setByteBufferPool(ByteBufferPool byteBufferPool) {
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * 返回当前用于此客户端新创建的 WebSocket 会话的 {@link io.undertow.connector.ByteBufferPool}。
	 *
	 * @return 字节缓冲区池
	 * @since 5.0.8
	 */
	public ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * 返回配置的 <code>Consumer&lt;ConnectionBuilder&gt;</code>。
	 *
	 * @return 配置的 ConnectionBuilder 消费者
	 */
	public Consumer<ConnectionBuilder> getConnectionBuilderConsumer() {
		return this.builderConsumer;
	}

	/**
	 * 执行 WebSocket 连接，使用默认的请求头。
	 *
	 * @param url     WebSocket 连接的 URI
	 * @param handler WebSocket 处理器
	 * @return 表示 WebSocket 连接结果的 Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	/**
	 * 执行 WebSocket 连接，使用指定的 URI、请求头和 WebSocket 处理器。
	 *
	 * @param url     WebSocket 连接的 URI
	 * @param headers 请求头
	 * @param handler WebSocket 处理器
	 * @return 表示 WebSocket 连接结果的 Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	/**
	 * 内部执行 WebSocket 连接的方法。
	 *
	 * @param url     WebSocket 连接的 URI
	 * @param headers 请求头
	 * @param handler WebSocket 处理器
	 * @return 表示 WebSocket 连接结果的 Mono<Void>
	 */
	private Mono<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		// 创建一个空的 Sinks.Empty<Void>
		Sinks.Empty<Void> completion = Sinks.empty();
		return Mono.deferContextual(
				contextView -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
					// 获取处理器的子协议
					List<String> protocols = handler.getSubProtocols();
					// 创建连接构建器
					ConnectionBuilder builder = createConnectionBuilder(url);
					// 创建默认协商
					DefaultNegotiation negotiation = new DefaultNegotiation(protocols, headers, builder);
					builder.setClientNegotiation(negotiation);
					// 连接 WebSocket 并添加通知器
					builder.connect().addNotifier(
							new IoFuture.HandlingNotifier<WebSocketChannel, Object>() {
								/**
								 * 处理成功完成的方法。
								 * @param channel WebSocket 通道
								 * @param attachment 附件对象
								 */
								@Override
								public void handleDone(WebSocketChannel channel, Object attachment) {
									// 处理 WebSocket 通道
									handleChannel(url, ContextWebSocketHandler.decorate(handler, contextView),
											completion, negotiation, channel);
								}

								/**
								 * 处理失败的方法。
								 * @param ex 失败时的异常
								 * @param attachment 附件对象
								 */
								@Override
								public void handleFailed(IOException ex, Object attachment) {
									// 忽略结果：不能溢出，如果不是第一个或者没有人监听，则无问题
									completion.tryEmitError(
											new IllegalStateException("Failed to connect to " + url, ex));
								}
							}, null);
					return completion.asMono();
				});
	}

	/**
	 * 创建给定 URI 的 {@link ConnectionBuilder}。
	 * <p>默认实现创建一个带有配置的 {@link #getXnioWorker() XnioWorker} 和
	 * {@link #getByteBufferPool() ByteBufferPool} 的构建器，
	 * 然后将其传递给在构造时提供的 {@link #getConnectionBuilderConsumer() 消费者}。
	 *
	 * @param url 给定的 URI
	 * @return 创建的 ConnectionBuilder
	 */
	protected ConnectionBuilder createConnectionBuilder(URI url) {
		ConnectionBuilder builder = io.undertow.websockets.client.WebSocketClient
				.connectionBuilder(getXnioWorker(), getByteBufferPool(), url);
		this.builderConsumer.accept(builder);
		return builder;
	}

	/**
	 * 处理 WebSocket 通道，连接到给定的 URI。
	 *
	 * @param url            WebSocket 连接的 URI
	 * @param handler        WebSocket 处理器
	 * @param completionSink 用于处理连接完成的 Sink
	 * @param negotiation    WebSocket 客户端协商
	 * @param channel        WebSocket 通道
	 */
	private void handleChannel(URI url, WebSocketHandler handler, Sinks.Empty<Void> completionSink,
							   DefaultNegotiation negotiation, WebSocketChannel channel) {

		// 创建握手信息
		HandshakeInfo info = createHandshakeInfo(url, negotiation);
		// 使用默认数据缓冲工厂创建 UndertowWebSocketSession
		DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
		UndertowWebSocketSession session = new UndertowWebSocketSession(channel, info, bufferFactory, completionSink);
		// 创建 UndertowWebSocketHandlerAdapter
		UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

		// 设置接收处理器并继续接收数据
		channel.getReceiveSetter().set(adapter);
		channel.resumeReceives();

		// 处理 WebSocket 会话
		handler.handle(session)
				.checkpoint(url + " [UndertowWebSocketClient]")
				.subscribe(session);
	}

	/**
	 * 创建握手信息。
	 *
	 * @param url         WebSocket 连接的 URI
	 * @param negotiation WebSocket 客户端协商
	 * @return 创建的握手信息
	 */
	private HandshakeInfo createHandshakeInfo(URI url, DefaultNegotiation negotiation) {
		HttpHeaders responseHeaders = negotiation.getResponseHeaders();
		String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
	}


	/**
	 * 默认的 WebSocket 客户端协商实现，继承自 WebSocketClientNegotiation。
	 */
	private static final class DefaultNegotiation extends WebSocketClientNegotiation {

		// 请求头
		private final HttpHeaders requestHeaders;

		// 响应头
		private final HttpHeaders responseHeaders = new HttpHeaders();

		// WebSocket 客户端委托实例
		@Nullable
		private final WebSocketClientNegotiation delegate;

		/**
		 * 构造函数，接受协议列表、请求头和连接构建器。
		 *
		 * @param protocols         协议列表
		 * @param requestHeaders    请求头
		 * @param connectionBuilder 连接构建器
		 */
		public DefaultNegotiation(List<String> protocols, HttpHeaders requestHeaders,
								  ConnectionBuilder connectionBuilder) {

			super(protocols, Collections.emptyList());
			this.requestHeaders = requestHeaders;
			this.delegate = connectionBuilder.getClientNegotiation();
		}

		/**
		 * 获取响应头。
		 *
		 * @return 响应头
		 */
		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		/**
		 * 在发起请求之前设置请求头。
		 *
		 * @param headers 请求头映射
		 */
		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			this.requestHeaders.forEach(headers::put);
			if (this.delegate != null) {
				this.delegate.beforeRequest(headers);
			}
		}

		/**
		 * 在发起请求之后处理响应头。
		 *
		 * @param headers 响应头映射
		 */
		@Override
		public void afterRequest(Map<String, List<String>> headers) {
			headers.forEach(this.responseHeaders::put);
			if (this.delegate != null) {
				this.delegate.afterRequest(headers);
			}
		}
	}

}
