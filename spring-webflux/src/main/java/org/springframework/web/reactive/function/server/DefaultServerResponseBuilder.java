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

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 默认的 {@link ServerResponse.BodyBuilder} 实现。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	/**
	 * HTTP响应的状态码
	 */
	private final int statusCode;

	/**
	 * HTTP响应的头信息
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * HTTP响应中的cookies
	 */
	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 提示信息的Map
	 */
	private final Map<String, Object> hints = new HashMap<>();


	public DefaultServerResponseBuilder(ServerResponse other) {
		Assert.notNull(other, "ServerResponse must not be null");
		this.headers.addAll(other.headers());
		this.cookies.addAll(other.cookies());
		if (other instanceof AbstractServerResponse) {
			AbstractServerResponse abstractOther = (AbstractServerResponse) other;
			this.statusCode = abstractOther.statusCode;
			this.hints.putAll(abstractOther.hints);
		} else {
			this.statusCode = other.statusCode().value();
		}
	}

	public DefaultServerResponseBuilder(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.statusCode = status.value();
	}

	public DefaultServerResponseBuilder(int statusCode) {
		this.statusCode = statusCode;
	}


	@Override
	public ServerResponse.BodyBuilder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	/**
	 * 设置ETag（实体标签）到HTTP响应的构建器中
	 *
	 * @param etag 要设置的ETag
	 * @return 返回一个ServerResponse.BodyBuilder，用于链式调用其他方法
	 */
	@Override
	public ServerResponse.BodyBuilder eTag(String etag) {
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
	public ServerResponse.BodyBuilder hint(String key, Object value) {
		this.hints.put(key, value);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder hints(Consumer<Map<String, Object>> hintsConsumer) {
		hintsConsumer.accept(this.hints);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Mono<ServerResponse> build() {
		return build((exchange, handlerStrategies) -> exchange.getResponse().setComplete());
	}

	/**
	 * 构建ServerResponse对象，使用给定的Void Publisher
	 *
	 * @param voidPublisher 要使用的Void Publisher
	 * @return 返回一个Mono表示的ServerResponse对象
	 */
	@Override
	public Mono<ServerResponse> build(Publisher<Void> voidPublisher) {
		// 断言 voidPublisher 不为空，如果为空则抛出异常
		Assert.notNull(voidPublisher, "Publisher must not be null");

		// 构建处理器函数，将 voidPublisher 转换为 Mono 并设置响应为完成状态
		return build((exchange, handlerStrategies) ->
				Mono.from(voidPublisher).then(exchange.getResponse().setComplete()));
	}

	@Override
	public Mono<ServerResponse> build(
			BiFunction<ServerWebExchange, ServerResponse.Context, Mono<Void>> writeFunction) {

		return Mono.just(new WriterFunctionResponse(
				this.statusCode, this.headers, this.cookies, writeFunction));
	}

	@Override
	public Mono<ServerResponse> bodyValue(Object body) {
		return initBuilder(body, BodyInserters.fromValue(body));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass) {
		return initBuilder(publisher, BodyInserters.fromPublisher(publisher, elementClass));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, ParameterizedTypeReference<T> typeRef) {
		return initBuilder(publisher, BodyInserters.fromPublisher(publisher, typeRef));
	}

	@Override
	public Mono<ServerResponse> body(Object producer, Class<?> elementClass) {
		return initBuilder(producer, BodyInserters.fromProducer(producer, elementClass));
	}

	@Override
	public Mono<ServerResponse> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
		return initBuilder(producer, BodyInserters.fromProducer(producer, elementTypeRef));
	}

	/**
	 * 初始化ServerResponse构建器，并构建ServerResponse对象
	 *
	 * @param entity   要构建的实体
	 * @param inserter 用于插入实体的BodyInserter
	 * @param <T>      实体的类型
	 * @return 返回一个Mono表示的ServerResponse对象
	 */
	private <T> Mono<ServerResponse> initBuilder(T entity, BodyInserter<T, ReactiveHttpOutputMessage> inserter) {
		// 创建一个 DefaultEntityResponseBuilder 对象，设置实体、插入器、状态码、头部、Cookie、提示信息
		return new DefaultEntityResponseBuilder<>(entity, inserter)
				// 设置状态码
				.status(this.statusCode)
				// 设置头部信息
				.headers(this.headers)
				// 设置 Cookie
				.cookies(cookies -> cookies.addAll(this.cookies))
				// 设置提示信息
				.hints(hints -> hints.putAll(this.hints))
				// 构建响应对象
				.build()
				// 返回构建的响应对象
				.map(Function.identity());
	}

	@Override
	public Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter) {
		return Mono.just(new BodyInserterResponse<>(
				this.statusCode, this.headers, this.cookies, inserter, this.hints));
	}

	@Override
	@Deprecated
	public Mono<ServerResponse> syncBody(Object body) {
		return bodyValue(body);
	}

	/**
	 * 渲染指定名称和模型属性的ServerResponse对象
	 *
	 * @param name            要渲染的名称
	 * @param modelAttributes 要渲染的模型属性
	 * @return 返回一个Mono表示的ServerResponse对象
	 */
	@Override
	public Mono<ServerResponse> render(String name, Object... modelAttributes) {
		// 创建一个 DefaultRenderingResponseBuilder 对象，设置视图名称、状态码、头部、Cookie、模型属性
		return new DefaultRenderingResponseBuilder(name)
				// 设置状态码
				.status(this.statusCode)
				// 设置头部信息
				.headers(this.headers)
				// 设置 Cookie
				.cookies(cookies -> cookies.addAll(this.cookies))
				// 设置模型属性
				.modelAttributes(modelAttributes)
				// 构建响应对象
				.build()
				// 返回构建的响应对象
				.map(Function.identity());
	}

	/**
	 * 使用指定名称和模型映射渲染ServerResponse对象
	 * @param name 要渲染的名称
	 * @param model 包含模型映射的Map
	 * @return 返回一个Mono表示的ServerResponse对象
	 */
	@Override
	public Mono<ServerResponse> render(String name, Map<String, ?> model) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(this.headers)
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(model)
				.build()
				.map(Function.identity());
	}


	/**
	 * {@link ServerResponse} 实现的抽象基类。
	 */
	abstract static class AbstractServerResponse implements ServerResponse {

		private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

		/**
		 * 状态码
		 */
		final int statusCode;

		/**
		 * HTTP响应头
		 */
		private final HttpHeaders headers;

		/**
		 * 响应的Cookie列表
		 */
		private final MultiValueMap<String, ResponseCookie> cookies;

		/**
		 * 提示信息
		 */
		final Map<String, Object> hints;


		protected AbstractServerResponse(
				int statusCode, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies,
				Map<String, Object> hints) {

			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
			this.hints = hints;
		}

		@Override
		public final HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public int rawStatusCode() {
			return this.statusCode;
		}

		@Override
		public final HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, ResponseCookie> cookies() {
			return this.cookies;
		}

		/**
		 * 将响应写入到服务器的方法。
		 *
		 * @param exchange 服务器Web交换对象
		 * @param context  上下文对象
		 * @return Mono<Void>对象，表示写入操作完成的信号
		 */
		@Override
		public final Mono<Void> writeTo(ServerWebExchange exchange, Context context) {
			// 将状态码和响应头写入响应对象
			writeStatusAndHeaders(exchange.getResponse());

			// 获取最后修改时间和请求的 HTTP 方法
			Instant lastModified = Instant.ofEpochMilli(headers().getLastModified());
			HttpMethod httpMethod = exchange.getRequest().getMethod();

			// 如果是安全的 HTTP 方法并且资源未修改，则返回完成的响应
			if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(headers().getETag(), lastModified)) {
				return exchange.getResponse().setComplete();
			} else {
				// 否则执行数据写入操作
				return writeToInternal(exchange, context);
			}
		}

		/**
		 * 设置响应的状态码和头部信息。
		 *
		 * @param response 服务器响应对象
		 */
		private void writeStatusAndHeaders(ServerHttpResponse response) {
			// 设置响应的原始状态码
			response.setRawStatusCode(this.statusCode);
			// 复制响应的头部信息
			copy(this.headers, response.getHeaders());
			// 复制响应的Cookies信息
			copy(this.cookies, response.getCookies());
		}

		/**
		 * 写入响应的内部方法。
		 *
		 * @param exchange 服务器Web交换对象
		 * @param context  上下文对象
		 * @return Mono<Void>对象，表示写入操作完成的信号
		 */
		protected abstract Mono<Void> writeToInternal(ServerWebExchange exchange, Context context);

		/**
		 * 将源MultiValueMap中的条目复制到目标MultiValueMap中。
		 *
		 * @param src 源MultiValueMap
		 * @param dst 目标MultiValueMap
		 */
		private static <K, V> void copy(MultiValueMap<K, V> src, MultiValueMap<K, V> dst) {
			if (!src.isEmpty()) {
				// 复制源MultiValueMap中的条目到目标MultiValueMap中
				dst.putAll(src);
			}
		}
	}


	/**
	 * WriterFunctionResponse是一个私有静态内部类，继承自AbstractServerResponse
	 */
	private static final class WriterFunctionResponse extends AbstractServerResponse {

		/**
		 * 写操作的函数式响应
		 */
		private final BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction;

		/**
		 * 构造函数，初始化WriterFunctionResponse对象
		 *
		 * @param statusCode    HTTP响应状态码
		 * @param headers       HTTP响应头信息
		 * @param cookies       HTTP响应中的cookies
		 * @param writeFunction 写操作的函数式接口
		 */
		public WriterFunctionResponse(int statusCode, HttpHeaders headers,
									  MultiValueMap<String, ResponseCookie> cookies,
									  BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction) {

			super(statusCode, headers, cookies, Collections.emptyMap());
			Assert.notNull(writeFunction, "BiFunction must not be null");
			this.writeFunction = writeFunction;
		}

		/**
		 * 重写父类方法，执行具体的写操作
		 *
		 * @param exchange 服务器之间交换的对象
		 * @param context  上下文信息
		 * @return 返回一个Mono表示的写操作的异步结果
		 */
		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			return this.writeFunction.apply(exchange, context);
		}
	}


	/**
	 * BodyInserterResponse是一个私有静态内部类，继承自AbstractServerResponse
	 *
	 * @param <T> 范型，表示响应体的类型
	 */
	private static final class BodyInserterResponse<T> extends AbstractServerResponse {

		/**
		 * 用于插入响应体的BodyInserter
		 */
		private final BodyInserter<T, ? super ServerHttpResponse> inserter;

		/**
		 * 构造函数，初始化BodyInserterResponse对象
		 *
		 * @param statusCode HTTP响应状态码
		 * @param headers    HTTP响应头信息
		 * @param cookies    HTTP响应中的cookies
		 * @param body       插入响应体的BodyInserter
		 * @param hints      提示信息的Map
		 */
		public BodyInserterResponse(int statusCode, HttpHeaders headers,
									MultiValueMap<String, ResponseCookie> cookies,
									BodyInserter<T, ? super ServerHttpResponse> body, Map<String, Object> hints) {

			super(statusCode, headers, cookies, hints);
			Assert.notNull(body, "BodyInserter must not be null");
			this.inserter = body;
		}

		/**
		 * 重写父类方法，执行具体的写操作
		 *
		 * @param exchange 服务器之间交换的对象
		 * @param context  上下文信息
		 * @return 返回一个Mono表示的写操作的异步结果
		 */
		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			return this.inserter.insert(exchange.getResponse(), new BodyInserter.Context() {
				@Override
				public List<HttpMessageWriter<?>> messageWriters() {
					return context.messageWriters();
				}

				@Override
				public Optional<ServerHttpRequest> serverRequest() {
					return Optional.of(exchange.getRequest());
				}

				@Override
				public Map<String, Object> hints() {
					hints.put(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
					return hints;
				}
			});
		}
	}

}
