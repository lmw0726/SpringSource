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

package org.springframework.web.reactive.function.server;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * 基于 {@link ServerWebExchange} 的 {@code ServerRequest} 实现。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultServerRequest implements ServerRequest {

	/**
	 * 将 UnsupportedMediaTypeException 映射为 UnsupportedMediaTypeStatusException 的功能。
	 */
	private static final Function<UnsupportedMediaTypeException, UnsupportedMediaTypeStatusException> ERROR_MAPPER =
			ex -> (ex.getContentType() != null ?
					new UnsupportedMediaTypeStatusException(
							ex.getContentType(), ex.getSupportedMediaTypes(), ex.getBodyType()) :
					new UnsupportedMediaTypeStatusException(ex.getMessage()));

	/**
	 * 将 DecodingException 映射为 ServerWebInputException 的功能。
	 */
	private static final Function<DecodingException, ServerWebInputException> DECODING_MAPPER =
			ex -> new ServerWebInputException("读取 HTTP 消息失败", null, ex);

	/**
	 * Web服务端交换
	 */
	private final ServerWebExchange exchange;

	/**
	 * 请求头
	 */
	private final Headers headers;

	/**
	 * 消息读取器列表
	 */
	private final List<HttpMessageReader<?>> messageReaders;


	DefaultServerRequest(ServerWebExchange exchange, List<HttpMessageReader<?>> messageReaders) {
		this.exchange = exchange;
		this.messageReaders = Collections.unmodifiableList(new ArrayList<>(messageReaders));
		this.headers = new DefaultHeaders();
	}

	/**
	 * 检查是否未修改。
	 *
	 * @param exchange       请求和响应交换对象
	 * @param lastModified   上次修改时间
	 * @param etag           实体标签
	 * @return 若请求未修改则返回相应的响应，否则返回空
	 */
	static Mono<ServerResponse> checkNotModified(ServerWebExchange exchange, @Nullable Instant lastModified,
												 @Nullable String etag) {

		// 如果 lastModified 为空，则将其设置为最小的 Instant 时间
		if (lastModified == null) {
			lastModified = Instant.MIN;
		}

		// 检查资源是否未被修改，如果未修改则返回 304 状态码
		if (exchange.checkNotModified(etag, lastModified)) {
			// 获取响应的原始状态码
			Integer statusCode = exchange.getResponse().getRawStatusCode();

			// 构建并返回一个 ServerResponse 对象，状态码为响应状态码或默认 200
			return ServerResponse.status(statusCode != null ? statusCode : 200)
					.headers(headers -> headers.addAll(exchange.getResponse().getHeaders()))
					.build();
		} else {
			// 如果资源已经被修改，则返回一个空的 Mono
			return Mono.empty();
		}
	}

	@Override
	public String methodName() {
		return request().getMethodValue();
	}

	@Override
	public URI uri() {
		return request().getURI();
	}

	@Override
	public UriBuilder uriBuilder() {
		return UriComponentsBuilder.fromUri(uri());
	}

	@Override
	public RequestPath requestPath() {
		return request().getPath();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, HttpCookie> cookies() {
		return request().getCookies();
	}

	@Override
	public Optional<InetSocketAddress> remoteAddress() {
		return Optional.ofNullable(request().getRemoteAddress());
	}

	@Override
	public Optional<InetSocketAddress> localAddress() {
		return Optional.ofNullable(request().getLocalAddress());
	}

	@Override
	public List<HttpMessageReader<?>> messageReaders() {
		return this.messageReaders;
	}

	/**
	 * 提取请求体内容。
	 *
	 * @param extractor 请求体提取器
	 * @param <T>       提取的请求体类型
	 * @return 提取的请求体内容
	 */
	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
		return bodyInternal(extractor, Hints.from(Hints.LOG_PREFIX_HINT, exchange().getLogPrefix()));
	}

	/**
	 * 提取请求体内容。
	 *
	 * @param extractor 请求体提取器
	 * @param hints     提取器所需的提示信息
	 * @param <T>       提取的请求体类型
	 * @return 提取的请求体内容
	 */
	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
		// 合并提示信息，设置日志前缀
		hints = Hints.merge(hints, Hints.LOG_PREFIX_HINT, exchange().getLogPrefix());

		// 调用 bodyInternal 方法，传递提取器和提示信息
		return bodyInternal(extractor, hints);
	}

	/**
	 * 从请求中提取特定类型的内容。
	 *
	 * @param <T>       提取的内容类型
	 * @param extractor 内容提取器
	 * @param hints     提取过程中的提示信息
	 * @return 提取的内容
	 */
	private <T> T bodyInternal(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
		return extractor.extract(request(), new BodyExtractor.Context() {
			/**
			 * 获取消息读取器列表。
			 * @return 消息读取器列表
			 */
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return messageReaders;
			}

			/**
			 * 获取服务器响应（如果有）。
			 * @return 可选的服务器响应
			 */
			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.of(exchange().getResponse());
			}

			/**
			 * 获取提取过程中的提示信息。
			 * @return 提取过程中的提示信息
			 */
			@Override
			public Map<String, Object> hints() {
				return hints;
			}
		});
	}

	/**
	 * 将请求体转换为 Mono。
	 *
	 * @param elementClass 请求体元素类型的类对象
	 * @param <T>          请求体元素类型
	 * @return 请求体的 Mono
	 */
	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		// 提取请求体内容为 Mono，并在出现不支持的媒体类型和解码异常时映射错误
		Mono<T> mono = body(BodyExtractors.toMono(elementClass));
		return mono.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER)
				.onErrorMap(DecodingException.class, DECODING_MAPPER);
	}

	/**
	 * 将请求体转换为 Mono。
	 *
	 * @param typeReference 请求体类型的参数化类型引用
	 * @param <T>           请求体类型
	 * @return 请求体的 Mono
	 */
	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
		// 提取请求体内容为 Mono，并在出现不支持的媒体类型和解码异常时映射错误
		Mono<T> mono = body(BodyExtractors.toMono(typeReference));
		return mono.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER)
				.onErrorMap(DecodingException.class, DECODING_MAPPER);
	}

	/**
	 * 将请求体转换为 Flux。
	 *
	 * @param elementClass 请求体元素类型的类对象
	 * @param <T>          请求体元素类型
	 * @return 请求体的 Flux
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		// 将请求体内容转换为 Flux，如果请求体元素类型为 DataBuffer 类型，则直接返回请求体的 Flux
		Flux<T> flux = (elementClass.equals(DataBuffer.class) ?
				(Flux<T>) request().getBody() : body(BodyExtractors.toFlux(elementClass)));
		return flux.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER)
				.onErrorMap(DecodingException.class, DECODING_MAPPER);
	}

	/**
	 * 将请求体转换为 Flux。
	 *
	 * @param typeReference 请求体类型的参数化类型引用
	 * @param <T>           请求体类型
	 * @return 请求体的 Flux
	 */
	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
		// 提取请求体内容为 Flux，并在出现不支持的媒体类型和解码异常时映射错误
		Flux<T> flux = body(BodyExtractors.toFlux(typeReference));
		return flux.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER)
				.onErrorMap(DecodingException.class, DECODING_MAPPER);
	}

	@Override
	public Map<String, Object> attributes() {
		return this.exchange.getAttributes();
	}

	@Override
	public MultiValueMap<String, String> queryParams() {
		return request().getQueryParams();
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.exchange.getAttributeOrDefault(
				RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
	}

	@Override
	public Mono<WebSession> session() {
		return this.exchange.getSession();
	}

	@Override
	public Mono<? extends Principal> principal() {
		return this.exchange.getPrincipal();
	}

	@Override
	public Mono<MultiValueMap<String, String>> formData() {
		return this.exchange.getFormData();
	}

	@Override
	public Mono<MultiValueMap<String, Part>> multipartData() {
		return this.exchange.getMultipartData();
	}

	private ServerHttpRequest request() {
		return this.exchange.getRequest();
	}

	@Override
	public ServerWebExchange exchange() {
		return this.exchange;
	}

	@Override
	public String toString() {
		return String.format("HTTP %s %s", method(), path());
	}


	private class DefaultHeaders implements Headers {

		/**
		 * HTTP请求头
		 */
		private final HttpHeaders httpHeaders =
				HttpHeaders.readOnlyHttpHeaders(request().getHeaders());

		@Override
		public List<MediaType> accept() {
			return this.httpHeaders.getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return this.httpHeaders.getAcceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return this.httpHeaders.getAcceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			long value = this.httpHeaders.getContentLength();
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(this.httpHeaders.getContentType());
		}

		@Override
		public InetSocketAddress host() {
			return this.httpHeaders.getHost();
		}

		@Override
		public List<HttpRange> range() {
			return this.httpHeaders.getRange();
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = this.httpHeaders.get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.httpHeaders;
		}

		@Override
		public String toString() {
			return this.httpHeaders.toString();
		}
	}

}
