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

package org.springframework.web.reactive.socket.server.upgrade;

import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * 用于Tomcat的{@link RequestUpgradeStrategy}
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class TomcatRequestUpgradeStrategy implements RequestUpgradeStrategy {
	/**
	 * WebSocket 容器的属性名。
	 */
	private static final String SERVER_CONTAINER_ATTR = "javax.websocket.server.ServerContainer";

	/**
	 * 异步发送超时时间
	 */
	@Nullable
	private Long asyncSendTimeout;

	/**
	 * 最大会话空闲时间
	 */
	@Nullable
	private Long maxSessionIdleTimeout;

	/**
	 * 最大文本消息缓冲区大小
	 */
	@Nullable
	private Integer maxTextMessageBufferSize;

	/**
	 * 最大二进制消息缓冲区大小
	 */
	@Nullable
	private Integer maxBinaryMessageBufferSize;

	/**
	 * WebSocket 服务器容器对象
	 */
	@Nullable
	private WsServerContainer serverContainer;

	/**
	 * 暴露了底层配置选项在{@link javax.websocket.server.ServerContainer#setAsyncSendTimeout(long)}上的方法。
	 */
	public void setAsyncSendTimeout(Long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	/**
	 * 获取异步发送超时时间。
	 *
	 * @return 异步发送超时时间，可能为 null
	 */
	@Nullable
	public Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}


	/**
	 * 暴露了底层配置选项在{@link javax.websocket.server.ServerContainer#setDefaultMaxSessionIdleTimeout(long)}上的方法。
	 */
	public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	/**
	 * 获取会话空闲超时时间。
	 *
	 * @return 会话空闲超时时间，可能为 null
	 */
	@Nullable
	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	/**
	 * 设置默认的最大文本消息缓冲区大小。
	 * 对应于 {@link javax.websocket.server.ServerContainer#setDefaultMaxTextMessageBufferSize(int)} 的配置选项。
	 *
	 * @param bufferSize 最大文本消息缓冲区大小
	 */
	public void setMaxTextMessageBufferSize(Integer bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	/**
	 * 获取默认的最大文本消息缓冲区大小。
	 *
	 * @return 最大文本消息缓冲区大小，可能为 null
	 */
	@Nullable
	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	/**
	 * 设置默认的最大二进制消息缓冲区大小。
	 * 对应于 {@link javax.websocket.server.ServerContainer#setDefaultMaxBinaryMessageBufferSize(int)} 的配置选项。
	 *
	 * @param bufferSize 最大二进制消息缓冲区大小
	 */
	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	/**
	 * 获取默认的最大二进制消息缓冲区大小。
	 *
	 * @return 最大二进制消息缓冲区大小，可能为 null
	 */
	@Nullable
	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	/**
	 * 使用 Tomcat 进行 WebSocket 升级的方法。
	 *
	 * @param exchange             ServerWebExchange 对象，包含 HTTP 请求和响应信息
	 * @param handler              WebSocketHandler 对象，处理 WebSocket 连接的逻辑
	 * @param subProtocol          可选的子协议
	 * @param handshakeInfoFactory 用于创建 HandshakeInfo 对象的 Supplier
	 * @return 代表 WebSocket 连接的 Mono
	 */
	@SuppressWarnings("deprecation")  // 用于 Tomcat 9.0.55 中旧版 doUpgrade 方法
	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
							  @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		// 获取 HTTP 请求和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		// 获取底层的 Tomcat HTTP 请求和响应对象
		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);

		// 获取握手信息和数据缓冲工厂
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory bufferFactory = response.bufferFactory();

		// 触发 WebFlux preCommit 操作并执行升级
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					// 创建 StandardWebSocketHandlerAdapter，并传入 WebSocketHandler 和上下文视图
					Endpoint endpoint = new StandardWebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new TomcatWebSocketSession(session, handshakeInfo, bufferFactory));

					// 获取请求 URI，并创建 DefaultServerEndpointConfig 对象
					String requestURI = servletRequest.getRequestURI();
					DefaultServerEndpointConfig config = new DefaultServerEndpointConfig(requestURI, endpoint);
					// 设置子协议
					config.setSubprotocols(subProtocol != null ?
							Collections.singletonList(subProtocol) : Collections.emptyList());

					// 获取 Tomcat WsServerContainer，并执行 WebSocket 升级
					WsServerContainer container = getContainer(servletRequest);
					try {
						container.doUpgrade(servletRequest, servletResponse, config, Collections.emptyMap());
					} catch (Exception ex) {
						return Mono.error(ex);
					}
					return Mono.empty();
				}));
	}

	/**
	 * 获取 WsServerContainer 对象。
	 * 如果当前实例中的 serverContainer 为 null，则从 ServletContext 中获取 'javax.websocket.server.ServerContainer' 属性，
	 * 并初始化 serverContainer。
	 *
	 * @param request HttpServletRequest 对象
	 * @return 获取到的 WsServerContainer 对象
	 * @throws IllegalStateException 如果 ServletContext 属性中未找到 'javax.websocket.server.ServerContainer'，抛出异常
	 */
	private WsServerContainer getContainer(HttpServletRequest request) {
		// 如果当前实例中的 serverContainer 为 null
		if (this.serverContainer == null) {
			// 从 ServletContext 中获取 'javax.websocket.server.ServerContainer' 属性
			Object container = request.getServletContext().getAttribute(SERVER_CONTAINER_ATTR);
			// 确保获取到的 container 是 WsServerContainer 类型
			Assert.state(container instanceof WsServerContainer,
					"ServletContext attribute 'javax.websocket.server.ServerContainer' not found.");
			// 将获取到的 container 强制转换为 WsServerContainer 类型，并初始化 serverContainer
			this.serverContainer = (WsServerContainer) container;
			// 初始化 serverContainer
			initServerContainer(this.serverContainer);
		}
		// 返回获取到的 WsServerContainer 对象
		return this.serverContainer;
	}


	/**
	 * 初始化 ServerContainer 对象的属性。
	 * 根据当前类中对应的属性值设置 ServerContainer 对象的属性。
	 *
	 * @param serverContainer 要初始化的 ServerContainer 对象
	 */
	private void initServerContainer(ServerContainer serverContainer) {
		// 如果 asyncSendTimeout 不为 null，则设置异步发送超时时间
		if (this.asyncSendTimeout != null) {
			serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		// 如果 maxSessionIdleTimeout 不为 null，则设置默认的最大会话空闲时间
		if (this.maxSessionIdleTimeout != null) {
			serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		// 如果 maxTextMessageBufferSize 不为 null，则设置默认的最大文本消息缓冲区大小
		if (this.maxTextMessageBufferSize != null) {
			serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		// 如果 maxBinaryMessageBufferSize 不为 null，则设置默认的最大二进制消息缓冲区大小
		if (this.maxBinaryMessageBufferSize != null) {
			serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}

}
