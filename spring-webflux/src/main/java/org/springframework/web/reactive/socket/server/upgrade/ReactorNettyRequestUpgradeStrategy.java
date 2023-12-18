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

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;

import java.net.URI;
import java.util.function.Supplier;

/**
 * 用于 Reactor Netty 的 {@link RequestUpgradeStrategy} 实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	/**
	 * WebsocketServerSpec.Builder 的 Supplier
	 */
	private final Supplier<WebsocketServerSpec.Builder> specBuilderSupplier;

	/**
	 * 最大帧负载长度
	 */
	@Nullable
	private Integer maxFramePayloadLength;

	/**
	 * 是否处理 Ping 消息
	 */
	@Nullable
	private Boolean handlePing;


	/**
	 * 使用默认的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder} 创建实例。
	 *
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy() {
		this(WebsocketServerSpec::builder);
	}

	/**
	 * 使用预配置的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder} 创建实例，
	 * 用于 WebSocket 升级。
	 *
	 * @param builderSupplier 预配置的 WebsocketServerSpec.Builder 的 Supplier
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy(Supplier<WebsocketServerSpec.Builder> builderSupplier) {
		Assert.notNull(builderSupplier, "WebsocketServerSpec.Builder is required");
		this.specBuilderSupplier = builderSupplier;
	}

	/**
	 * 构建 {@code WebsocketServerSpec} 实例，反映当前的配置。
	 * <p>
	 * 这可用于检查已配置的参数，但不包括子协议，子协议取决于用于给定升级的 {@link WebSocketHandler}。
	 *
	 * @return 反映当前配置的 WebsocketServerSpec
	 * @since 5.2.6
	 */
	public WebsocketServerSpec getWebsocketServerSpec() {
		return buildSpec(null);
	}

	/**
	 * 构建 WebsocketServerSpec 实例，根据当前配置设置参数。
	 *
	 * @param subProtocol 子协议，如果为 null 则不设置
	 * @return 构建的 WebsocketServerSpec 实例
	 */
	WebsocketServerSpec buildSpec(@Nullable String subProtocol) {
		WebsocketServerSpec.Builder builder = this.specBuilderSupplier.get();
		// 如果子协议不为 null，则设置协议
		if (subProtocol != null) {
			builder.protocols(subProtocol);
		}
		// 如果最大帧负载长度不为 null，则设置最大帧负载长度
		if (this.maxFramePayloadLength != null) {
			builder.maxFramePayloadLength(this.maxFramePayloadLength);
		}
		// 如果 handlePing 不为 null，则设置 handlePing
		if (this.handlePing != null) {
			builder.handlePing(this.handlePing);
		}
		// 构建 WebsocketServerSpec 实例并返回
		return builder.build();
	}


	/**
	 * 配置最大允许的帧负载长度。将此值设置为应用程序的要求可能会减少使用长数据帧的拒绝服务攻击。
	 * <p>对应于 Netty 中 {@link io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
	 * WebSocketServerHandshakerFactory} 构造函数中具有相同名称的参数。
	 * <p>默认设置为 65536（64K）。
	 *
	 * @param maxFramePayloadLength 帧的最大长度
	 * @since 5.1
	 * @deprecated 自 5.2.6 起不推荐使用，建议使用提供带有构造函数参数的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder} 的供应商
	 */
	@Deprecated
	public void setMaxFramePayloadLength(Integer maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * 返回配置的帧的最大长度。
	 *
	 * @return 帧的最大长度
	 * @since 5.1
	 * @deprecated 自 5.2.6 起不推荐使用，建议使用 {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public int getMaxFramePayloadLength() {
		return getWebsocketServerSpec().maxFramePayloadLength();
	}

	/**
	 * 配置是否允许 Ping 帧通过并由升级方法给定的 {@link WebSocketHandler} 处理。
	 * 默认情况下，Reactor Netty 会自动回复 Pong 帧以响应 Ping。
	 * 这在代理中很有用，允许通过 Ping 和 Pong 帧。
	 * <p>默认情况下，此设置为 {@code false}，在这种情况下，Ping 帧会被 Reactor Netty 自动处理。
	 * 如果设置为 {@code true}，Ping 帧将被传递给 {@link WebSocketHandler}。
	 *
	 * @param handlePing 是否允许处理 Ping 帧
	 * @since 5.2.4
	 * @deprecated 自 5.2.6 起不推荐使用，建议使用提供带有构造函数参数的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder} 的供应商
	 */
	@Deprecated
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * 返回配置的 {@link #setHandlePing(boolean)}。
	 *
	 * @return 是否允许处理 Ping 帧
	 * @since 5.2.4
	 * @deprecated 自 5.2.6 起不推荐使用，建议使用 {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public boolean getHandlePing() {
		return getWebsocketServerSpec().handlePing();
	}

	/**
	 * 使用 Reactor Netty 进行 WebSocket 升级的方法。
	 *
	 * @param exchange             ServerWebExchange 对象，包含 HTTP 请求和响应信息
	 * @param handler              WebSocketHandler 对象，处理 WebSocket 连接的逻辑
	 * @param subProtocol          可选的子协议
	 * @param handshakeInfoFactory 用于创建 HandshakeInfo 对象的 Supplier
	 * @return 代表 WebSocket 连接的 Mono
	 */
	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
							  @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		// 获取 HTTP 响应对象
		ServerHttpResponse response = exchange.getResponse();
		// 获取底层的 Reactor Netty HTTP 响应对象
		HttpServerResponse reactorResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		// 获取握手信息和数据缓冲工厂
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();
		// 获取请求的 URI
		URI uri = exchange.getRequest().getURI();

		// 触发 WebFlux preCommit 操作并执行升级
		return response.setComplete()
				.then(Mono.defer(() -> {
					// 构建 WebSocketServerSpec 对象
					WebsocketServerSpec spec = buildSpec(subProtocol);
					// 使用 Reactor Netty 的 sendWebsocket 方法执行 WebSocket 升级
					return reactorResponse.sendWebsocket((in, out) -> {
						// 创建 ReactorNettyWebSocketSession，并传入输入输出流、握手信息、数据缓冲工厂和最大帧负载长度
						ReactorNettyWebSocketSession session = new ReactorNettyWebSocketSession(
								in, out, handshakeInfo, bufferFactory, spec.maxFramePayloadLength());
						// 调用 WebSocketHandler 处理 WebSocket 会话，并返回处理的结果
						return handler.handle(session).checkpoint(uri + " [ReactorNettyRequestUpgradeStrategy]");
					}, spec);
				}));
	}


}
