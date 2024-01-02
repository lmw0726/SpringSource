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

package org.springframework.web.reactive.function.client;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link ClientResponse.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.0.5
 */
final class DefaultClientResponseBuilder implements ClientResponse.Builder {

	/**
	 * 空的Http请求
	 */
	private static final HttpRequest EMPTY_REQUEST = new HttpRequest() {

		private final URI empty = URI.create("");

		@Override
		public String getMethodValue() {
			return "UNKNOWN";
		}

		@Override
		public URI getURI() {
			return this.empty;
		}

		@Override
		public HttpHeaders getHeaders() {
			return HttpHeaders.EMPTY;
		}
	};


	/**
	 * 用于交换的策略。
	 */
	private ExchangeStrategies strategies;

	/**
	 * 状态码，默认为 200。
	 */
	private int statusCode = 200;

	/**
	 * HTTP响应的头信息。
	 */
	@Nullable
	private HttpHeaders headers;

	/**
	 * HTTP响应的Cookie信息。
	 */
	@Nullable
	private MultiValueMap<String, ResponseCookie> cookies;

	/**
	 * HTTP响应的数据体，默认为空的 Flux。
	 */
	private Flux<DataBuffer> body = Flux.empty();

	/**
	 * 原始的客户端响应。
	 */
	@Nullable
	private ClientResponse originalResponse;

	/**
	 * HTTP请求。
	 */
	private HttpRequest request;


	DefaultClientResponseBuilder(ExchangeStrategies strategies) {
		Assert.notNull(strategies, "ExchangeStrategies must not be null");
		this.strategies = strategies;
		this.headers = new HttpHeaders();
		this.cookies = new LinkedMultiValueMap<>();
		this.request = EMPTY_REQUEST;
	}

	/**
	 * 根据给定的 ClientResponse 和布尔值构造 DefaultClientResponseBuilder 对象。
	 *
	 * @param other  给定的 ClientResponse 对象
	 * @param mutate 布尔值，用于决定是否进行改变
	 */
	DefaultClientResponseBuilder(ClientResponse other, boolean mutate) {
		// 确保给定的 ClientResponse 不为空
		Assert.notNull(other, "ClientResponse must not be null");

		// 获取 ClientResponse 的策略并设置到当前对象
		this.strategies = other.strategies();

		// 获取 ClientResponse 的状态码并设置到当前对象
		this.statusCode = other.rawStatusCode();

		if (mutate) {
			// 如果需要改变，则将 ClientResponse 的 body 转换为 Flux<DataBuffer> 并设置到当前对象
			this.body = other.bodyToFlux(DataBuffer.class);
		} else {
			// 如果不需要改变，则创建新的 HttpHeaders 对象，并将原始 ClientResponse 的 headers 添加到其中
			this.headers = new HttpHeaders();
			this.headers.addAll(other.headers().asHttpHeaders());
		}

		// 设置原始的 ClientResponse 到当前对象
		this.originalResponse = other;

		// 获取原始 ClientResponse 的请求或设置空请求到当前对象
		this.request = (other instanceof DefaultClientResponse ?
				((DefaultClientResponse) other).request() : EMPTY_REQUEST);
	}


	@Override
	public DefaultClientResponseBuilder statusCode(HttpStatus statusCode) {
		return rawStatusCode(statusCode.value());
	}

	@Override
	public DefaultClientResponseBuilder rawStatusCode(int statusCode) {
		Assert.isTrue(statusCode >= 100 && statusCode < 600, "StatusCode must be between 1xx and 5xx");
		this.statusCode = statusCode;
		return this;
	}

