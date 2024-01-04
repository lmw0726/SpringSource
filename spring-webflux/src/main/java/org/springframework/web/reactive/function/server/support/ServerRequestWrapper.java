/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;

/**
 * {@link ServerRequest} 接口的实现，可以被子类化以适应
 * {@link org.springframework.web.reactive.function.server.HandlerFilterFunction 处理器过滤函数} 中的请求。
 * 所有方法默认调用委托请求。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerRequestWrapper implements ServerRequest {

	/**
	 * 服务端请求
	 */
	private final ServerRequest delegate;


	/**
	 * 创建一个包装给定请求的新 {@code ServerRequestWrapper}。
	 *
	 * @param delegate 要包装的请求
	 */
	public ServerRequestWrapper(ServerRequest delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	/**
	 * 返回被包装的请求。
	 */
	public ServerRequest request() {
		return this.delegate;
	}

	@Override
	public HttpMethod method() {
		return this.delegate.method();
	}

	@Override
	public String methodName() {
		return this.delegate.methodName();
	}

	@Override
	public URI uri() {
		return this.delegate.uri();
	}

	@Override
	public UriBuilder uriBuilder() {
		return this.delegate.uriBuilder();
	}

	@Override
	public String path() {
		return this.delegate.path();
	}

	@Override
	@Deprecated
	public PathContainer pathContainer() {
		return this.delegate.pathContainer();
	}

	@Override
	public RequestPath requestPath() {
		return this.delegate.requestPath();
	}

	@Override
	public Headers headers() {
		return this.delegate.headers();
	}

	@Override
	public MultiValueMap<String, HttpCookie> cookies() {
		return this.delegate.cookies();
	}

	@Override
	public Optional<InetSocketAddress> remoteAddress() {
		return this.delegate.remoteAddress();
	}

	@Override
	public Optional<InetSocketAddress> localAddress() {
		return this.delegate.localAddress();
	}

	@Override
	public List<HttpMessageReader<?>> messageReaders() {
		return this.delegate.messageReaders();
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
		return this.delegate.body(extractor);
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
		return this.delegate.body(extractor, hints);
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return this.delegate.bodyToMono(elementClass);
	}

	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
		return this.delegate.bodyToMono(typeReference);
	}

	@Override
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return this.delegate.bodyToFlux(elementClass);
	}

	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
		return this.delegate.bodyToFlux(typeReference);
	}

	@Override
	public Optional<Object> attribute(String name) {
		return this.delegate.attribute(name);
	}

	@Override
	public Map<String, Object> attributes() {
		return this.delegate.attributes();
	}

	@Override
	public Optional<String> queryParam(String name) {
		return this.delegate.queryParam(name);
	}

	@Override
	public MultiValueMap<String, String> queryParams() {
		return this.delegate.queryParams();
	}

	@Override
	public String pathVariable(String name) {
		return this.delegate.pathVariable(name);
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.delegate.pathVariables();
	}

	@Override
	public Mono<WebSession> session() {
		return this.delegate.session();
	}

	@Override
	public Mono<? extends Principal> principal() {
		return this.delegate.principal();
	}

	@Override
	public Mono<MultiValueMap<String, String>> formData() {
		return this.delegate.formData();
	}

	@Override
	public Mono<MultiValueMap<String, Part>> multipartData() {
		return this.delegate.multipartData();
	}

	@Override
	public ServerWebExchange exchange() {
		return this.delegate.exchange();
	}

	/**
	 * {@code Headers} 接口的实现，可以被子类化以适应
	 * {@link org.springframework.web.reactive.function.server.HandlerFilterFunction 处理器过滤函数} 中的请求头。
	 * 所有方法默认调用委托请求头。
	 */
	public static class HeadersWrapper implements ServerRequest.Headers {

		/**
		 * 请求头
		 */
		private final Headers headers;

		/**
		 * 创建一个包装给定请求头的新 {@code HeadersWrapper}。
		 *
		 * @param headers 要包装的请求头
		 */
		public HeadersWrapper(Headers headers) {
			Assert.notNull(headers, "Headers must not be null");
			this.headers = headers;
		}

		@Override
		public List<MediaType> accept() {
			return this.headers.accept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return this.headers.acceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return this.headers.acceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			return this.headers.contentLength();
		}

		@Override
		public Optional<MediaType> contentType() {
			return this.headers.contentType();
		}

		@Override
		public InetSocketAddress host() {
			return this.headers.host();
		}

		@Override
		public List<HttpRange> range() {
			return this.headers.range();
		}

		@Override
		public List<String> header(String headerName) {
			return this.headers.header(headerName);
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.headers.asHttpHeaders();
		}
	}

}
