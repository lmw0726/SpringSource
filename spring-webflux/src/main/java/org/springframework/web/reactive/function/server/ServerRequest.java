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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
/**
 * 表示由 {@code HandlerFunction} 处理的服务器端 HTTP 请求。
 *
 * <p>通过 {@link Headers} 和 {@link #body(BodyExtractor)} 提供对头部和正文的访问。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerRequest {

	/**
	 * 获取 HTTP 方法。
	 *
	 * @return HTTP 方法作为 HttpMethod 枚举值，如果无法解析（例如非标准的 HTTP 方法），则返回 {@code null}
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * 获取 HTTP 方法的名称。
	 *
	 * @return HTTP 方法作为字符串
	 */
	String methodName();

	/**
	 * 获取请求的 URI。
	 */
	URI uri();

	/**
	 * 从与此 {@code ServerRequest} 相关联的 URI 获取 {@code UriBuilderComponents}。
	 * <p><strong>注意：</strong>从 5.1 版本开始，此方法会忽略指定客户端来源地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头部。
	 * 考虑使用 {@code ForwardedHeaderFilter} 提取和使用这些头部，或者丢弃这样的头部。
	 *
	 * @return 一个 URI 构建器
	 */
	UriBuilder uriBuilder();

	/**
	 * 获取请求路径。
	 */
	default String path() {
		return requestPath().pathWithinApplication().value();
	}

	/**
	 * 以 {@code PathContainer} 形式获取请求路径。
	 *
	 * @deprecated 自 5.3 起，推荐使用 {@link #requestPath()}
	 */
	@Deprecated
	default PathContainer pathContainer() {
		return requestPath();
	}

	/**
	 * 以 {@code PathContainer} 形式获取请求路径。
	 *
	 * @since 5.3
	 */
	default RequestPath requestPath() {
		return exchange().getRequest().getPath();
	}

	/**
	 * 获取此请求的头部信息。
	 */
	Headers headers();

	/**
	 * 获取此请求的 Cookies。
	 */
	MultiValueMap<String, HttpCookie> cookies();

	/**
	 * 获取此请求连接的远程地址（如果可用）。
	 *
	 * @since 5.1
	 */
	Optional<InetSocketAddress> remoteAddress();
	/**
	 * 获取此请求连接的本地地址（如果可用）。
	 *
	 * @since 5.2.3
	 */
	Optional<InetSocketAddress> localAddress();

	/**
	 * 获取用于转换此请求体的读取器。
	 *
	 * @since 5.1
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * 使用给定的 {@code BodyExtractor} 提取请求体。
	 *
	 * @param extractor 从请求中读取的 {@code BodyExtractor}
	 * @param <T> 返回的请求体类型
	 * @return 提取的请求体
	 * @see #body(BodyExtractor, Map)
	 */
	<T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor);

	/**
	 * 使用给定的 {@code BodyExtractor} 和提示提取请求体。
	 *
	 * @param extractor 从请求中读取的 {@code BodyExtractor}
	 * @param hints 自定义请求体提取的提示信息，比如 {@link Jackson2CodecSupport#JSON_VIEW_HINT}
	 * @param <T> 返回的请求体类型
	 * @return 提取的请求体
	 */
	<T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints);

	/**
	 * 提取请求体为 {@code Mono}。
	 *
	 * @param elementClass {@code Mono} 中元素的类
	 * @param <T> 元素类型
	 * @return 作为 mono 的请求体
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * 提取请求体为 {@code Mono}。
	 *
	 * @param typeReference 描述期望响应请求类型的类型引用
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的请求体的 mono
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference);

	/**
	 * 提取请求体为 {@code Flux}。
	 *
	 * @param elementClass {@code Flux} 中元素的类
	 * @param <T> 元素类型
	 * @return 请求体作为 flux
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);
	/**
	 * 提取请求体为 {@code Flux}。
	 *
	 * @param typeReference 描述期望请求体类型的类型引用
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的请求体的 flux
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference);

	/**
	 * 如果存在，获取请求属性的值。
	 *
	 * @param name 属性名
	 * @return 属性值
	 */
	default Optional<Object> attribute(String name) {
		return Optional.ofNullable(attributes().get(name));
	}

	/**
	 * 获取可变的请求属性映射。
	 *
	 * @return 请求属性
	 */
	Map<String, Object> attributes();

	/**
	 * 如果存在，获取给定名称的第一个查询参数。
	 *
	 * @param name 参数名
	 * @return 参数值
	 */
	default Optional<String> queryParam(String name) {
		List<String> queryParamValues = queryParams().get(name);
		if (CollectionUtils.isEmpty(queryParamValues)) {
			return Optional.empty();
		} else {
			String value = queryParamValues.get(0);
			if (value == null) {
				value = "";
			}
			return Optional.of(value);
		}
	}

	/**
	 * 获取此请求的所有查询参数。
	 */
	MultiValueMap<String, String> queryParams();

	/**
	 * 如果存在，获取具有给定名称的路径变量。
	 *
	 * @param name 变量名
	 * @return 变量值
	 * @throws IllegalArgumentException 如果没有具有给定名称的路径变量
	 */
	default String pathVariable(String name) {
		Map<String, String> pathVariables = pathVariables();
		if (pathVariables.containsKey(name)) {
			return pathVariables().get(name);
		} else {
			throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
		}
	}

	/**
	 * 获取此请求的所有路径变量。
	 */
	Map<String, String> pathVariables();

	/**
	 * 获取此请求的 Web 会话。
	 * <p>始终保证返回一个实例，可以匹配客户端请求的会话 ID，或者因为客户端没有指定会话 ID 或基础会话已过期而返回一个新的会话 ID。
	 * <p>使用此方法不会自动创建会话。
	 */
	Mono<WebSession> session();

	/**
	 * 获取请求的已认证用户（如果有）。
	 */
	Mono<? extends Principal> principal();

	/**
	 * 如果 Content-Type 为 {@code "application/x-www-form-urlencoded"}，则从请求体中获取表单数据；否则返回一个空的 map。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完全读取和解析，并且生成的 {@code MultiValueMap} 会被缓存，因此可以安全地多次调用此方法。
	 */
	Mono<MultiValueMap<String, String>> formData();

	/**
	 * 如果 Content-Type 为 {@code "multipart/form-data"}，则获取多部分请求的部分；否则返回一个空的 map。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完全读取和解析，并且生成的 {@code MultiValueMap} 会被缓存，因此可以安全地多次调用此方法。
	 */
	Mono<MultiValueMap<String, Part>> multipartData();

	/**
	 * 获取此请求基于的 Web 交换。
	 * <p>注意：直接操纵交换（而不是使用 {@code ServerRequest} 和 {@code ServerResponse} 提供的方法）可能导致不规则的结果。
	 *
	 * @since 5.1
	 */
	ServerWebExchange exchange();
	/**
	 * 检查所请求的资源是否根据应用程序确定的上次修改时间戳（{@code Instant}）已被修改。
	 * <p>如果未修改，此方法返回具有相应状态代码和标头的响应；否则返回一个空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public Mono&lt;ServerResponse&gt; myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // 应用程序特定的计算
	 *	 return request.checkNotModified(lastModified)
	 *	   .switchIfEmpty(Mono.defer(() -&gt; {
	 *	     // 进一步请求处理，实际构建内容
	 *		 return ServerResponse.ok().body(...);
	 *	   }));
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>您可以使用此 {@code #checkNotModified(Instant)} 方法；或 {@link #checkNotModified(String)}。
	 * 如果要同时强制使用实体标签（{@code ETag}）和最后修改时间戳（{@code Last-Modified}），
	 * 正如 HTTP 规范建议的那样，则应使用 {@link #checkNotModified(Instant, String)}。
	 *
	 * @param lastModified 应用程序为基础资源确定的最后修改时间戳
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回一个空结果
	 * @since 5.2.5
	 */
	default Mono<ServerResponse> checkNotModified(Instant lastModified) {
		Assert.notNull(lastModified, "LastModified must not be null");
		return DefaultServerRequest.checkNotModified(exchange(), lastModified, null);
	}

	/**
	 * 检查所请求的资源是否根据应用程序确定的 {@code ETag}（实体标签）已被修改。
	 * <p>如果未修改，此方法返回具有相应状态代码和标头的响应；否则返回一个空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public Mono&lt;ServerResponse&gt; myHandleMethod(ServerRequest request) {
	 *   String eTag = // 应用程序特定的计算
	 *	 return request.checkNotModified(eTag)
	 *	   .switchIfEmpty(Mono.defer(() -&gt; {
	 *	     // 进一步请求处理，实际构建内容
	 *		 return ServerResponse.ok().body(...);
	 *	   }));
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>您可以使用此 {@link #checkNotModified(Instant)} 方法；或 {@code #checkNotModified(String)}。
	 * 如果要同时强制使用实体标签和最后修改时间戳，正如 HTTP 规范建议的那样，则应使用 {@link #checkNotModified(Instant, String)}。
	 *
	 * @param etag 应用程序为基础资源确定的实体标签。必要时，此参数将用引号（"）填充。
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回一个空结果
	 * @since 5.2.5
	 */
	default Mono<ServerResponse> checkNotModified(String etag) {
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(exchange(), null, etag);
	}

	/**
	 * 检查所请求的资源是否根据应用程序确定的 {@code ETag}（实体标签）和最后修改时间戳已被修改。
	 * <p>如果未修改，此方法返回具有相应状态代码和标头的响应；否则返回一个空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public Mono&lt;ServerResponse&gt; myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // 应用程序特定的计算
	 *   String eTag = // 应用程序特定的计算
	 *	 return request.checkNotModified(lastModified, eTag)
	 *	   .switchIfEmpty(Mono.defer(() -&gt; {
	 *	     // 进一步请求处理，实际构建内容
	 *		 return ServerResponse.ok().body(...);
	 *	   }));
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 *
	 * @param lastModified 应用程序为基础资源确定的最后修改时间戳
	 * @param etag 应用程序为基础资源确定的实体标签。必要时，此参数将用引号（"）填充。
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回一个空结果
	 * @since 5.2.5
	 */
	default Mono<ServerResponse> checkNotModified(Instant lastModified, String etag) {
		Assert.notNull(lastModified, "LastModified must not be null");
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(exchange(), lastModified, etag);
	}

	// 静态生成器方法
	/**
	 * 基于给定的 {@code ServerWebExchange} 和消息读取器创建一个新的 {@code ServerRequest}。
	 * @param exchange 交换对象
	 * @param messageReaders 消息读取器
	 * @return 创建的 {@code ServerRequest}
	 */
	static ServerRequest create(ServerWebExchange exchange, List<HttpMessageReader<?>> messageReaders) {
		return new DefaultServerRequest(exchange, messageReaders);
	}

	/**
	 * 使用给定请求的消息读取器、方法名、URI、标头、Cookie 和属性创建一个构建器。
	 * @param other 要从中复制消息读取器、方法名、URI、标头和属性的请求
	 * @return 创建的构建器
	 * @since 5.1
	 */
	static Builder from(ServerRequest other) {
		return new DefaultServerRequestBuilder(other);
	}


	/**
	 * 表示 HTTP 请求的标头。
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * 获取可接受的媒体类型列表，由 {@code Accept} 标头指定。
		 * <p>如果未指定可接受的媒体类型，则返回空列表。
		 */
		List<MediaType> accept();

		/**
		 * 获取可接受的字符集列表，由 {@code Accept-Charset} 标头指定。
		 */
		List<Charset> acceptCharset();

		/**
		 * 获取可接受的语言列表，由 {@code Accept-Language} 标头指定。
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * 获取以字节为单位的请求体长度，由 {@code Content-Length} 标头指定。
		 */
		OptionalLong contentLength();

		/**
		 * 获取请求体的媒体类型，由 {@code Content-Type} 标头指定。
		 */
		Optional<MediaType> contentType();

		/**
		 * 获取 {@code Host} 标头的值（如果可用）。
		 * <p>如果标头值不包含端口，则返回地址中的 {@linkplain InetSocketAddress#getPort() 端口} 将为 {@code 0}。
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * 获取 {@code Range} 标头的值。
		 * <p>当范围未知时返回空列表。
		 */
		List<HttpRange> range();

		/**
		 * 获取具有给定名称的标头的标头值（如果有）。
		 * <p>如果未找到标头值，则返回空列表。
		 * @param headerName 标头名称
		 */
		List<String> header(String headerName);

		/**
		 * 获取具有给定名称的标头的第一个标头值（如果有）。
		 * <p>如果未找到标头值，则返回 {@code null}。
		 * @param headerName 标头名称
		 * @since 5.2.5
		 */
		@Nullable
		default String firstHeader(String headerName) {
			List<String> list = header(headerName);
			return list.isEmpty() ? null : list.get(0);
		}

		/**
		 * 将标头作为 {@link HttpHeaders} 实例获取。
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * 定义了请求的构建器。
	 * @since 5.1
	 */
	interface Builder {

		/**
		 * 设置请求的方法。
		 * @param method 新方法
		 * @return 此构建器
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 URI。
		 * @param uri 新 URI
		 * @return 此构建器
		 */
		Builder uri(URI uri);

		/**
		 * 在给定名称下添加给定的标头值。
		 * @param headerName 标头名称
		 * @param headerValues 标头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的标头。
		 * <p>提供给消费者的标头是“活动的”，因此消费者可以用于
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有标头值，
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 使用给定名称和值(s)添加一个 cookie。
		 * @param name cookie 名称
		 * @param values cookie 值
		 * @return 此构建器
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的 cookie。
		 * <p>提供给消费者的映射是“活动的”，因此消费者可以用于
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie，
		 * {@linkplain MultiValueMap#remove(Object) 删除} cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 cookie 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

		/**
		 * 设置请求的正文。
		 * <p>调用此方法将
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器的现有正文。
		 * @param body 新正文
		 * @return 此构建器
		 */
		Builder body(Flux<DataBuffer> body);

		/**
		 * 将请求的正文设置为给定字符串的 UTF-8 编码字节。
		 * <p>调用此方法将
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器的现有正文。
		 * @param body 新正文
		 * @return 此构建器
		 */
		Builder body(String body);

		/**
		 * 使用给定名称和值添加一个属性。
		 * @param name 属性名称
		 * @param value 属性值
		 * @return 此构建器
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作此请求的属性。
		 * <p>提供给消费者的映射是“活动的”，因此消费者可以用于
		 * {@linkplain Map#put(Object, Object) 覆盖}现有属性，
		 * {@linkplain Map#remove(Object) 删除}属性，或使用任何其他
		 * {@link Map} 方法。
		 * @param attributesConsumer 消费属性映射的函数
		 * @return 此构建器
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 构建请求。
		 * @return 构建的请求
		 */
		ServerRequest build();
	}

}
