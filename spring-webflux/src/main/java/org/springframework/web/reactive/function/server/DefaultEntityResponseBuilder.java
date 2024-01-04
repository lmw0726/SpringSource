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

package org.springframework.web.reactive.function.server;

import org.springframework.core.codec.Hints;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * 默认的 {@link EntityResponse.Builder} 实现。
 *
 * @param <T> 构建器类型的自引用
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {
	/**
	 * 实体对象
	 */
	private final T entity;

	/**
	 * 用于插入实体的BodyInserter对象
	 */
	private final BodyInserter<T, ? super ServerHttpResponse> inserter;

	/**
	 * 状态码，默认为OK（200）
	 */
	private int status = HttpStatus.OK.value();

	/**
	 * HTTP响应头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 响应的Cookie列表
	 */
	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 提示信息映射
	 */
	private final Map<String, Object> hints = new HashMap<>();


	public DefaultEntityResponseBuilder(T entity, BodyInserter<T, ? super ServerHttpResponse> inserter) {
		this.entity = entity;
		this.inserter = inserter;
	}


	@Override
	public EntityResponse.Builder<T> status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status.value();
		return this;
	}

	@Override
	public EntityResponse.Builder<T> status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> headers(HttpHeaders headers) {
		this.headers.putAll(headers);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	/**
	 * 设置实体标签（ETag）。
	 *
	 * @param etag 要设置的实体标签
	 * @return EntityResponse.Builder对象，用于链式调用
	 */
	@Override
	public EntityResponse.Builder<T> eTag(String etag) {
		if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
			etag = "\"" + etag;
		}
		if (!etag.endsWith("\"")) {
			etag = etag + "\"";
		}
		this.headers.setETag(etag);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> hint(String key, Object value) {
		this.hints.put(key, value);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> hints(Consumer<Map<String, Object>> hintsConsumer) {
		hintsConsumer.accept(this.hints);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Mono<EntityResponse<T>> build() {
		return Mono.just(new DefaultEntityResponse<T>(
				this.status, this.headers, this.cookies, this.entity, this.inserter, this.hints));
	}


	private static final class DefaultEntityResponse<T>
			extends DefaultServerResponseBuilder.AbstractServerResponse
			implements EntityResponse<T> {

		/**
		 * 实体对象。
		 */
		private final T entity;

		/**
		 * 实体对象的插入器，用于将实体写入到服务器HTTP响应中。
		 */
		private final BodyInserter<T, ? super ServerHttpResponse> inserter;


		public DefaultEntityResponse(int statusCode, HttpHeaders headers,
									 MultiValueMap<String, ResponseCookie> cookies, T entity,
									 BodyInserter<T, ? super ServerHttpResponse> inserter, Map<String, Object> hints) {

			super(statusCode, headers, cookies, hints);
			this.entity = entity;
			this.inserter = inserter;
		}

		/**
		 * 获取实体对象。
		 *
		 * @return 实体对象
		 */
		@Override
		public T entity() {
			return this.entity;
		}

		/**
		 * 获取用于插入实体的BodyInserter对象。
		 *
		 * @return BodyInserter对象，用于将实体插入ServerHttpResponse
		 */
		@Override
		public BodyInserter<T, ? super ServerHttpResponse> inserter() {
			return this.inserter;
		}

		/**
		 * 覆盖父类方法，用于在内部执行写入操作。
		 *
		 * @param exchange 服务器Web交换对象
		 * @param context  上下文对象
		 * @return Mono<Void>对象，表示写入操作完成的信号
		 */
		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			return inserter().insert(exchange.getResponse(), new BodyInserter.Context() {
				/**
				 * 获取消息写入器列表。
				 *
				 * @return HttpMessageWriter列表
				 */
				@Override
				public List<HttpMessageWriter<?>> messageWriters() {
					return context.messageWriters();
				}

				/**
				 * 获取服务器HTTP请求对象的可选项。
				 *
				 * @return 可选的ServerHttpRequest对象
				 */
				@Override
				public Optional<ServerHttpRequest> serverRequest() {
					return Optional.of(exchange.getRequest());
				}

				/**
				 * 获取提示信息的映射。
				 *
				 * @return 提示信息映射
				 */
				@Override
				public Map<String, Object> hints() {
					Map<String, Object> hints = new HashMap<>();
					hints.put(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
					return hints;
				}
			});
		}
	}

}
