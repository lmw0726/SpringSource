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

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ServerRequest.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.1
 */
class DefaultServerRequestBuilder implements ServerRequest.Builder {

	/**
	 * HTTP消息阅读器的列表。
	 */
	private final List<HttpMessageReader<?>> messageReaders;

	/**
	 * 服务器端Web交换对象。
	 */
	private ServerWebExchange exchange;

	/**
	 * 方法名。
	 */
	private String methodName;

	/**
	 * 请求的URI。
	 */
	private URI uri;

	/**
	 * 请求的HTTP头信息。
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 请求的HTTP Cookie 信息，以 MultiValueMap 形式存储。
	 */
	private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 存储属性信息的 Map 对象。
	 */
	private final Map<String, Object> attributes = new LinkedHashMap<>();

	/**
	 * 请求体的数据流，默认为空流。
	 */
	private Flux<DataBuffer> body = Flux.empty();


	DefaultServerRequestBuilder(ServerRequest other) {
		Assert.notNull(other, "ServerRequest must not be null");
		this.messageReaders = other.messageReaders();
		this.exchange = other.exchange();
		this.methodName = other.methodName();
		this.uri = other.uri();
		this.headers.addAll(other.headers().asHttpHeaders());
		this.cookies.addAll(other.cookies());
		this.attributes.putAll(other.attributes());
	}


