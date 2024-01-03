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

package org.springframework.web.reactive.function.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

/**
 * {@link WebClient}的默认实现.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 5.0
 */
class DefaultWebClient implements WebClient {
	/**
	 * URI 模板属性的键名
	 */
	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	/**
	 * 表示没有 HTTP 客户端响应的 Mono
	 */
	private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
			() -> new IllegalStateException("The underlying HTTP client completed without emitting a response."));

	/**
	 * 用于执行 HTTP 请求的 ExchangeFunction
	 */
	private final ExchangeFunction exchangeFunction;

	/**
	 * URI 构建工厂
	 */
	private final UriBuilderFactory uriBuilderFactory;

	/**
	 * 默认的 HttpHeaders
	 */
	@Nullable
	private final HttpHeaders defaultHeaders;

	/**
	 * 默认的 Cookies
	 */
	@Nullable
	private final MultiValueMap<String, String> defaultCookies;

	/**
	 * 默认的请求头规范消费者
	 */
	@Nullable
	private final Consumer<RequestHeadersSpec<?>> defaultRequest;

	/**
	 * 默认的 WebClientBuilder
	 */
	private final DefaultWebClientBuilder builder;


	/**
	 * 构造函数，初始化 DefaultWebClient 实例
	 *
	 * @param exchangeFunction  用于执行 HTTP 请求的 ExchangeFunction
	 * @param uriBuilderFactory URI 构建工厂
	 * @param defaultHeaders    默认的 HttpHeaders
	 * @param defaultCookies    默认的 Cookies
	 * @param defaultRequest    默认的请求头规范消费者
	 * @param builder           默认的 WebClientBuilder
	 */
	DefaultWebClient(ExchangeFunction exchangeFunction, UriBuilderFactory uriBuilderFactory,
					 @Nullable HttpHeaders defaultHeaders, @Nullable MultiValueMap<String, String> defaultCookies,
					 @Nullable Consumer<RequestHeadersSpec<?>> defaultRequest, DefaultWebClientBuilder builder) {

		this.exchangeFunction = exchangeFunction;
		this.uriBuilderFactory = uriBuilderFactory;
		this.defaultHeaders = defaultHeaders;
		this.defaultCookies = defaultCookies;
		this.defaultRequest = defaultRequest;
		this.builder = builder;
	}


	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public RequestHeadersUriSpec<?> head() {
		return methodInternal(HttpMethod.HEAD);
	}

	@Override
	public RequestBodyUriSpec post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public RequestBodyUriSpec put() {
		return methodInternal(HttpMethod.PUT);
	}

	@Override
	public RequestBodyUriSpec patch() {
		return methodInternal(HttpMethod.PATCH);
	}

	@Override
	public RequestHeadersUriSpec<?> delete() {
		return methodInternal(HttpMethod.DELETE);
	}

	@Override
	public RequestHeadersUriSpec<?> options() {
		return methodInternal(HttpMethod.OPTIONS);
	}

	@Override
	public RequestBodyUriSpec method(HttpMethod httpMethod) {
		return methodInternal(httpMethod);
	}

	/**
	 * 内部方法，创建一个 RequestBodyUriSpec 对象，用于指定 HTTP 方法
	 *
	 * @param httpMethod HTTP 方法
	 * @return 返回一个 RequestBodyUriSpec 对象
	 */
	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(httpMethod);
	}

	/**
	 * 创建一个用于构建新 WebClient 实例的 Builder
	 *
	 * @return 返回一个 Builder 对象，用于构建新的 WebClient 实例
	 */
	@Override
	public Builder mutate() {
		return new DefaultWebClientBuilder(this.builder);
	}

	/**
	 * 释放未被消耗的 ClientResponse
	 *
	 * @param response 未被消耗的 ClientResponse 对象
	 * @return 返回一个 Mono<Void>，表示释放未被消耗的响应
	 */
	private static Mono<Void> releaseIfNotConsumed(ClientResponse response) {
		return response.releaseBody().onErrorResume(ex2 -> Mono.empty());
	}

	/**
	 * 释放未被消耗的 ClientResponse，并在发生异常时触发错误信号
	 *
	 * @param response 未被消耗的 ClientResponse 对象
	 * @param ex       异常信息
	 * @param <T>      泛型类型
	 * @return 返回一个 Mono<T>，表示释放未被消耗的响应并触发指定的异常
	 */
	private static <T> Mono<T> releaseIfNotConsumed(ClientResponse response, Throwable ex) {
		return response.releaseBody().onErrorResume(ex2 -> Mono.empty()).then(Mono.error(ex));
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		/**
		 * HTTP 请求的方法类型
		 */
		private final HttpMethod httpMethod;

		/**
		 * 请求的 URI
		 */
		@Nullable
		private URI uri;

		/**
		 * 请求的 HttpHeaders
		 */
		@Nullable
		private HttpHeaders headers;

		/**
		 * 请求的 Cookies
		 */
		@Nullable
		private MultiValueMap<String, String> cookies;

		/**
		 * 请求的 BodyInserter，用于处理请求体
		 */
		@Nullable
		private BodyInserter<?, ? super ClientHttpRequest> inserter;

		/**
		 * 请求的属性映射
		 */
		private final Map<String, Object> attributes = new LinkedHashMap<>(4);

		/**
		 * 请求的上下文修改器函数
		 */
		@Nullable
		private Function<Context, Context> contextModifier;

		/**
		 * HTTP 请求的消费者函数
		 */
		@Nullable
		private Consumer<ClientHttpRequest> httpRequestConsumer;


		/**
		 * 构造函数，接收 HTTP 方法并初始化 DefaultRequestBodyUriSpec 对象
		 *
		 * @param httpMethod HTTP 方法
		 */
		DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		/**
		 * 使用 URI 模板和变量展开请求的 URI
		 *
		 * @param uriTemplate  URI 模板字符串
		 * @param uriVariables URI 变量
		 * @return 返回一个 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			// 设置 URI 模板属性，并通过变量展开设置 URI
			attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		/**
		 * 使用 URI 模板和变量 Map 展开请求的 URI
		 *
		 * @param uriTemplate  URI 模板字符串
		 * @param uriVariables URI 变量 Map
		 * @return 返回一个 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			// 设置 URI 模板属性，并通过变量 Map 展开设置 URI
			attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		/**
		 * 设置请求的 URI，使用 URI 模板和 URI 构建器函数
		 *
		 * @param uriTemplate URI 模板字符串
		 * @param uriFunction URI 构建器函数
		 * @return 返回一个 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec uri(String uriTemplate, Function<UriBuilder, URI> uriFunction) {
			// 设置 URI 模板属性，并通过 URI 构建器函数设置 URI
			attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
			return uri(uriFunction.apply(uriBuilderFactory.uriString(uriTemplate)));
		}

		/**
		 * 设置请求的 URI，使用 URI 构建器函数
		 *
		 * @param uriFunction URI 构建器函数
		 * @return 返回一个 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			// 通过 URI 构建器函数设置 URI
			return uri(uriFunction.apply(uriBuilderFactory.builder()));
		}

		/**
		 * 设置请求的 URI
		 *
		 * @param uri 请求的 URI
		 * @return 返回一个 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec uri(URI uri) {
			// 设置请求的 URI，并返回 RequestBodySpec 对象
			this.uri = uri;
			return this;
		}

		/**
		 * 获取请求的 HttpHeaders 对象
		 *
		 * @return 返回一个 HttpHeaders 对象
		 */
		private HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
			}
			return this.headers;
		}

		/**
		 * 获取 Cookies
		 *
		 * @return 返回一个 MultiValueMap 包含 Cookies
		 */
		private MultiValueMap<String, String> getCookies() {
			if (this.cookies == null) {
				this.cookies = new LinkedMultiValueMap<>(3);
			}
			return this.cookies;
		}

		/**
		 * 设置请求的单个头信息
		 *
		 * @param headerName   头信息名称
		 * @param headerValues 头信息值数组
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				// 将给定的头信息添加到请求头中，并返回 DefaultRequestBodyUriSpec 对象
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		/**
		 * 设置请求的多个头信息，通过消费者函数接受请求头映射
		 *
		 * @param headersConsumer 请求头映射的消费者函数
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec headers(Consumer<HttpHeaders> headersConsumer) {
			// 将请求头映射传递给消费者函数进行处理，并返回 DefaultRequestBodyUriSpec 对象
			headersConsumer.accept(getHeaders());
			return this;
		}

		/**
		 * 设置请求的可接受媒体类型
		 *
		 * @param acceptableMediaTypes 可接受的媒体类型数组
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec accept(MediaType... acceptableMediaTypes) {
			// 设置请求的可接受媒体类型，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		/**
		 * 设置请求的可接受字符集
		 *
		 * @param acceptableCharsets 可接受的字符集数组
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec acceptCharset(Charset... acceptableCharsets) {
			// 将可接受的字符集设置到请求头中，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		/**
		 * 设置请求的内容类型
		 *
		 * @param contentType 请求的内容类型
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec contentType(MediaType contentType) {
			// 设置请求的内容类型，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setContentType(contentType);
			return this;
		}

		/**
		 * 设置请求的内容长度
		 *
		 * @param contentLength 请求的内容长度
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec contentLength(long contentLength) {
			// 设置请求的内容长度，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setContentLength(contentLength);
			return this;
		}

		/**
		 * 设置请求的 Cookie
		 *
		 * @param name  Cookie 名称
		 * @param value Cookie 值
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec cookie(String name, String value) {
			// 将给定的 Cookie 名称和值添加到 Cookie 中，并返回 DefaultRequestBodyUriSpec 对象
			getCookies().add(name, value);
			return this;
		}

		/**
		 * 设置请求的多个 Cookie，通过消费者函数接受 Cookie 映射
		 *
		 * @param cookiesConsumer Cookie 映射的消费者函数
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			// 将 Cookie 映射传递给消费者函数进行处理，并返回 DefaultRequestBodyUriSpec 对象
			cookiesConsumer.accept(getCookies());
			return this;
		}

		/**
		 * 设置请求的 If-Modified-Since 头信息，用于条件检查
		 *
		 * @param ifModifiedSince If-Modified-Since 头的时间
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			// 设置 If-Modified-Since 头为给定的时间，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setIfModifiedSince(ifModifiedSince);
			return this;
		}

		/**
		 * 设置请求的 If-None-Match 头信息，用于条件检查
		 *
		 * @param ifNoneMatches If-None-Match 头的值数组
		 * @return 返回一个 DefaultRequestBodyUriSpec 对象
		 */
		@Override
		public DefaultRequestBodyUriSpec ifNoneMatch(String... ifNoneMatches) {
			// 将 If-None-Match 头设置为给定的值列表，并返回 DefaultRequestBodyUriSpec 对象
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		/**
		 * 设置请求的属性
		 *
		 * @param name  属性名
		 * @param value 属性值
		 * @return 返回当前的 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec attribute(String name, Object value) {
			// 将属性名和值放入属性映射中，并返回当前 RequestBodySpec 对象
			this.attributes.put(name, value);
			return this;
		}

		/**
		 * 设置请求的多个属性，通过消费者函数接受属性映射
		 *
		 * @param attributesConsumer 属性映射的消费者函数
		 * @return 返回当前的 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			// 将属性映射传递给消费者函数进行处理，并返回当前 RequestBodySpec 对象
			attributesConsumer.accept(this.attributes);
			return this;
		}

		/**
		 * 设置请求的上下文修改器
		 *
		 * @param contextModifier 上下文修改器函数，用于修改请求的上下文信息
		 * @return 返回当前的 RequestBodySpec 对象
		 * @deprecated 该方法已被弃用
		 */
		@Override
		@SuppressWarnings("deprecation")
		public RequestBodySpec context(Function<Context, Context> contextModifier) {
			// 将上下文修改器函数设置到当前请求规范对象中，并返回
			this.contextModifier = (this.contextModifier != null ?
					this.contextModifier.andThen(contextModifier) : contextModifier);
			return this;
		}

		/**
		 * 设置 HTTP 请求的消费者
		 *
		 * @param requestConsumer 请求消费者，用于对 ClientHttpRequest 进行自定义操作
		 * @return 返回当前的 RequestBodySpec 对象
		 */
		@Override
		public RequestBodySpec httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
			// 将请求消费者设置到当前请求规范对象中，并返回
			this.httpRequestConsumer = (this.httpRequestConsumer != null ?
					this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
			return this;
		}

		/**
		 * 设置请求体的值
		 *
		 * @param body 请求体的值
		 * @return 返回一个 RequestHeadersSpec 对象
		 */
		@Override
		public RequestHeadersSpec<?> bodyValue(Object body) {
			// 使用给定的值创建请求体插入器，并将其设置到当前请求规范对象中
			this.inserter = BodyInserters.fromValue(body);
			return this;
		}

		/**
		 * 设置请求体为指定的发布者对象和参数化类型引用
		 *
		 * @param publisher      请求体的发布者对象
		 * @param elementTypeRef 参数化类型引用，用于指定请求体的类型
		 * @param <T>            请求体元素的类型
		 * @param <P>            发布者对象类型
		 * @return 返回当前的 RequestHeadersSpec 对象
		 */
		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(
				P publisher, ParameterizedTypeReference<T> elementTypeRef) {
			// 使用给定的发布者对象和参数化类型引用创建请求体插入器，并将其设置到当前请求规范对象中
			this.inserter = BodyInserters.fromPublisher(publisher, elementTypeRef);
			return this;
		}

		/**
		 * 设置请求体为指定的发布者对象和类
		 *
		 * @param publisher    请求体的发布者对象
		 * @param elementClass 类型引用，用于指定请求体的类型
		 * @param <T>          请求体元素的类型
		 * @param <P>          发布者对象类型
		 * @return 返回当前的 RequestHeadersSpec 对象
		 */
		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass) {
			// 使用给定的发布者对象和类创建请求体插入器，并将其设置到当前请求规范对象中
			this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
			return this;
		}

		/**
		 * 设置请求体为指定的生产者对象和类
		 *
		 * @param producer     请求体的生产者对象
		 * @param elementClass 类型引用，用于指定请求体的类型
		 * @return 返回当前的 RequestHeadersSpec 对象
		 */
		@Override
		public RequestHeadersSpec<?> body(Object producer, Class<?> elementClass) {
			// 使用给定的生产者对象和类创建请求体插入器，并将其设置到当前请求规范对象中
			this.inserter = BodyInserters.fromProducer(producer, elementClass);
			return this;
		}

		/**
		 * 设置请求体为指定的生产者对象和参数化类型引用
		 *
		 * @param producer       请求体的生产者对象
		 * @param elementTypeRef 参数化类型引用，用于指定请求体的类型
		 * @return 返回当前的 RequestHeadersSpec 对象
		 */
		@Override
		public RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
			// 使用给定的生产者对象和参数化类型引用创建请求体插入器，并将其设置到当前请求规范对象中
			this.inserter = BodyInserters.fromProducer(producer, elementTypeRef);
			return this;
		}

		/**
		 * 设置请求体插入器到当前请求规范对象
		 *
		 * @param inserter 请求体插入器，用于处理请求体的插入
		 * @return 返回当前的 RequestHeadersSpec 对象
		 */
		@Override
		public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
			// 将请求体插入器设置到当前请求规范对象中并返回
			this.inserter = inserter;
			return this;
		}

		/**
		 * 重写父类方法，将同步请求体设置为指定的对象
		 *
		 * @param body 要设置的请求体对象
		 * @return 返回一个 RequestHeadersSpec 对象
		 * @deprecated 该方法已被弃用
		 */
		@Override
		@Deprecated
		public RequestHeadersSpec<?> syncBody(Object body) {
			// 调用 bodyValue 方法设置请求体，并返回 RequestHeadersSpec 对象
			return bodyValue(body);
		}

		/**
		 * 获取响应规范
		 *
		 * @return 返回一个响应规范对象
		 */
		@Override
		public ResponseSpec retrieve() {
			// 创建并返回一个默认的响应规范对象，使用当前对象的 exchange 方法和 createRequest 方法
			return new DefaultResponseSpec(exchange(), this::createRequest);
		}

		/**
		 * 创建一个 HttpRequest 对象
		 *
		 * @return 返回一个 HttpRequest 对象
		 */
		private HttpRequest createRequest() {
			return new HttpRequest() {
				/**
				 * 初始化 URI
				 */
				private final URI uri = initUri();
				/**
				 * 初始化 HttpHeaders
				 */
				private final HttpHeaders headers = initHeaders();

				/**
				 * 获取 HTTP 方法
				 *
				 * @return 返回 HTTP 方法
				 */
				@Override
				public HttpMethod getMethod() {
					return httpMethod;
				}

				/**
				 * 获取 HTTP 方法的字符串表示
				 *
				 * @return 返回 HTTP 方法的字符串表示
				 */
				@Override
				public String getMethodValue() {
					return httpMethod.name();
				}

				/**
				 * 获取 URI
				 *
				 * @return 返回 URI
				 */
				@Override
				public URI getURI() {
					return this.uri;
				}

				/**
				 * 获取 HttpHeaders
				 *
				 * @return 返回 HttpHeaders
				 */
				@Override
				public HttpHeaders getHeaders() {
					return this.headers;
				}
			};
		}

		/**
		 * 将响应处理为一个 Mono
		 *
		 * @param responseHandler 处理响应的函数，将 ClientResponse 转换为 Mono
		 * @param <V>             泛型类型，表示 Mono 中的元素类型
		 * @return 返回一个 Mono
		 */
		@Override
		public <V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler) {
			return exchange().flatMap(response -> {
				try {
					// 尝试处理响应并将其转换为 Mono
					return responseHandler.apply(response)
							// 处理完响应后释放资源，并返回响应结果
							.flatMap(value -> releaseIfNotConsumed(response).thenReturn(value))
							// 如果响应为空，则释放资源并返回一个空的 Mono
							.switchIfEmpty(Mono.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
							// 在出现错误时捕获并处理
							.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
				} catch (Throwable ex) {
					// 处理异常情况并释放资源
					return releaseIfNotConsumed(response, ex);
				}
			});
		}

		/**
		 * 将响应处理为一个 Flux 流
		 *
		 * @param responseHandler 处理响应的函数，将 ClientResponse 转换为 Flux 流
		 * @param <V>             泛型类型，表示 Flux 流中的元素类型
		 * @return 返回一个 Flux 流
		 */
		@Override
		public <V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler) {
			return exchange().flatMapMany(response -> {
				try {
					// 尝试处理响应并将其转换为 Flux 流
					return responseHandler.apply(response)
							// 将处理后的流与释放资源的操作连接起来
							.concatWith(Flux.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
							// 在出现错误时捕获并处理
							.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
				} catch (Throwable ex) {
					// 处理异常情况并释放资源
					return releaseIfNotConsumed(response, ex);
				}
			});
		}

		/**
		 * 发出客户端请求
		 *
		 * @return 响应的{@code Mono}
		 */
		@Override
		@SuppressWarnings("deprecation")
		public Mono<ClientResponse> exchange() {
			// 创建客户端请求
			ClientRequest request = (this.inserter != null ?
					initRequestBuilder().body(this.inserter).build() :
					initRequestBuilder().build());
			return Mono.defer(() -> {
				// 发出请求并获取响应的 Mono
				Mono<ClientResponse> responseMono = exchangeFunction.exchange(request)
						.checkpoint("Request to " + this.httpMethod.name() + " " + this.uri + " [DefaultWebClient]")
						.switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR);
				// 如果存在上下文修改器，则应用上下文修改器到响应的 Mono
				if (this.contextModifier != null) {
					responseMono = responseMono.contextWrite(this.contextModifier);
				}
				return responseMono;
			});
		}


		/**
		 * 初始化请求构建器的方法。根据当前实例的属性，构建一个客户端请求构建器。
		 * 如果设置了默认请求 defaultRequest，则使用其设置；否则，根据当前实例的属性构建请求构建器。
		 *
		 * @return 客户端请求构建器，用于创建 HTTP 请求
		 */
		private ClientRequest.Builder initRequestBuilder() {
			if (defaultRequest != null) {
				// 如果存在默认请求，将当前请求的属性应用到默认请求中
				defaultRequest.accept(this);
			}

			// 创建一个新的 ClientRequest.Builder，并设置初始的 HTTP 方法和 URI
			ClientRequest.Builder builder = ClientRequest.create(this.httpMethod, initUri())
					// 设置初始的请求头部信息
					.headers(headers -> headers.addAll(initHeaders()))
					// 设置初始的 cookies
					.cookies(cookies -> cookies.addAll(initCookies()))
					// 设置初始的属性
					.attributes(attributes -> attributes.putAll(this.attributes));

			if (this.httpRequestConsumer != null) {
				// 如果存在 HTTP 请求消费者，将其应用到请求构建器中
				builder.httpRequest(this.httpRequestConsumer);
			}
			return builder;
		}


		/**
		 * 初始化 URI 的方法。
		 * 如果当前实例的 uri 属性不为空，则返回该属性所表示的 URI。
		 * 如果当前实例的 uri 属性为空，则使用 uriBuilderFactory 创建一个空的 URI。
		 *
		 * @return 初始化后的 URI 实例，用于表示请求的 URI
		 */
		private URI initUri() {
			return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
		}

		/**
		 * 初始化 Headers 的方法。
		 *
		 * @return 初始化后的 HttpHeaders 实例，用于表示请求头信息
		 */
		private HttpHeaders initHeaders() {
			if (CollectionUtils.isEmpty(this.headers)) {
				// 如果当前 headers 为空，则返回默认的 headers，如果默认 headers 也为空，则返回一个空的 HttpHeaders
				return (defaultHeaders != null ? defaultHeaders : new HttpHeaders());
			} else if (CollectionUtils.isEmpty(defaultHeaders)) {
				// 如果默认 headers 为空，则返回当前 headers
				return this.headers;
			} else {
				// 合并当前 headers 和默认 headers，并返回合并后的结果
				HttpHeaders result = new HttpHeaders();
				result.putAll(defaultHeaders);
				result.putAll(this.headers);
				return result;
			}
		}

		/**
		 * 初始化 Cookie 的方法。
		 *
		 * @return 初始化后的 MultiValueMap，用于表示 Cookies
		 */
		private MultiValueMap<String, String> initCookies() {
			if (CollectionUtils.isEmpty(this.cookies)) {
				// 如果当前 cookies 为空，则返回默认的 cookies，如果默认 cookies 也为空，则返回一个空的 MultiValueMap
				return (defaultCookies != null ? defaultCookies : new LinkedMultiValueMap<>());
			} else if (CollectionUtils.isEmpty(defaultCookies)) {
				// 如果默认 cookies 为空，则返回当前 cookies
				return this.cookies;
			} else {
				// 合并当前 cookies 和默认 cookies，并返回合并后的结果
				MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
				result.putAll(defaultCookies);
				result.putAll(this.cookies);
				return result;
			}
		}
	}


	private static class DefaultResponseSpec implements ResponseSpec {

		/**
		 * 检查状态码是否为错误状态的谓词
		 */
		private static final IntPredicate STATUS_CODE_ERROR = (value -> value >= 400);

		/**
		 * 默认的状态处理器，根据状态码创建异常
		 */
		private static final StatusHandler DEFAULT_STATUS_HANDLER =
				new StatusHandler(STATUS_CODE_ERROR, ClientResponse::createException);

		/**
		 * 表示 HTTP 响应的 Mono
		 */
		private final Mono<ClientResponse> responseMono;

		/**
		 * 用于提供 HttpRequest 实例的 Supplier
		 */
		private final Supplier<HttpRequest> requestSupplier;

		/**
		 * 状态处理器列表
		 */
		private final List<StatusHandler> statusHandlers = new ArrayList<>(1);


		DefaultResponseSpec(Mono<ClientResponse> responseMono, Supplier<HttpRequest> requestSupplier) {
			this.responseMono = responseMono;
			this.requestSupplier = requestSupplier;
			this.statusHandlers.add(DEFAULT_STATUS_HANDLER);
		}


		/**
		 * 根据 HttpStatus 的断言和异常函数配置状态处理器
		 *
		 * @param statusPredicate     HttpStatus 的断言，用于匹配状态码
		 * @param exceptionFunction   根据 ClientResponse 创建异常的函数
		 * @return 返回一个 ResponseSpec 对象
		 */
		@Override
		public ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
									 Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {
			// 将 HttpStatus 的断言转换为 IntPredicate，并使用异常函数配置状态处理器
			return onRawStatus(toIntPredicate(statusPredicate), exceptionFunction);
		}

		/**
		 * 将 HttpStatus 的断言转换为 IntPredicate
		 *
		 * @param predicate HttpStatus 的断言
		 * @return 返回一个 IntPredicate 对象，用于匹配整数状态码
		 */
		private static IntPredicate toIntPredicate(Predicate<HttpStatus> predicate) {
			return value -> {
				HttpStatus status = HttpStatus.resolve(value);
				return (status != null && predicate.test(status));
			};
		}

		/**
		 * 根据整数状态码断言和异常函数配置状态处理器
		 *
		 * @param statusCodePredicate 整数状态码的断言，用于匹配状态码
		 * @param exceptionFunction   根据 ClientResponse 创建异常的函数
		 * @return 返回一个 ResponseSpec 对象
		 */
		@Override
		public ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
										Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {
			// 确保 statusCodePredicate 和 exceptionFunction 不为 null
			Assert.notNull(statusCodePredicate, "IntPredicate must not be null");
			Assert.notNull(exceptionFunction, "Function must not be null");

			// 将状态处理器添加到列表中，确保默认处理器始终位于最后一个位置
			// 默认处理器始终位于最后一个位置
			int index = this.statusHandlers.size() - 1;
			this.statusHandlers.add(index, new StatusHandler(statusCodePredicate, exceptionFunction));
			return this;
		}

		/**
		 * 将响应的主体内容转换为 Mono<T>
		 *
		 * @param elementClass 要转换的类
		 * @param <T>          泛型类型
		 * @return 返回一个 Mono<T>，表示响应的主体内容
		 */
		@Override
		public <T> Mono<T> bodyToMono(Class<T> elementClass) {
			Assert.notNull(elementClass, "Class must not be null");
			return this.responseMono.flatMap(response ->
					handleBodyMono(response, response.bodyToMono(elementClass)));
		}

		/**
		 * 将响应的主体内容转换为 Mono<T>，使用 ParameterizedTypeReference 类型引用
		 *
		 * @param elementTypeRef 参数化类型的引用
		 * @param <T>            泛型类型
		 * @return 返回一个 Mono<T>，表示响应的主体内容
		 */
		@Override
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
			Assert.notNull(elementTypeRef, "ParameterizedTypeReference must not be null");
			return this.responseMono.flatMap(response ->
					handleBodyMono(response, response.bodyToMono(elementTypeRef)));
		}

		/**
		 * 将响应的主体内容转换为 Flux<T>
		 *
		 * @param elementClass 要转换的类
		 * @param <T>          泛型类型
		 * @return 返回一个 Flux<T>，表示响应的主体内容
		 */
		@Override
		public <T> Flux<T> bodyToFlux(Class<T> elementClass) {
			Assert.notNull(elementClass, "Class must not be null");
			return this.responseMono.flatMapMany(response ->
					handleBodyFlux(response, response.bodyToFlux(elementClass)));
		}

		/**
		 * 将响应的主体内容转换为 Flux<T>，使用 ParameterizedTypeReference 类型引用
		 *
		 * @param elementTypeRef 参数化类型的引用
		 * @param <T>            泛型类型
		 * @return 返回一个 Flux<T>，表示响应的主体内容
		 */
		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
			Assert.notNull(elementTypeRef, "ParameterizedTypeReference must not be null");
			return this.responseMono.flatMapMany(response ->
					handleBodyFlux(response, response.bodyToFlux(elementTypeRef)));
		}

		/**
		 * 将响应的主体内容转换为包含 ResponseEntity<T> 的 Mono
		 *
		 * @param bodyClass 要转换的类
		 * @param <T>       泛型类型
		 * @return 返回一个 Mono<ResponseEntity<T>>，表示响应的主体内容包装成 ResponseEntity
		 */
		@Override
		public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response,
							handleBodyMono(response, response.bodyToMono(bodyClass))));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<T>
		 *
		 * @param bodyTypeRef 要转换的参数化类型引用
		 * @param <T>         泛型类型
		 * @return 返回一个 Mono<ResponseEntity<T>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeRef) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response,
							handleBodyMono(response, response.bodyToMono(bodyTypeRef))));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<List<T>>
		 *
		 * @param elementClass 要转换的列表元素类
		 * @param <T>          泛型类型
		 * @return 返回一个 Mono<ResponseEntity<List<T>>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntityList(response,
							handleBodyFlux(response, response.bodyToFlux(elementClass))));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<List<T>>
		 *
		 * @param elementTypeRef 要转换的列表元素参数化类型引用
		 * @param <T>            泛型类型
		 * @return 返回一个 Mono<ResponseEntity<List<T>>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntityList(response,
							handleBodyFlux(response, response.bodyToFlux(elementTypeRef))));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<Flux<T>>
		 *
		 * @param elementType 要转换的 Flux 元素类
		 * @param <T>         泛型类型
		 * @return 返回一个 Mono<ResponseEntity<Flux<T>>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.bodyToFlux(elementType)));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<Flux<T>>，使用 ParameterizedTypeReference 类型引用
		 *
		 * @param elementTypeRef 要转换的 Flux 元素参数化类型引用
		 * @param <T>            泛型类型
		 * @return 返回一个 Mono<ResponseEntity<Flux<T>>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeRef) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.bodyToFlux(elementTypeRef)));
		}

		/**
		 * 将响应的主体内容转换为 ResponseEntity<Flux<T>>，使用自定义 BodyExtractor
		 *
		 * @param bodyExtractor 自定义的 BodyExtractor 对象
		 * @param <T>           泛型类型
		 * @return 返回一个 Mono<ResponseEntity<Flux<T>>>，表示响应的主体内容和元数据
		 */
		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.body(bodyExtractor)));
		}

		/**
		 * 将响应转换为 ResponseEntity<Void>，不包含主体内容
		 *
		 * @return 返回一个 Mono<ResponseEntity<Void>>，表示响应的元数据
		 */
		@Override
		public Mono<ResponseEntity<Void>> toBodilessEntity() {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response, handleBodyMono(response, Mono.<Void>empty()))
							// 将响应体映射到实体对象，并继续处理空的 Mono
							.flatMap(entity -> response.releaseBody()
									// 在释放响应体后，处理异常并返回之前映射的实体对象
									.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response))
									.thenReturn(entity))
			);
		}

		/**
		 * 处理响应的主体内容 Mono
		 *
		 * @param response 响应对象
		 * @param body     响应的主体内容 Mono
		 * @param <T>      泛型类型
		 * @return 返回一个 Mono<T>，表示响应的主体内容
		 */
		private <T> Mono<T> handleBodyMono(ClientResponse response, Mono<T> body) {
			body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));

			Mono<T> result = applyStatusHandlers(response);

			// 如果 result 不为空，则返回 result，否则返回 body，如果 result 是空的，则返回 body
			return (result != null ? result.switchIfEmpty(body) : body);
		}

		/**
		 * 处理响应的主体内容 Flux
		 *
		 * @param response 响应对象
		 * @param body     响应的主体内容 Flux
		 * @param <T>      泛型类型
		 * @return 返回一个 Publisher<T>，表示响应的主体内容
		 */
		private <T> Publisher<T> handleBodyFlux(ClientResponse response, Flux<T> body) {
			body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));

			Mono<T> result = applyStatusHandlers(response);

			// 如果 result 不为空，则返回 result 的 flux，否则返回 body，如果 result 的 flux 是空的，则返回 body
			return (result != null ? result.flux().switchIfEmpty(body) : body);
		}

		/**
		 * 处理响应的 ResponseEntity<Flux<T>>
		 *
		 * @param response 响应对象
		 * @param body     响应的主体内容 Flux
		 * @param <T>      泛型类型
		 * @return 返回一个 Mono<? extends ResponseEntity<Flux<T>>>，表示响应的主体内容和元数据
		 */
		private <T> Mono<? extends ResponseEntity<Flux<T>>> handlerEntityFlux(ClientResponse response, Flux<T> body) {
			// 创建一个 ResponseEntity 对象，包含一个 Flux<T> 类型的 body，通过 onErrorResume 处理异常
			ResponseEntity<Flux<T>> entity = new ResponseEntity<>(
					body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response)),
					// 获取响应的 HTTP 头信息
					response.headers().asHttpHeaders(),
					// 获取原始的 HTTP 状态码
					response.rawStatusCode()
			);

			// 应用状态处理器，返回一个 Mono 包装的 ResponseEntity<Flux<T>> 对象
			Mono<ResponseEntity<Flux<T>>> result = applyStatusHandlers(response);

			// 如果 result 不为 null，则返回 result；否则返回一个默认值为 entity 的 Mono 对象
			return (result != null ? result.defaultIfEmpty(entity) : Mono.just(entity));
		}

		/**
		 * 创建用于包装异常的函数，用于响应的异常处理
		 *
		 * @param response 响应对象
		 * @param <T>      泛型类型
		 * @return 返回一个 Function<Throwable, Mono<? extends T>> 对象，用于异常包装处理
		 */
		private <T> Function<Throwable, Mono<? extends T>> exceptionWrappingFunction(ClientResponse response) {
			return t -> response.createException().flatMap(ex -> Mono.error(ex.initCause(t)));
		}

		/**
		 * 应用状态处理器来处理响应的状态码
		 *
		 * @param response 响应对象
		 * @param <T>      泛型类型
		 * @return 返回一个 Mono<T>，表示根据状态处理器处理后的结果
		 */
		@Nullable
		private <T> Mono<T> applyStatusHandlers(ClientResponse response) {
			// 获取响应的原始状态码
			int statusCode = response.rawStatusCode();

			// 遍历注册的状态处理器列表
			for (StatusHandler handler : this.statusHandlers) {
				// 检查处理器是否匹配当前状态码
				if (handler.test(statusCode)) {
					Mono<? extends Throwable> exMono;
					try {
						// 应用匹配的状态处理器来处理响应
						exMono = handler.apply(response);
						// 如果发生错误，释放资源并处理异常
						exMono = exMono.flatMap(ex -> releaseIfNotConsumed(response, ex));
						exMono = exMono.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
					} catch (Throwable ex2) {
						// 如果处理器抛出异常，释放资源并处理异常
						exMono = releaseIfNotConsumed(response, ex2);
					}
					// 将异常转换为 Mono 的错误信号
					Mono<T> result = exMono.flatMap(Mono::error);
					// 获取当前请求
					HttpRequest request = this.requestSupplier.get();
					// 在结果中插入检查点信息，并返回
					return insertCheckpoint(result, statusCode, request);
				}
			}

			// 如果没有匹配的处理器，返回 null
			return null;
		}

		/**
		 * 插入检查点描述到结果的 Mono 中
		 *
		 * @param result      要插入检查点的 Mono 结果
		 * @param statusCode  响应的状态码
		 * @param request     发送请求的 HttpRequest 对象
		 * @param <T>         泛型类型
		 * @return 返回一个带有检查点描述的 Mono<T>
		 */
		private <T> Mono<T> insertCheckpoint(Mono<T> result, int statusCode, HttpRequest request) {
			// 获取 HTTP 请求的方法（GET、POST 等）
			String httpMethod = request.getMethodValue();

			// 获取请求的 URI
			URI uri = request.getURI();

			// 构建检查点描述信息，包括状态码、HTTP 方法、URI 和标识
			String description = statusCode + " from " + httpMethod + " " + uri + " [DefaultWebClient]";

			// 将描述信息设置为结果的检查点并返回
			return result.checkpoint(description);
		}


		private static class StatusHandler {
			/**
			 * 整型断言
			 */
			private final IntPredicate predicate;
			/**
			 * 异常函数
			 */
			private final Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction;

			/**
			 * 构造函数，用于指定状态谓词和处理异常的函数。
			 *
			 * @param predicate         状态谓词
			 * @param exceptionFunction 处理异常的函数
			 */
			public StatusHandler(IntPredicate predicate,
								 Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

				this.predicate = predicate;
				this.exceptionFunction = exceptionFunction;
			}

			/**
			 * 检查给定的状态是否满足谓词。
			 *
			 * @param status 待检查的状态码
			 * @return 如果状态满足谓词则为true，否则为false
			 */
			public boolean test(int status) {
				return this.predicate.test(status);
			}

			/**
			 * 应用处理异常的函数到给定的响应。
			 *
			 * @param response 响应对象
			 * @return 返回处理异常的Mono
			 */
			public Mono<? extends Throwable> apply(ClientResponse response) {
				return this.exceptionFunction.apply(response);
			}
		}
	}

}
