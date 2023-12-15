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
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.websocket.WebsocketInbound;

import java.net.URI;
import java.util.function.Supplier;

/**
 * 用于Reactor Netty的 {@link WebSocketClient} 实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketClient implements WebSocketClient {

	// 日志记录器
	private static final Log logger = LogFactory.getLog(ReactorNettyWebSocketClient.class);

	// HTTP 客户端
	private final HttpClient httpClient;

	// WebsocketClientSpec 构建器的供应者
	private final Supplier<WebsocketClientSpec.Builder> specBuilderSupplier;

	// 最大帧载荷长度（可为空）
	@Nullable
	private Integer maxFramePayloadLength;

	// 是否处理 Ping 消息（可为空）
	@Nullable
	private Boolean handlePing;

	/**
	 * 默认构造函数。
	 */
	public ReactorNettyWebSocketClient() {
		this(HttpClient.create());
	}

	/**
	 * 构造函数，接受现有的 {@link HttpClient} 构建器和默认的 {@link reactor.netty.http.client.WebsocketClientSpec.Builder}。
	 *
	 * @since 5.1
	 */
	public ReactorNettyWebSocketClient(HttpClient httpClient) {
		this(httpClient, WebsocketClientSpec.builder());
	}

	/**
	 * 构造函数，接受现有的 {@link HttpClient} 构建器和预配置的 {@link reactor.netty.http.client.WebsocketClientSpec.Builder}。
	 *
	 * @since 5.3
	 */
	public ReactorNettyWebSocketClient(
			HttpClient httpClient, Supplier<WebsocketClientSpec.Builder> builderSupplier) {

		Assert.notNull(httpClient, "HttpClient is required");
		Assert.notNull(builderSupplier, "WebsocketClientSpec.Builder is required");
		this.httpClient = httpClient;
		this.specBuilderSupplier = builderSupplier;
	}


	/**
	 * 返回配置的 {@link HttpClient}。
	 *
	 * @return 配置的 {@link HttpClient} 实例
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 构建一个反映当前配置的 {@code WebsocketClientSpec} 实例。
	 * 这可用于检查配置的参数，但子协议取决于用于给定升级的 {@link WebSocketHandler}。
	 *
	 * @since 5.3
	 */
	public WebsocketClientSpec getWebsocketClientSpec() {
		return buildSpec(null);
	}

	/**
	 * 构建一个 {@code WebsocketClientSpec} 实例，反映当前配置。
	 * 这个方法可用于构建用于连接的特定规范。
	 *
	 * @param protocols 协议列表（可为空）
	 * @return 根据当前配置构建的 {@code WebsocketClientSpec} 实例
	 */
	private WebsocketClientSpec buildSpec(@Nullable String protocols) {
		// 获取 WebsocketClientSpec.Builder 实例
		WebsocketClientSpec.Builder builder = this.specBuilderSupplier.get();

		// 如果协议列表不为空，则配置协议
		if (StringUtils.hasText(protocols)) {
			builder.protocols(protocols);
		}

		// 如果最大帧载荷长度不为空，则配置最大帧载荷长度
		if (this.maxFramePayloadLength != null) {
			builder.maxFramePayloadLength(this.maxFramePayloadLength);
		}

		// 如果处理 Ping 消息不为空，则配置处理 Ping 消息
		if (this.handlePing != null) {
			builder.handlePing(this.handlePing);
		}

		// 构建 WebsocketClientSpec 实例
		return builder.build();

	}

	/**
	 * 配置最大允许的帧载荷长度。
	 * 将此值设置为您的应用程序需求可能会减少使用长数据帧的拒绝服务攻击。
	 * <p>对应于 Netty 中 {@link io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
	 * WebSocketServerHandshakerFactory} 构造函数中具有相同名称的参数。
	 * <p>默认设置为 65536（64K）。
	 *
	 * @param maxFramePayloadLength 帧的最大长度
	 * @since 5.2
	 * @deprecated 自 5.3 起弃用，推荐使用提供构造参数的 {@link reactor.netty.http.client.WebsocketClientSpec.Builder} 的供应者
	 */
	@Deprecated
	public void setMaxFramePayloadLength(int maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * 返回配置的 {@link #setMaxFramePayloadLength(int) maxFramePayloadLength}。
	 *
	 * @return 配置的最大帧载荷长度
	 * @since 5.2
	 * @deprecated 自 5.3 起弃用，推荐使用 {@link #getWebsocketClientSpec()}
	 */
	@Deprecated
	public int getMaxFramePayloadLength() {
		return getWebsocketClientSpec().maxFramePayloadLength();
	}

	/**
	 * 配置是否允许 Ping 帧通过以供 {@link WebSocketHandler} 处理。
	 * 默认情况下，Reactor Netty 在收到 Ping 后自动回复 Pong 帧。这在代理中允许 Ping 和 Pong 帧通过时很有用。
	 * <p>默认情况下设置为 {@code false}，此时 Reactor Netty 自动处理 Ping 帧。
	 * 如果设置为 {@code true}，则 Ping 帧将传递到 {@link WebSocketHandler}。
	 *
	 * @param handlePing 是否允许处理 Ping 帧
	 * @since 5.2.4
	 * @deprecated 自 5.3 起弃用，推荐使用提供构造参数的 {@link reactor.netty.http.client.WebsocketClientSpec.Builder} 的供应者
	 */
	@Deprecated
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * 返回配置的 {@link #setHandlePing(boolean)}。
	 *
	 * @return 配置的是否处理 Ping 帧
	 * @since 5.2.4
	 * @deprecated 自 5.3 起弃用，推荐使用 {@link #getWebsocketClientSpec()}
	 */
	@Deprecated
	public boolean getHandlePing() {
		return getWebsocketClientSpec().handlePing();
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
	 * @param url WebSocket 连接的 URI
	 * @param requestHeaders 请求头
	 * @param handler WebSocket 处理器
	 * @return 表示 WebSocket 连接结果的 Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		// 将处理器的子协议转换为逗号分隔的字符串
		String protocols = StringUtils.collectionToCommaDelimitedString(handler.getSubProtocols());
		return getHttpClient()
				// 设置请求头
				.headers(nettyHeaders -> setNettyHeaders(requestHeaders, nettyHeaders))
				// 执行 WebSocket 连接
				.websocket(buildSpec(protocols))
				// 设置连接的 URI
				.uri(url.toString())
				// 处理 WebSocket 连接
				.handle((inbound, outbound) -> {
					// 获取入站消息的响应头，并从中获取协议信息
					HttpHeaders responseHeaders = toHttpHeaders(inbound);
					String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
					// 构建握手信息
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
					// 创建 NettyDataBufferFactory 实例
					NettyDataBufferFactory factory = new NettyDataBufferFactory(outbound.alloc());
					// 创建 ReactorNettyWebSocketSession 实例
					WebSocketSession session = new ReactorNettyWebSocketSession(
							inbound, outbound, info, factory, getMaxFramePayloadLength());
					// 在调试模式下记录连接开始信息
					if (logger.isDebugEnabled()) {
						logger.debug("Started session '" + session.getId() + "' for " + url);
					}
					// 处理 WebSocket 会话
					return handler.handle(session).checkpoint(url + " [ReactorNettyWebSocketClient]");
				})
				// 在发起请求时记录连接信息
				.doOnRequest(n -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
				})
				// 返回表示连接结果的 Mono<Void>
				.next();
	}


	/**
	 * 将 HttpHeaders 对象的内容设置到 Netty 的 HttpHeaders 中。
	 *
	 * @param httpHeaders  要设置的 HttpHeaders 对象
	 * @param nettyHeaders 目标 Netty HttpHeaders
	 */
	private void setNettyHeaders(HttpHeaders httpHeaders, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
		httpHeaders.forEach(nettyHeaders::set);
	}


	/**
	 * 将 WebSocket 入站消息的标头转换为 HttpHeaders 对象。
	 *
	 * @param inbound WebSocket 入站消息
	 * @return 转换后的 HttpHeaders 对象
	 */
	private HttpHeaders toHttpHeaders(WebsocketInbound inbound) {
		HttpHeaders headers = new HttpHeaders();
		io.netty.handler.codec.http.HttpHeaders nettyHeaders = inbound.headers();
		nettyHeaders.forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, nettyHeaders.getAll(name));
		});
		return headers;
	}


}