	@Override
	public ServerRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.methodName = method.name();
		return this;
	}

	@Override
	public ServerRequest.Builder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		return this;
	}

	@Override
	public ServerRequest.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerRequest.Builder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, new HttpCookie(name, value));
		}
		return this;
	}

	/**
	 * 设置或修改请求的HTTP Cookie信息，通过传入一个用于处理HTTP Cookie的Consumer。
	 *
	 * @param cookiesConsumer 用于处理HTTP Cookie的Consumer函数
	 * @return 返回修改后的ServerRequest.Builder对象
	 */
	@Override
	public ServerRequest.Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
		// 调用Consumer函数处理HTTP Cookie
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	/**
	 * 设置请求的数据流。
	 *
	 * @param body 请求的数据流，不能为空
	 * @return 返回修改后的 ServerRequest.Builder 对象
	 */
	@Override
	public ServerRequest.Builder body(Flux<DataBuffer> body) {
		Assert.notNull(body, "Body must not be null");
		// 释放之前的请求体资源
		releaseBody();
		// 设置新的请求体数据流
		this.body = body;
		return this;
	}

	/**
	 * 设置请求的数据体为字符串形式。
	 *
	 * @param body 请求的字符串形式数据体，不能为空
	 * @return 返回修改后的 ServerRequest.Builder 对象
	 */
	@Override
	public ServerRequest.Builder body(String body) {
		Assert.notNull(body, "Body must not be null");
		// 释放之前的请求体资源
		releaseBody();
		// 将字符串转换为字节数组，构建新的请求体数据流
		this.body = Flux.just(body)
				.map(s -> {
					byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
					return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
				});
		return this;
	}

	/**
	 * 释放请求体的数据流资源。
	 */
	private void releaseBody() {
		// 订阅数据流并释放资源
		this.body.subscribe(DataBufferUtils.releaseConsumer());
	}

	@Override
	public ServerRequest.Builder attribute(String name, Object value) {
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ServerRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		attributesConsumer.accept(this.attributes);
		return this;
	}

	/**
	 * 构建 ServerRequest 对象。
	 *
	 * @return 返回构建完成的 ServerRequest 对象
	 */
	@Override
	public ServerRequest build() {
		// 创建ServerHttpRequest对象
		ServerHttpRequest serverHttpRequest = new BuiltServerHttpRequest(
				this.exchange.getRequest().getId(),
				this.methodName,
				this.uri,
				this.headers,
				this.cookies,
				this.body
		);
		// 创建ServerWebExchange对象
		ServerWebExchange exchange = new DelegatingServerWebExchange(
				serverHttpRequest,
				this.attributes,
				this.exchange,
				this.messageReaders
		);
		// 返回一个新的DefaultServerRequest对象
		return new DefaultServerRequest(exchange, this.messageReaders);
	}


	private static class BuiltServerHttpRequest implements ServerHttpRequest {

		/**
		 * 匹配查询字符串的正则表达式模式。
		 */
		private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

		/**
		 * 请求的唯一标识符。
		 */
		private final String id;

		/**
		 * 请求的HTTP方法。
		 */
		private final String method;

		/**
		 * 请求的URI。
		 */
		private final URI uri;

		/**
		 * 请求的路径。
		 */
		private final RequestPath path;

		/**
		 * 请求的查询参数，以 MultiValueMap 形式存储。
		 */
		private final MultiValueMap<String, String> queryParams;

		/**
		 * 请求的HTTP头信息。
		 */
		private final HttpHeaders headers;

		/**
		 * 请求的HTTP Cookie 信息，以 MultiValueMap 形式存储。
		 */
		private final MultiValueMap<String, HttpCookie> cookies;

		/**
		 * 请求体的数据流。
		 */
		private final Flux<DataBuffer> body;

		public BuiltServerHttpRequest(String id, String method, URI uri, HttpHeaders headers,
									  MultiValueMap<String, HttpCookie> cookies, Flux<DataBuffer> body) {

			this.id = id;
			this.method = method;
			this.uri = uri;
			this.path = RequestPath.parse(uri, null);
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = unmodifiableCopy(cookies);
			this.queryParams = parseQueryParams(uri);
			this.body = body;
		}

		private static <K, V> MultiValueMap<K, V> unmodifiableCopy(MultiValueMap<K, V> original) {
			return CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(original));
		}

		/**
		 * 解析URI中的查询参数并以 MultiValueMap 的形式返回。
		 *
		 * @param uri 要解析的URI对象
		 * @return 包含解析后查询参数的 MultiValueMap 对象
		 */
		private static MultiValueMap<String, String> parseQueryParams(URI uri) {
			// 创建一个用于存储查询参数的 MultiValueMap
			MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
			// 获取URI中的原始查询字符串
			String query = uri.getRawQuery();
			if (query != null) {
				// 使用正则表达式匹配查询参数
				Matcher matcher = QUERY_PATTERN.matcher(query);
				while (matcher.find()) {
					// 获取参数名并解码
					String name = UriUtils.decode(matcher.group(1), StandardCharsets.UTF_8);
					// 获取等号（=）部分
					String eq = matcher.group(2);
					// 获取参数值并解码
					String value = matcher.group(3);
					if (value != null) {
						value = UriUtils.decode(value, StandardCharsets.UTF_8);
					} else {
						// 如果值为空，则根据等号情况设置空值或null值
						value = (StringUtils.hasLength(eq) ? "" : null);
					}
					// 将解析后的参数名和值添加到 MultiValueMap 中
					queryParams.add(name, value);
				}
			}
			// 返回解析后的查询参数
			return queryParams;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public String getMethodValue() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		public RequestPath getPath() {
			return this.path;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, HttpCookie> getCookies() {
			return this.cookies;
		}

		@Override
		public MultiValueMap<String, String> getQueryParams() {
			return this.queryParams;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.body;
		}
	}


	private static class DelegatingServerWebExchange implements ServerWebExchange {
		/**
		 * 表单数据的 ResolvableType 类型，表示键值对为 String 类型的 MultiValueMap。
		 */
		private static final ResolvableType FORM_DATA_TYPE =
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

		/**
		 * 多部分数据的 ResolvableType 类型，表示键为 String 类型，值为 Part 类型的 MultiValueMap。
		 */
		private static final ResolvableType MULTIPART_DATA_TYPE =
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

		/**
		 * 空的表单数据对象，一个不可变的、空的 MultiValueMap 对象。
		 */
		private static final Mono<MultiValueMap<String, String>> EMPTY_FORM_DATA =
				Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0))).cache();

		/**
		 * 空的多部分数据对象，一个不可变的、空的 MultiValueMap 对象。
		 */
		private static final Mono<MultiValueMap<String, Part>> EMPTY_MULTIPART_DATA =
				Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, Part>(0))).cache();

		/**
		 * 服务器端的 HTTP 请求对象。
		 */
		private final ServerHttpRequest request;

		/**
		 * 存储属性信息的 Map 对象。
		 */
		private final Map<String, Object> attributes;

		/**
		 * 代理对象，用于处理服务器端Web交换。
		 */
		private final ServerWebExchange delegate;

		/**
		 * 表单数据的 Mono 对象，包含 MultiValueMap<String, String> 类型的数据。
		 */
		private final Mono<MultiValueMap<String, String>> formDataMono;

		/**
		 * 多部分数据的 Mono 对象，包含 MultiValueMap<String, Part> 类型的数据。
		 */
		private final Mono<MultiValueMap<String, Part>> multipartDataMono;

		DelegatingServerWebExchange(ServerHttpRequest request, Map<String, Object> attributes,
									ServerWebExchange delegate, List<HttpMessageReader<?>> messageReaders) {

			this.request = request;
			this.attributes = attributes;
			this.delegate = delegate;
			this.formDataMono = initFormData(request, messageReaders);
			this.multipartDataMono = initMultipartData(request, messageReaders);
		}

		/**
		 * 初始化表单数据。
		 *
		 * @param request ServerHttpRequest 对象，用于获取请求头信息和内容类型
		 * @param readers 包含HttpMessageReader的列表，用于读取HTTP消息
		 * @return 返回一个 Mono 包含 MultiValueMap<String, String> 类型的对象
		 */
		@SuppressWarnings("unchecked")
		private static Mono<MultiValueMap<String, String>> initFormData(ServerHttpRequest request,
																		List<HttpMessageReader<?>> readers) {

			try {
				// 获取请求的内容类型
				MediaType contentType = request.getHeaders().getContentType();
				if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
					// 如果媒体类型是 APPLICATION_FORM_URLENCODED
					return ((HttpMessageReader<MultiValueMap<String, String>>) readers.stream()
							.filter(reader -> reader.canRead(FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No form data HttpMessageReader.")))
							// 读取表单数据并返回 Mono 对象
							.readMono(FORM_DATA_TYPE, request, Hints.none())
							// 如果数据为空则返回空对象
							.switchIfEmpty(EMPTY_FORM_DATA)
							// 缓存数据
							.cache();
				}
			} catch (InvalidMediaTypeException ex) {
				// 忽略无效的媒体类型异常
			}
			// 返回空的表单数据对象
			return EMPTY_FORM_DATA;
		}

		/**
		 * 初始化多部分数据。
		 *
		 * @param request ServerHttpRequest 对象，用于获取请求头信息和内容类型
		 * @param readers 包含HttpMessageReader的列表，用于读取HTTP消息
		 * @return 返回一个 Mono 包含 MultiValueMap<String, Part> 类型的对象
		 */
		@SuppressWarnings("unchecked")
		private static Mono<MultiValueMap<String, Part>> initMultipartData(ServerHttpRequest request,
																		   List<HttpMessageReader<?>> readers) {

			try {
				// 获取请求的内容类型
				MediaType contentType = request.getHeaders().getContentType();
				if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
					//如果媒体类型是 多部分表单数据类型
					return ((HttpMessageReader<MultiValueMap<String, Part>>) readers.stream()
							.filter(reader -> reader.canRead(MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No multipart HttpMessageReader.")))
							// 读取多部分数据并返回 Mono 对象
							.readMono(MULTIPART_DATA_TYPE, request, Hints.none())
							// 如果数据为空则返回空对象
							.switchIfEmpty(EMPTY_MULTIPART_DATA)
							// 缓存数据
							.cache();
				}
			} catch (InvalidMediaTypeException ex) {
				// 忽略无效的媒体类型异常
			}
			// 返回空的多部分数据对象
			return EMPTY_MULTIPART_DATA;
		}

		@Override
		public ServerHttpRequest getRequest() {
			return this.request;
		}

		@Override
		public Map<String, Object> getAttributes() {
			return this.attributes;
		}

		@Override
		public Mono<MultiValueMap<String, String>> getFormData() {
			return this.formDataMono;
		}

		@Override
		public Mono<MultiValueMap<String, Part>> getMultipartData() {
			return this.multipartDataMono;
		}

		// 代理方法

		@Override
		public ServerHttpResponse getResponse() {
			return this.delegate.getResponse();
		}

		@Override
		public Mono<WebSession> getSession() {
			return this.delegate.getSession();
		}

		@Override
		public <T extends Principal> Mono<T> getPrincipal() {
			return this.delegate.getPrincipal();
		}

		@Override
		public LocaleContext getLocaleContext() {
			return this.delegate.getLocaleContext();
		}

		@Nullable
		@Override
		public ApplicationContext getApplicationContext() {
			return this.delegate.getApplicationContext();
		}

		@Override
		public boolean isNotModified() {
			return this.delegate.isNotModified();
		}

		@Override
		public boolean checkNotModified(Instant lastModified) {
			return this.delegate.checkNotModified(lastModified);
		}

		@Override
		public boolean checkNotModified(String etag) {
			return this.delegate.checkNotModified(etag);
		}

		@Override
		public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
			return this.delegate.checkNotModified(etag, lastModified);
		}

		@Override
		public String transformUrl(String url) {
			return this.delegate.transformUrl(url);
		}

		/**
		 * 向当前对象添加URL转换器。
		 *
		 * @param transformer URL转换器函数，接受一个String类型的参数并返回一个String类型的结果
		 */
		@Override
		public void addUrlTransformer(Function<String, String> transformer) {
			this.delegate.addUrlTransformer(transformer);
		}

		@Override
		public String getLogPrefix() {
			return this.delegate.getLogPrefix();
		}
	}

}
