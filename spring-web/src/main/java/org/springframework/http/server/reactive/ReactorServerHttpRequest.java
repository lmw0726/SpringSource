/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.server.reactive;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.ssl.SslHandler;
import org.apache.commons.logging.Log;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpLogging;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerRequest;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 将 {@link ServerHttpRequest} 适配为 Reactor {@link HttpServerRequest}。
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactorServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * Reactor Netty 1.0.5+。
	 */
	static final boolean reactorNettyRequestChannelOperationsIdPresent = ClassUtils.isPresent(
			"reactor.netty.ChannelOperationsId", ReactorServerHttpRequest.class.getClassLoader());
	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(ReactorServerHttpRequest.class);

	/**
	 * 日志前缀索引
	 */
	private static final AtomicLong logPrefixIndex = new AtomicLong();

	/**
	 * 服务端请求
	 */
	private final HttpServerRequest request;

	/**
	 * Netty数据缓冲区工厂
	 */
	private final NettyDataBufferFactory bufferFactory;


	public ReactorServerHttpRequest(HttpServerRequest request, NettyDataBufferFactory bufferFactory)
			throws URISyntaxException {

		super(initUri(request), "", new NettyHeadersAdapter(request.requestHeaders()));
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		this.request = request;
		this.bufferFactory = bufferFactory;
	}

	private static URI initUri(HttpServerRequest request) throws URISyntaxException {
		Assert.notNull(request, "HttpServerRequest must not be null");
		return new URI(resolveBaseUrl(request) + resolveRequestUri(request));
	}

	private static URI resolveBaseUrl(HttpServerRequest request) throws URISyntaxException {
		// 获取请求的方案（scheme）
		String scheme = getScheme(request);
		// 获取请求头中的Host
		String header = request.requestHeaders().get(HttpHeaderNames.HOST);
		// 如果Host头部不为空
		if (header != null) {
			final int portIndex;
			// 确定端口的索引位置
			if (header.startsWith("[")) {
				portIndex = header.indexOf(':', header.indexOf(']'));
			} else {
				portIndex = header.indexOf(':');
			}
			// 如果找到端口索引
			if (portIndex != -1) {
				try {
					// 解析主机名和端口号，并创建URI对象
					return new URI(scheme, null, header.substring(0, portIndex),
							Integer.parseInt(header.substring(portIndex + 1)), null, null, null);
				} catch (NumberFormatException ex) {
					// 如果端口号解析失败，抛出URISyntaxException异常
					throw new URISyntaxException(header, "Unable to parse port", portIndex);
				}
			} else {
				// 如果没有找到端口索引，创建URI对象
				return new URI(scheme, header, null, null);
			}
		} else {
			// 如果Host头部为空，获取本地地址和端口，并创建URI对象
			InetSocketAddress localAddress = request.hostAddress();
			Assert.state(localAddress != null, "No host address available");
			return new URI(scheme, null, localAddress.getHostString(),
					localAddress.getPort(), null, null, null);
		}
	}

	private static String getScheme(HttpServerRequest request) {
		return request.scheme();
	}

	private static String resolveRequestUri(HttpServerRequest request) {
		// 获取请求的URI
		String uri = request.uri();
		// 遍历URI的字符
		for (int i = 0; i < uri.length(); i++) {
			char c = uri.charAt(i);
			// 如果遇到'/'、'?'或'#'，停止遍历
			if (c == '/' || c == '?' || c == '#') {
				break;
			}
			// 如果遇到':'并且后面两个字符是'//'，说明是完整的URL
			if (c == ':' && (i + 2 < uri.length())) {
				if (uri.charAt(i + 1) == '/' && uri.charAt(i + 2) == '/') {
					// 查找完整URL后面的路径部分并返回
					// 从当前位置的下一个位置开始遍历URI的字符
					for (int j = i + 3; j < uri.length(); j++) {
						// 获取当前字符
						c = uri.charAt(j);
						// 如果当前字符是'/'、'?'或'#'，表示找到了路径分隔符或查询参数分隔符或片段标识符
						if (c == '/' || c == '?' || c == '#') {
							// 返回从当前位置(j)开始到末尾的子字符串
							return uri.substring(j);
						}
					}
					return "";
				}
			}
		}
		// 如果没有找到完整的URL，返回整个URI
		return uri;
	}


	@Override
	public String getMethodValue() {
		return this.request.method().name();
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		// 创建一个多值映射，用于存储HTTP Cookie
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		// 遍历请求中的所有Cookie名称
		for (CharSequence name : this.request.cookies().keySet()) {
			// 遍历每个Cookie的值
			for (Cookie cookie : this.request.cookies().get(name)) {
				// 创建HttpCookie对象，并添加到多值映射中
				HttpCookie httpCookie = new HttpCookie(name.toString(), cookie.value());
				cookies.add(name.toString(), httpCookie);
			}
		}
		// 返回填充好的多值映射
		return cookies;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.request.hostAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.request.remoteAddress();
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		// 获取请求的连接的通道
		Channel channel = ((Connection) this.request).channel();
		// 从通道的管道中获取SSL处理器
		SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
		// 如果SSL处理器为空且通道的父级不为空（表示HTTP/2），则从父级管道中获取SSL处理器
		if (sslHandler == null && channel.parent() != null) {
			sslHandler = channel.parent().pipeline().get(SslHandler.class);
		}
		// 如果SSL处理器不为空
		if (sslHandler != null) {
			// 获取SSL会话
			SSLSession session = sslHandler.engine().getSession();
			// 创建并返回默认的SSL信息对象
			return new DefaultSslInfo(session);
		}
		// 如果SSL处理器为空，返回null
		return null;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.request.receive().retain().map(this.bufferFactory::wrap);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	@Override
	@Nullable
	protected String initId() {
		// 如果请求是连接类型
		if (this.request instanceof Connection) {
			// 获取连接的通道ID的短文本形式，并添加日志前缀索引
			return ((Connection) this.request).channel().id().asShortText() +
					"-" + logPrefixIndex.incrementAndGet();
		}
		// 如果请求不是连接类型，返回null
		return null;
	}

	@Override
	protected String initLogPrefix() {
		// 如果存在 Reactor Netty 请求通道操作 ID
		if (reactorNettyRequestChannelOperationsIdPresent) {
			// 获取请求的 ID
			String id = (ChannelOperationsIdHelper.getId(this.request));
			// 如果 ID 不为空，则返回该 ID
			if (id != null) {
				return id;
			}
		}

		// 如果请求是 Connection 类型
		if (this.request instanceof Connection) {
			// 返回连接的通道 ID 的短文本形式加上日志前缀索引的增量
			return ((Connection) this.request).channel().id().asShortText() +
					"-" + logPrefixIndex.incrementAndGet();
		}

		// 返回默认的 ID
		return getId();
	}


	private static class ChannelOperationsIdHelper {

		@Nullable
		public static String getId(HttpServerRequest request) {
			// 如果请求是 Reactor Netty 的 ChannelOperationsId 类型
			if (request instanceof reactor.netty.ChannelOperationsId) {
				// 如果日志级别是调试级别
				return (logger.isDebugEnabled() ?
						// 返回长文本形式的 ChannelOperationsId
						((reactor.netty.ChannelOperationsId) request).asLongText() :
						// 否则返回短文本形式的 ChannelOperationsId
						((reactor.netty.ChannelOperationsId) request).asShortText());
			}
			// 返回空值
			return null;
		}
	}

}
