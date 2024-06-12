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

package org.springframework.http.client.reactive;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactive.ReactiveEntityProducer;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.Function;

import static org.springframework.http.MediaType.ALL_VALUE;

/**
 * Apache HttpComponents HttpClient 5.x 的 {@link ClientHttpRequest} 实现。
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 5.3
 */
class HttpComponentsClientHttpRequest extends AbstractClientHttpRequest {

	/**
	 * 用于发送HTTP请求的HTTP请求对象。
	 */
	private final HttpRequest httpRequest;

	/**
	 * 用于创建数据缓冲区的数据缓冲区工厂。
	 */
	private final DataBufferFactory dataBufferFactory;

	/**
	 * HttpClientContext对象，包含HTTP客户端的状态和上下文信息。
	 */
	private final HttpClientContext context;

	/**
	 * 用于异步传输字节缓冲区的Flux。
	 */
	@Nullable
	private Flux<ByteBuffer> byteBufferFlux;

	/**
	 * 内容长度，初始值为-1，表示长度未知。
	 */
	private transient long contentLength = -1;


	public HttpComponentsClientHttpRequest(HttpMethod method, URI uri, HttpClientContext context,
										   DataBufferFactory dataBufferFactory) {

		this.context = context;
		this.httpRequest = new BasicHttpRequest(method.name(), uri);
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public HttpMethod getMethod() {
		HttpMethod method = HttpMethod.resolve(this.httpRequest.getMethod());
		Assert.state(method != null, "Method must not be null");
		return method;
	}

	@Override
	public URI getURI() {
		try {
			// 获取URI
			return this.httpRequest.getUri();
		} catch (URISyntaxException ex) {
			// 获取不到的抛出非法参数异常
			throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
		}
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.httpRequest;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// 执行提交操作，并在提交时设置响应体的字节缓冲流
		return doCommit(() -> {
			// 将响应体转换为字节缓冲流，并存储到 byteBufferFlux 中
			this.byteBufferFlux = Flux.from(body).map(DataBuffer::asByteBuffer);
			// 返回一个空的 Mono 信号
			return Mono.empty();
		});
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(Function.identity()));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit();
	}

	@Override
	protected void applyHeaders() {
		// 获取响应头
		HttpHeaders headers = getHeaders();

		// 遍历响应头中的键值对，并将除了 CONTENT_LENGTH 键的其他键值对添加到 HTTP 请求头中
		headers.entrySet()
				.stream()
				.filter(entry -> !HttpHeaders.CONTENT_LENGTH.equals(entry.getKey()))
				.forEach(entry -> entry.getValue().forEach(v -> this.httpRequest.addHeader(entry.getKey(), v)));

		// 如果 HTTP 请求头中不包含 ACCEPT 键，则向 HTTP 请求头中添加 ACCEPT 键，并设置其值为 "*/*"
		if (!this.httpRequest.containsHeader(HttpHeaders.ACCEPT)) {
			this.httpRequest.addHeader(HttpHeaders.ACCEPT, ALL_VALUE);
		}

		// 获取响应头中的 CONTENT_LENGTH 值，并存储到 contentLength 变量中
		this.contentLength = headers.getContentLength();
	}

	@Override
	protected void applyCookies() {
		// 如果响应中没有 Cookie，则直接返回
		if (getCookies().isEmpty()) {
			return;
		}

		// 获取 HTTP 上下文中的 CookieStore
		CookieStore cookieStore = this.context.getCookieStore();

		// 遍历响应中的所有 Cookie，并将其添加到 CookieStore 中
		getCookies().values()
				.stream()
				.flatMap(Collection::stream)
				.forEach(cookie -> {
					// 创建一个基本的客户端 Cookie，并设置其属性
					BasicClientCookie clientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
					clientCookie.setDomain(getURI().getHost());
					clientCookie.setPath(getURI().getPath());
					// 将客户端 Cookie 添加到 CookieStore 中
					cookieStore.addCookie(clientCookie);
				});
	}

	@Override
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(new HttpComponentsHeadersAdapter(this.httpRequest));
	}

	public AsyncRequestProducer toRequestProducer() {
		// 初始化响应实体生产者为 null
		ReactiveEntityProducer reactiveEntityProducer = null;

		// 如果有字节缓冲流存在
		if (this.byteBufferFlux != null) {
			// 获取响应头中的内容编码和内容类型
			String contentEncoding = getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
			ContentType contentType = null;
			if (getHeaders().getContentType() != null) {
				contentType = ContentType.parse(getHeaders().getContentType().toString());
			}
			// 创建响应实体生产者
			reactiveEntityProducer = new ReactiveEntityProducer(
					this.byteBufferFlux, this.contentLength, contentType, contentEncoding);
		}

		// 创建基本请求生产者，并将其与响应实体生产者关联
		return new BasicRequestProducer(this.httpRequest, reactiveEntityProducer);
	}

}
