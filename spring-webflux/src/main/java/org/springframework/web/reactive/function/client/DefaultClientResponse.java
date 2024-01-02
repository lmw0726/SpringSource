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

package org.springframework.web.reactive.function.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;

/**
 * {@link ClientResponse} 的默认实现。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
class DefaultClientResponse implements ClientResponse {

	/**
	 * 空字节数组
	 */
	private static final byte[] EMPTY = new byte[0];

	/**
	 * 客户端响应
	 */
	private final ClientHttpResponse response;

	/**
	 * 响应头
	 */
	private final Headers headers;

	/**
	 * 交换策略
	 */
	private final ExchangeStrategies strategies;

	/**
	 * 日志前缀
	 */
	private final String logPrefix;
	/**
	 * 请求描述
	 */
	private final String requestDescription;

	/**
	 * 请求提供程序
	 */
	private final Supplier<HttpRequest> requestSupplier;
	/**
	 * 请求体提取器上下文
	 */
	private final BodyExtractor.Context bodyExtractorContext;


	public DefaultClientResponse(ClientHttpResponse response, ExchangeStrategies strategies,
								 String logPrefix, String requestDescription, Supplier<HttpRequest> requestSupplier) {

		this.response = response;
		this.strategies = strategies;
		this.headers = new DefaultHeaders();
		this.logPrefix = logPrefix;
		this.requestDescription = requestDescription;
		this.requestSupplier = requestSupplier;
		this.bodyExtractorContext = new BodyExtractor.Context() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return strategies.messageReaders();
			}

			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.empty();
			}

			@Override
			public Map<String, Object> hints() {
				return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix);
			}
		};
	}


	@Override
	public ExchangeStrategies strategies() {
		return this.strategies;
	}

	@Override
	public HttpStatus statusCode() {
		return this.response.getStatusCode();
	}

	@Override
	public int rawStatusCode() {
		return this.response.getRawStatusCode();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> cookies() {
		return this.response.getCookies();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		// 提取结果
		T result = extractor.extract(this.response, this.bodyExtractorContext);

		// 构建描述字符串
		String description = "Body from " + this.requestDescription + " [DefaultClientResponse]";

		// 根据结果类型应用 checkpoint
		if (result instanceof Mono) {
			return (T) ((Mono<?>) result).checkpoint(description);
		} else if (result instanceof Flux) {
			return (T) ((Flux<?>) result).checkpoint(description);
		} else {
			return result;
		}
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return body(BodyExtractors.toMono(elementClass));
	}

	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
		return body(BodyExtractors.toMono(elementTypeRef));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return elementClass.equals(DataBuffer.class) ?
				(Flux<T>) body(BodyExtractors.toDataBuffers()) : body(BodyExtractors.toFlux(elementClass));
	}

	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
		return body(BodyExtractors.toFlux(elementTypeRef));
	}

	@Override
	public Mono<Void> releaseBody() {
		return body(BodyExtractors.toDataBuffers()).map(DataBufferUtils::release).then();
	}

	@Override
	public Mono<ResponseEntity<Void>> toBodilessEntity() {
		return releaseBody().then(WebClientUtils.mapToEntity(this, Mono.empty()));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
		return WebClientUtils.mapToEntity(this, bodyToMono(bodyType));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
		return WebClientUtils.mapToEntity(this, bodyToMono(bodyTypeReference));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
		return WebClientUtils.mapToEntityList(this, bodyToFlux(elementClass));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
		return WebClientUtils.mapToEntityList(this, bodyToFlux(elementTypeRef));
	}

	@Override
	public Mono<WebClientResponseException> createException() {
		return bodyToMono(byte[].class)
				.defaultIfEmpty(EMPTY)
				.onErrorReturn(ex -> !(ex instanceof Error), EMPTY)
				.map(bodyBytes -> {
					// 获取请求
					HttpRequest request = this.requestSupplier.get();

					// 获取字符集
					Charset charset = headers().contentType().map(MimeType::getCharset).orElse(null);

					// 获取原始状态码和对应的 HTTP 状态码
					int statusCode = rawStatusCode();
					HttpStatus httpStatus = HttpStatus.resolve(statusCode);

					// 根据 HTTP 状态码创建 WebClientResponseException 或 UnknownHttpStatusCodeException
					if (httpStatus != null) {
						return WebClientResponseException.create(
								statusCode,
								httpStatus.getReasonPhrase(),
								headers().asHttpHeaders(),
								bodyBytes,
								charset,
								request);
					} else {
						return new UnknownHttpStatusCodeException(
								statusCode,
								headers().asHttpHeaders(),
								bodyBytes,
								charset,
								request);
					}
				});
	}

	@Override
	public String logPrefix() {
		return this.logPrefix;
	}

	/**
	 * 由DefaultClientResponseBuilder使用
	 *
	 * @return Http请求
	 */
	HttpRequest request() {
		return this.requestSupplier.get();
	}

	private class DefaultHeaders implements Headers {
		/**
		 * HTTP请求头
		 */
		private final HttpHeaders httpHeaders =
				HttpHeaders.readOnlyHttpHeaders(response.getHeaders());

		@Override
		public OptionalLong contentLength() {
			return toOptionalLong(this.httpHeaders.getContentLength());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(this.httpHeaders.getContentType());
		}

		@Override
		public List<String> header(String headerName) {
			// 获取请求头值列表
			List<String> headerValues = this.httpHeaders.get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.httpHeaders;
		}

		private OptionalLong toOptionalLong(long value) {
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}
	}

}