	@Override
	public ClientResponse.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			getHeaders().add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ClientResponse.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(getHeaders());
		return this;
	}

	@SuppressWarnings("ConstantConditions")
	private HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = HttpHeaders.writableHttpHeaders(this.originalResponse.headers().asHttpHeaders());
		}
		return this.headers;
	}

	@Override
	public DefaultClientResponseBuilder cookie(String name, String... values) {
		for (String value : values) {
			getCookies().add(name, ResponseCookie.from(name, value).build());
		}
		return this;
	}

	@Override
	public ClientResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(getCookies());
		return this;
	}

	@SuppressWarnings("ConstantConditions")
	private MultiValueMap<String, ResponseCookie> getCookies() {
		if (this.cookies == null) {
			this.cookies = new LinkedMultiValueMap<>(this.originalResponse.cookies());
		}
		return this.cookies;
	}

	@Override
	public ClientResponse.Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer) {
		this.body = transformer.apply(this.body);
		return this;
	}

	@Override
	public ClientResponse.Builder body(Flux<DataBuffer> body) {
		Assert.notNull(body, "Body must not be null");
		// 释放当前请求体内容占用的资源。
		releaseBody();
		this.body = body;
		return this;
	}

	/**
	 * 设置请求体内容并返回 Builder 对象。
	 *
	 * @param body 请求体内容
	 * @return 返回设置了请求体内容的 Builder 对象
	 */
	@Override
	public ClientResponse.Builder body(String body) {
		// 确保请求体内容不为空
		Assert.notNull(body, "Body must not be null");
		// 释放现有的请求体内容
		releaseBody();
		// 使用给定的字符串创建 Flux<DataBuffer>
		this.body = Flux.just(body)
				.map(s -> {
					// 将字符串转换为 UTF-8 编码的字节数组
					byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
					// 使用 DefaultDataBufferFactory 创建一个 DataBuffer 包装字节数组
					return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
				});
		// 返回更新后的 Builder 对象
		return this;
	}

	/**
	 * 释放当前请求体内容占用的资源。
	 * 注意：此方法为私有方法，用于释放请求体内容所占用的资源。
	 */
	private void releaseBody() {
		// 订阅当前请求体内容，并使用 DataBufferUtils.releaseConsumer() 方法释放资源
		this.body.subscribe(DataBufferUtils.releaseConsumer());
	}

	@Override
	public ClientResponse.Builder request(HttpRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
		return this;
	}

	/**
	 * 构建并返回一个 ClientResponse 对象。
	 *
	 * @return 构建的 ClientResponse 对象
	 */
	@Override
	public ClientResponse build() {

		// 创建一个 ClientHttpResponse 对象
		ClientHttpResponse httpResponse = new BuiltClientHttpResponse(
				this.statusCode, this.headers, this.cookies, this.body, this.originalResponse);

		// 返回一个新的 DefaultClientResponse 对象
		return new DefaultClientResponse(
				httpResponse, this.strategies,
				this.originalResponse != null ? this.originalResponse.logPrefix() : "",
				this.request.getMethodValue() + " " + this.request.getURI(),
				() -> this.request);
	}


	/**
	 * 内部类实现了 {@link ClientHttpResponse} 接口。
	 */
	private static class BuiltClientHttpResponse implements ClientHttpResponse {

		/**
		 * HTTP响应的状态码。
		 */
		private final int statusCode;

		/**
		 * HTTP响应的头信息。
		 */
		@Nullable
		private final HttpHeaders headers;

		/**
		 * HTTP响应的Cookie信息。
		 */
		@Nullable
		private final MultiValueMap<String, ResponseCookie> cookies;

		/**
		 * HTTP响应的数据体。
		 */
		private final Flux<DataBuffer> body;

		/**
		 * 原始的客户端响应。
		 */
		@Nullable
		private final ClientResponse originalResponse;

		/**
		 * 构造函数，初始化 HTTP 响应的各项属性。
		 *
		 * @param statusCode       HTTP响应的状态码
		 * @param headers          HTTP响应的头信息
		 * @param cookies          HTTP响应的Cookie信息
		 * @param body             HTTP响应的数据体
		 * @param originalResponse 原始的客户端响应
		 */
		BuiltClientHttpResponse(int statusCode, @Nullable HttpHeaders headers,
								@Nullable MultiValueMap<String, ResponseCookie> cookies, Flux<DataBuffer> body,
								@Nullable ClientResponse originalResponse) {

			Assert.isTrue(headers != null || originalResponse != null,
					"Expected either headers or an original response with headers.");

			Assert.isTrue(cookies != null || originalResponse != null,
					"Expected either cookies or an original response with cookies.");

			this.statusCode = statusCode;
			this.headers = (headers != null ? HttpHeaders.readOnlyHttpHeaders(headers) : null);
			this.cookies = (cookies != null ? CollectionUtils.unmodifiableMultiValueMap(cookies) : null);
			this.body = body;
			this.originalResponse = originalResponse;
		}

		@Override
		public HttpStatus getStatusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public int getRawStatusCode() {
			return this.statusCode;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public HttpHeaders getHeaders() {
			return (this.headers != null ? this.headers : this.originalResponse.headers().asHttpHeaders());
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public MultiValueMap<String, ResponseCookie> getCookies() {
			return (this.cookies != null ? this.cookies : this.originalResponse.cookies());
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.body;
		}
	}

}
