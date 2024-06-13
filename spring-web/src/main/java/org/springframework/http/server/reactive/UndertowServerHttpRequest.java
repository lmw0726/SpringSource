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

package org.springframework.http.server.reactive;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.publisher.Flux;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 将 {@link ServerHttpRequest} 适配为 Undertow {@link HttpServerExchange}。
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class UndertowServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * 日志前缀索引
	 */
	private static final AtomicLong logPrefixIndex = new AtomicLong();

	/**
	 * Http服务端交换
	 */
	private final HttpServerExchange exchange;

	/**
	 * 请求体发布器
	 */
	private final RequestBodyPublisher body;


	public UndertowServerHttpRequest(HttpServerExchange exchange, DataBufferFactory bufferFactory)
			throws URISyntaxException {

		super(initUri(exchange), "", new UndertowHeadersAdapter(exchange.getRequestHeaders()));
		this.exchange = exchange;
		this.body = new RequestBodyPublisher(exchange, bufferFactory);
		this.body.registerListeners(exchange);
	}

	private static URI initUri(HttpServerExchange exchange) throws URISyntaxException {
		Assert.notNull(exchange, "HttpServerExchange is required");
		String requestURL = exchange.getRequestURL();
		String query = exchange.getQueryString();
		String requestUriAndQuery = (StringUtils.hasLength(query) ? requestURL + "?" + query : requestURL);
		return new URI(requestUriAndQuery);
	}

	@Override
	public String getMethodValue() {
		return this.exchange.getRequestMethod().toString();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		// 创建一个多值映射，用于存储转换后的 HTTP Cookie
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

		// getRequestCookies() 在Undertow 2.2中已弃用
		// 遍历请求中的所有 Cookie 名称
		for (String name : this.exchange.getRequestCookies().keySet()) {
			// 获取指定名称的 Cookie 对象
			Cookie cookie = this.exchange.getRequestCookies().get(name);
			// 创建对应的 HTTP Cookie 对象
			HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
			// 将 HTTP Cookie 添加到多值映射中，按名称进行分组
			cookies.add(name, httpCookie);
		}

		// 返回转换后的 HTTP Cookie 的多值映射
		return cookies;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.exchange.getDestinationAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.exchange.getSourceAddress();
	}

	@Nullable
	@Override
	protected SslInfo initSslInfo() {
		// 获取与当前连接关联的 SSLSession
		SSLSession session = this.exchange.getConnection().getSslSession();

		// 如果 SSLSession 不为null
		if (session != null) {
			// 返回一个包含 SSLSession 信息的 DefaultSslInfo 对象
			return new DefaultSslInfo(session);
		}

		// 如果 SSLSession 为null，则返回空值
		return null;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.body);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.exchange;
	}

	@Override
	protected String initId() {
		return ObjectUtils.getIdentityHexString(this.exchange.getConnection()) +
				"-" + logPrefixIndex.incrementAndGet();
	}


	private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {

		/**
		 * 输入流源通道。
		 */
		private final StreamSourceChannel channel;

		/**
		 * 数据缓冲区工厂。
		 */
		private final DataBufferFactory bufferFactory;

		/**
		 * 字节缓冲池。
		 */
		private final ByteBufferPool byteBufferPool;

		public RequestBodyPublisher(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
			super(UndertowServerHttpRequest.this.getLogPrefix());
			this.channel = exchange.getRequestChannel();
			this.bufferFactory = bufferFactory;
			this.byteBufferPool = exchange.getConnection().getByteBufferPool();
		}

		private void registerListeners(HttpServerExchange exchange) {
			// 添加交换完成监听器，处理数据全部读取后的操作
			exchange.addExchangeCompleteListener((ex, next) -> {
				// 所有数据读取完成后的回调函数
				onAllDataRead();
				// 继续处理下一个步骤
				next.proceed();
			});

			// 设置通道的读取监听器，处理数据可用时的操作
			this.channel.getReadSetter().set(c -> onDataAvailable());

			// 设置通道的关闭监听器，处理通道关闭时的操作
			this.channel.getCloseSetter().set(c -> onAllDataRead());

			// 恢复通道的读取操作
			this.channel.resumeReads();
		}

		@Override
		protected void checkOnDataAvailable() {
			// 恢复通道的读取操作
			this.channel.resumeReads();

			// 尝试处理数据可用时的操作
			onDataAvailable();
		}

		@Override
		protected void readingPaused() {
			this.channel.suspendReads();
		}

		@Override
		@Nullable
		protected DataBuffer read() throws IOException {
			// 从字节缓冲池分配一个 PooledByteBuffer 对象
			PooledByteBuffer pooledByteBuffer = this.byteBufferPool.allocate();
			try {
				// 获取 ByteBuffer 对象
				ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();
				// 从通道读取数据到 ByteBuffer 中
				int read = this.channel.read(byteBuffer);

				// 如果日志级别为跟踪，则记录读取的字节数
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Read " + read + (read != -1 ? " bytes" : ""));
				}

				// 如果成功读取了数据
				if (read > 0) {
					// 反转 ByteBuffer 准备读取数据
					byteBuffer.flip();
					// 使用缓冲工厂分配一个 DataBuffer 对象
					DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(read);
					// 将 ByteBuffer 中的数据写入到 DataBuffer 中
					dataBuffer.write(byteBuffer);
					// 返回读取的数据缓冲区
					return dataBuffer;
				} else if (read == -1) {
					// 数据已全部读取完成的回调
					onAllDataRead();
				}
				// 返回空值，表示没有读取到数据
				return null;
			} finally {
				// 关闭 PooledByteBuffer 对象，释放资源
				pooledByteBuffer.close();
			}
		}

		@Override
		protected void discardData() {
			// 没有什么可丢弃的，因为我们立即传递数据缓冲区。
		}
	}

}
