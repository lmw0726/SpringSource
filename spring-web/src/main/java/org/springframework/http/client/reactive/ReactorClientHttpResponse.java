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

package org.springframework.http.client.reactive;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Reactor-Netty HTTP 客户端的 {@link ClientHttpResponse} 实现。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see reactor.netty.http.client.HttpClient
 * @since 5.0
 */
class ReactorClientHttpResponse implements ClientHttpResponse {

	/**
	 * Reactor Netty 1.0.5+。
	 * 是否存在Reactor Netty请求通道操作Id
	 */
	static final boolean reactorNettyRequestChannelOperationsIdPresent = ClassUtils.isPresent(
			"reactor.netty.ChannelOperationsId", ReactorClientHttpResponse.class.getClassLoader());

	/**
	 * 日志记录器。
	 */
	private static final Log logger = LogFactory.getLog(ReactorClientHttpResponse.class);

	/**
	 * HTTP 客户端响应对象。
	 */
	private final HttpClientResponse response;

	/**
	 * HTTP 响应的头部信息。
	 */
	private final HttpHeaders headers;

	/**
	 * Netty 入站数据流。
	 */
	private final NettyInbound inbound;

	/**
	 * Netty 数据缓冲区工厂。
	 */
	private final NettyDataBufferFactory bufferFactory;

	/**
	 * 订阅状态：
	 * 0 - 未订阅
	 * 1 - 已订阅
	 * 2 - 通过连接器取消订阅（在订阅之前）
	 */
	private final AtomicInteger state = new AtomicInteger();


	/**
	 * 从 {@link reactor.netty.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)} 方法提取的输入匹配的构造函数。
	 *
	 * @param response   客户端响应
	 * @param connection 连接
	 * @since 5.2.8
	 */
	public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
		this.response = response;
		MultiValueMap<String, String> adapter = new NettyHeadersAdapter(response.responseHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
		this.inbound = connection.inbound();
		this.bufferFactory = new NettyDataBufferFactory(connection.outbound().alloc());
	}

