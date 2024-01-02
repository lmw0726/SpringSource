/*
 * Copyright 2002-2018 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/**
 * {@link ClientRequest.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
final class DefaultClientRequestBuilder implements ClientRequest.Builder {

	/**
	 * 请求方法
	 */
	private HttpMethod method;

	/**
	 * 请求URL
	 */
	private URI url;

	/**
	 * 请求头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * Cookie
	 */
	private final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

	/**
	 * 属性
	 */
	private final Map<String, Object> attributes = new LinkedHashMap<>();

	/**
	 * 请求体插入器
	 */
	private BodyInserter<?, ? super ClientHttpRequest> body = BodyInserters.empty();

	/**
	 * HTTP请求消费者
	 */
	@Nullable
	private Consumer<ClientHttpRequest> httpRequestConsumer;


	public DefaultClientRequestBuilder(ClientRequest other) {
		Assert.notNull(other, "ClientRequest must not be null");
		this.method = other.method();
		this.url = other.url();
		headers(headers -> headers.addAll(other.headers()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		attributes(attributes -> attributes.putAll(other.attributes()));
		body(other.body());
		this.httpRequestConsumer = other.httpRequest();
	}

	public DefaultClientRequestBuilder(HttpMethod method, URI url) {
		Assert.notNull(method, "HttpMethod must not be null");
		Assert.notNull(url, "URI must not be null");
		this.method = method;
		this.url = url;
	}


	@Override
	public ClientRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.method = method;
		return this;
	}

	@Override
	public ClientRequest.Builder url(URI url) {
		Assert.notNull(url, "URI must not be null");
		this.url = url;
		return this;
	}

	@Override
	public ClientRequest.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ClientRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ClientRequest.Builder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, value);
		}
		return this;
	}

	@Override
	public ClientRequest.Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public <S, P extends Publisher<S>> ClientRequest.Builder body(P publisher, Class<S> elementClass) {
		this.body = BodyInserters.fromPublisher(publisher, elementClass);
		return this;
	}

	/**
	 * 设置请求体为给定的 {@code Publisher} 和 {@code ParameterizedTypeReference}。
	 *
	 * @param publisher      要写入请求的 {@code Publisher}
	 * @param typeReference  描述 {@code Publisher} 中包含的元素类型的类型引用
	 * @param <S>            {@code Publisher} 中的元素类型
	 * @param <P>            {@code Publisher} 的类型
	 * @return               此构建器
	 */
	@Override
	public <S, P extends Publisher<S>> ClientRequest.Builder body(
			P publisher, ParameterizedTypeReference<S> typeReference) {

		this.body = BodyInserters.fromPublisher(publisher, typeReference);
		return this;
	}

	@Override
	public ClientRequest.Builder attribute(String name, Object value) {
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ClientRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		attributesConsumer.accept(this.attributes);
		return this;
	}

	@Override
	public ClientRequest.Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
		this.httpRequestConsumer = (this.httpRequestConsumer != null ?
				this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
		return this;
	}

	@Override
	public ClientRequest.Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
		this.body = inserter;
		return this;
	}

	@Override
	public ClientRequest build() {
		return new BodyInserterRequest(
				this.method, this.url, this.headers, this.cookies, this.body,
				this.attributes, this.httpRequestConsumer);
	}


	private static class BodyInserterRequest implements ClientRequest {

		/**
		 * 请求方法
		 */
		private final HttpMethod method;

		/**
		 * 请求URL
		 */
		private final URI url;

		/**
		 * 请求头
		 */
		private final HttpHeaders headers;

		/**
		 * Cookie
		 */
		private final MultiValueMap<String, String> cookies;

		/**
		 * 请求体插入器
		 */
		private final BodyInserter<?, ? super ClientHttpRequest> body;

		/**
		 * 属性
		 */
		private final Map<String, Object> attributes;

		/**
		 * HTTP请求消费者
		 */
		@Nullable
		private final Consumer<ClientHttpRequest> httpRequestConsumer;

		/**
		 * 日志前缀
		 */
		private final String logPrefix;

		public BodyInserterRequest(HttpMethod method, URI url, HttpHeaders headers,
								   MultiValueMap<String, String> cookies, BodyInserter<?, ? super ClientHttpRequest> body,
								   Map<String, Object> attributes, @Nullable Consumer<ClientHttpRequest> httpRequestConsumer) {

			// 设置请求方法和URL
			this.method = method;
			this.url = url;

			// 设置只读的请求头
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);

			// 设置不可修改的 Cookie
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);

			// 设置请求体插入器、属性和 HTTP 请求消费者
			this.body = body;
			this.attributes = Collections.unmodifiableMap(attributes);
			this.httpRequestConsumer = httpRequestConsumer;

			// 计算并设置日志前缀
			Object id = attributes.computeIfAbsent(LOG_ID_ATTRIBUTE, name -> ObjectUtils.getIdentityHexString(this));
			this.logPrefix = "[" + id + "] ";
		}

		@Override
		public HttpMethod method() {
			return this.method;
		}

		@Override
		public URI url() {
			return this.url;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, String> cookies() {
			return this.cookies;
		}

		@Override
		public BodyInserter<?, ? super ClientHttpRequest> body() {
			return this.body;
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Consumer<ClientHttpRequest> httpRequest() {
			return this.httpRequestConsumer;
		}

		@Override
		public String logPrefix() {
			return this.logPrefix;
		}

		@Override
		public Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies) {
			// 获取请求头和 Cookie
			HttpHeaders requestHeaders = request.getHeaders();
			MultiValueMap<String, HttpCookie> requestCookies = request.getCookies();

			// 合并请求头
			if (!this.headers.isEmpty()) {
				this.headers.entrySet().stream()
						.filter(entry -> !requestHeaders.containsKey(entry.getKey()))
						.forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
			}

			// 添加 Cookie
			if (!this.cookies.isEmpty()) {
				this.cookies.forEach((name, values) -> values.forEach(value -> {
					HttpCookie cookie = new HttpCookie(name, value);
					requestCookies.add(name, cookie);
				}));
			}

			// 应用 HTTP 请求消费者
			if (this.httpRequestConsumer != null) {
				this.httpRequestConsumer.accept(request);
			}

			// 插入请求体
			return this.body.insert(request, new BodyInserter.Context() {
				@Override
				public List<HttpMessageWriter<?>> messageWriters() {
					return strategies.messageWriters();
				}

				@Override
				public Optional<ServerHttpRequest> serverRequest() {
					return Optional.empty();
				}

				@Override
				public Map<String, Object> hints() {
					return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix());
				}
			});
		}
	}

}
