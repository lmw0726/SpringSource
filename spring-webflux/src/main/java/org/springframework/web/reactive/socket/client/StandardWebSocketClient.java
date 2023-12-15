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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import javax.websocket.*;
import javax.websocket.ClientEndpointConfig.Configurator;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * {@link WebSocketClient}的实现，用于与Java WebSocket API一起使用。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @see <a href="https://www.jcp.org/en/jsr/detail?id=356">https://www.jcp.org/en/jsr/detail?id=356</a>
 * @since 5.0
 */
public class StandardWebSocketClient implements WebSocketClient {
	/**
	 * StandardWebSocketClient是一个实现了WebSocketClient接口的类，用于与Java WebSocket API一起使用。
	 */
	private static final Log logger = LogFactory.getLog(StandardWebSocketClient.class);

	// WebSocketContainer实例，用于管理WebSocket连接
	private final WebSocketContainer webSocketContainer;

	/**
	 * 默认构造函数，调用ContainerProvider.getWebSocketContainer()获取一个新的WebSocketContainer实例。
	 */
	public StandardWebSocketClient() {
		this(ContainerProvider.getWebSocketContainer());
	}

	/**
	 * 接受现有的WebSocketContainer实例的构造函数。
	 *
	 * @param webSocketContainer WebSocketContainer实例
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}

	/**
	 * 返回配置的WebSocketContainer实例。
	 *
	 * @return WebSocketContainer实例
	 */
	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	/**
	 * 执行WebSocket握手请求到指定的URL，并处理WebSocket会话。
	 *
	 * @param url     WebSocket握手的URL
	 * @param handler WebSocket会话的处理程序
	 * @return 表示WebSocket会话处理结果的完成Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	/**
	 * 执行WebSocket握手请求到指定的URL，并使用给定的头信息处理WebSocket会话。
	 *
	 * @param url     WebSocket握手的URL
	 * @param headers WebSocket握手请求的头信息
	 * @param handler WebSocket会话的处理程序
	 * @return 表示WebSocket会话处理结果的完成Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	/**
	 * 执行WebSocket握手请求的内部方法，创建并连接到WebSocket会话。
	 *
	 * @param url            WebSocket握手的URL
	 * @param requestHeaders WebSocket握手请求的头信息
	 * @param handler        WebSocket会话的处理程序
	 * @return 表示WebSocket会话处理结果的完成Mono<Void>
	 */
	private Mono<Void> executeInternal(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		// 创建一个空的Sinks
		Sinks.Empty<Void> completion = Sinks.empty();
		// 返回一个根据上下文延迟执行的Mono
		return Mono.deferContextual(
						contextView -> {
							// 如果日志级别为DEBUG，则记录连接到URL的调试信息
							if (logger.isDebugEnabled()) {
								logger.debug("Connecting to " + url);
							}
							// 获取处理程序的子协议列表
							List<String> protocols = handler.getSubProtocols();
							// 创建WebSocket配置
							DefaultConfigurator configurator = new DefaultConfigurator(requestHeaders);
							// 创建Endpoint，处理WebSocket会话
							Endpoint endpoint = createEndpoint(
									url, ContextWebSocketHandler.decorate(handler, contextView), completion, configurator);
							// 创建Endpoint配置
							ClientEndpointConfig config = createEndpointConfig(configurator, protocols);
							try {
								// 连接到WebSocket服务器
								this.webSocketContainer.connectToServer(endpoint, config, url);
								return completion.asMono();
							} catch (Exception ex) {
								return Mono.error(ex);
							}
						})
				// 在有界弹性调度器上订阅
				// 连接到服务器是阻塞的
				.subscribeOn(Schedulers.boundedElastic());

	}

	/**
	 * 创建Endpoint，并将WebSocket会话委托给WebSocket处理程序适配器。
	 *
	 * @param url            WebSocket握手的URL
	 * @param handler        WebSocket会话的处理程序
	 * @param completionSink 完成Sink
	 * @param configurator   WebSocket配置
	 * @return WebSocket处理程序适配器
	 */
	private StandardWebSocketHandlerAdapter createEndpoint(URI url, WebSocketHandler handler,
														   Sinks.Empty<Void> completionSink, DefaultConfigurator configurator) {

		return new StandardWebSocketHandlerAdapter(handler, session ->
				createWebSocketSession(session, createHandshakeInfo(url, configurator), completionSink));
	}

	/**
	 * 创建握手信息。
	 *
	 * @param url          WebSocket握手的URL
	 * @param configurator WebSocket配置
	 * @return 握手信息
	 */
	private HandshakeInfo createHandshakeInfo(URI url, DefaultConfigurator configurator) {
		// 获取配置器的响应头信息
		HttpHeaders responseHeaders = configurator.getResponseHeaders();
		// 从响应头中获取“Sec-WebSocket-Protocol”字段的值作为协议
		String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
		// 创建一个 HandshakeInfo 对象，传入 URL、响应头信息、空的 Mono（类似于一个可能为空的值），以及获取的协议信息
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);

	}

	/**
	 * 创建并返回WebSocket会话对象。
	 *
	 * @param session        WebSocket会话
	 * @param info           握手信息
	 * @param completionSink 完成Sink
	 * @return WebSocket会话对象
	 */
	protected StandardWebSocketSession createWebSocketSession(
			Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {

		return new StandardWebSocketSession(
				session, info, DefaultDataBufferFactory.sharedInstance, completionSink);
	}

	/**
	 * 创建Endpoint配置。
	 *
	 * @param configurator WebSocket配置
	 * @param subProtocols 子协议列表
	 * @return ClientEndpointConfig
	 */
	private ClientEndpointConfig createEndpointConfig(Configurator configurator, List<String> subProtocols) {
		// 创建客户端端点配置的构建器
		return ClientEndpointConfig.Builder.create()
				// 设置配置器
				.configurator(configurator)
				// 设置首选子协议
				.preferredSubprotocols(subProtocols)
				// 构建客户端端点配置
				.build();

	}

	/**
	 * 返回DataBufferFactory实例。
	 *
	 * @return DataBufferFactory实例
	 */
	protected DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	/**
	 * DefaultConfigurator类继承自Configurator，用于处理WebSocket的配置。
	 */
	private static final class DefaultConfigurator extends Configurator {

		/**
		 * 保存请求头和响应头的类成员。
		 */
		private final HttpHeaders requestHeaders;
		private final HttpHeaders responseHeaders = new HttpHeaders();

		/**
		 * 构造函数，接收请求头信息。
		 *
		 * @param requestHeaders 请求头信息
		 */
		public DefaultConfigurator(HttpHeaders requestHeaders) {
			this.requestHeaders = requestHeaders;
		}

		/**
		 * 获取响应头信息。
		 *
		 * @return 响应头信息
		 */
		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		/**
		 * 在请求之前应用配置。
		 *
		 * @param requestHeaders 请求头信息
		 */
		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.requestHeaders);
		}

		/**
		 * 在响应之后应用配置。
		 *
		 * @param response 握手响应
		 */
		@Override
		public void afterResponse(HandshakeResponse response) {
			response.getHeaders().forEach(this.responseHeaders::put);
		}
	}

}