	/**
	 * 使用从 {@link Connection} 提取的输入的构造函数。
	 *
	 * @param response 客户端响应
	 * @param inbound  传入的 Netty 流
	 * @param alloc    ByteBuf 分配器
	 * @deprecated 自 5.2.8 起，使用 {@link #ReactorClientHttpResponse(HttpClientResponse, Connection)} 替代
	 */
	@Deprecated
	public ReactorClientHttpResponse(HttpClientResponse response, NettyInbound inbound, ByteBufAllocator alloc) {
		this.response = response;
		MultiValueMap<String, String> adapter = new NettyHeadersAdapter(response.responseHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
		this.inbound = inbound;
		this.bufferFactory = new NettyDataBufferFactory(alloc);
	}


	@Override
	public String getId() {
		// 初始化变量id为null
		String id = null;

		// 如果reactorNettyRequestChannelOperationsIdPresent为true
		if (reactorNettyRequestChannelOperationsIdPresent) {
			// 从response中获取channel操作的id
			id = ChannelOperationsIdHelper.getId(this.response);
		}

		// 如果id仍然为null，并且response是Connection的实例
		if (id == null && this.response instanceof Connection) {
			// 从Connection的channel中获取id的短文本形式
			id = ((Connection) this.response).channel().id().asShortText();
		}

		// 如果id不为null，则返回id，否则返回该对象的十六进制标识字符串
		return (id != null ? id : ObjectUtils.getIdentityHexString(this));
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// 返回传入数据流，并在订阅时执行操作
		return this.inbound.receive()
				.doOnSubscribe(s -> {
					// 当订阅时执行的操作
					if (this.state.compareAndSet(0, 1)) {
						// 如果当前状态从0变为1
						// 直接返回，继续执行
						return;
					}
					if (this.state.get() == 2) {
						// 如果当前状态为2
						// 抛出异常，表示客户端响应已经因为取消操作而释放了响应体
						throw new IllegalStateException(
								"The client response body has been released already due to cancellation.");
					}
				})
				// 映射字节缓冲，每个字节缓冲都会被保留
				.map(byteBuf -> {
					// 保留字节缓冲
					byteBuf.retain();
					// 使用缓冲工厂将字节缓冲包装为数据缓冲
					return this.bufferFactory.wrap(byteBuf);
				});
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.response.status().code();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		// 创建一个LinkedMultiValueMap来存储转换后的ResponseCookie对象
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();

		// 遍历响应中的所有Cookie
		this.response.cookies().values().stream()
				.flatMap(Collection::stream)
				.forEach(cookie -> result.add(cookie.name(),
						// 将ClientResponse中的Cookie转换为ResponseCookie对象，并设置其属性
						ResponseCookie.fromClientResponse(cookie.name(), cookie.value())
								.domain(cookie.domain())
								.path(cookie.path())
								.maxAge(cookie.maxAge())
								.secure(cookie.isSecure())
								.httpOnly(cookie.isHttpOnly())
								.sameSite(getSameSite(cookie))
								.build()));

		// 返回不可修改的MultiValueMap
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Nullable
	private static String getSameSite(Cookie cookie) {
		// 检查给定的Cookie是否是DefaultCookie类型
		if (cookie instanceof DefaultCookie) {
			// 将Cookie转换为DefaultCookie类型
			DefaultCookie defaultCookie = (DefaultCookie) cookie;
			// 如果DefaultCookie的SameSite属性不为空
			if (defaultCookie.sameSite() != null) {
				// 返回SameSite属性的名称
				return defaultCookie.sameSite().name();
			}
		}
		// 如果不满足以上条件，则返回空
		return null;
	}

	/**
	 * 当检测到取消操作但内容尚未订阅时，由 {@link ReactorClientHttpConnector} 调用。
	 * 如果订阅从未实现，那么内容将保持未被清空状态。
	 * 或者，如果取消操作发生得非常早，或者由于某些原因导致响应读取被延迟，那么内容仍然可能会被清空。
	 *
	 * @param method 请求方法
	 */
	void releaseAfterCancel(HttpMethod method) {
		// 检查请求方法是否可能具有请求体，并且当前状态是否可以从0变为2
		if (mayHaveBody(method) && this.state.compareAndSet(0, 2)) {
			// 如果日志级别为DEBUG，则打印释放请求体的信息
			if (logger.isDebugEnabled()) {
				logger.debug("[" + getId() + "]" + "Releasing body, not yet subscribed.");
			}
			// 接收传入的数据流，并对每个字节缓冲进行处理
			this.inbound.receive().doOnNext(byteBuf -> {
			}).subscribe(byteBuf -> {
			}, ex -> {
			});
		}
	}

	private boolean mayHaveBody(HttpMethod method) {
		// 获取原始的HTTP状态码
		int code = this.getRawStatusCode();

		// 返回值，表示响应是否有内容
		// 如果状态码在100到199之间，或者状态码为204或205，或者请求方法为HEAD，
		// 或者响应头中的内容长度为0，则返回true，否则返回false
		return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
				method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
				"status=" + getRawStatusCode() + '}';
	}


	private static class ChannelOperationsIdHelper {

		@Nullable
		public static String getId(HttpClientResponse response) {
			// 检查 响应 是否是 reactor.netty.ChannelOperationsId 类型的实例
			if (response instanceof reactor.netty.ChannelOperationsId) {
				// 如果日志级别为调试级别，则返回长文本形式的 ChannelOperationsId，否则返回短文本形式的 ChannelOperationsId
				return (logger.isDebugEnabled() ?
						((reactor.netty.ChannelOperationsId) response).asLongText() :
						((reactor.netty.ChannelOperationsId) response).asShortText());
			}
			// 如果 响应 不是 ChannelOperationsId 类型的实例，则返回 null
			return null;
		}
	}

}
