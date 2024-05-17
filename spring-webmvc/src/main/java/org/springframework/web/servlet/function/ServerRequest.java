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

package org.springframework.web.servlet.function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * 表示由 {@code HandlerFunction} 处理的服务器端 HTTP 请求。
 * 通过 {@link Headers} 和 {@link #body(Class)} 分别访问头信息和正文。
 * <p>
 * 代表一个服务器端 HTTP 请求。由 {@link Headers} 和 {@link #body(Class)} 分别提供对头信息和正文的访问。
 * </p>
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerRequest {

	/**
	 * 获取 HTTP 方法。
	 *
	 * @return HTTP 方法作为 HttpMethod 枚举值，如果无法解析则返回 {@code null}（例如在非标准 HTTP 方法的情况下）
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * 获取 HTTP 方法名称。
	 *
	 * @return HTTP 方法名称作为字符串
	 */
	String methodName();

	/**
	 * 获取请求 URI。
	 */
	URI uri();

	/**
	 * 从与此 {@code ServerRequest} 关联的 URI 获取一个 {@code UriBuilderComponents}。
	 *
	 * @return URI 构建器
	 */
	UriBuilder uriBuilder();

	/**
	 * 获取请求路径。
	 */
	default String path() {
		return requestPath().pathWithinApplication().value();
	}

	/**
	 * 获取请求路径作为 {@code PathContainer}。
	 *
	 * @deprecated as of 5.3, in favor of {@link #requestPath()}
	 */
	@Deprecated
	default PathContainer pathContainer() {
		return requestPath();
	}

	/**
	 * 获取请求路径作为 {@code RequestPath}。
	 *
	 * @since 5.3
	 */
	default RequestPath requestPath() {
		return ServletRequestPathUtils.getParsedRequestPath(servletRequest());
	}

	/**
	 * 获取此请求的头信息。
	 */
	Headers headers();

	/**
	 * 获取此请求的 Cookie。
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * 获取此请求连接的远程地址（如果可用）。
	 */
	Optional<InetSocketAddress> remoteAddress();

	/**
	 * 获取用于转换此请求正文的读取器。
	 */
	List<HttpMessageConverter<?>> messageConverters();

	/**
	 * 将正文提取为给定类型的对象。
	 *
	 * @param bodyType 返回值的类型
	 * @param <T>      正文的类型
	 * @return 正文
	 * @throws ServletException 如果解析失败
	 * @throws IOException      如果发生 I/O 错误
	 */
	<T> T body(Class<T> bodyType) throws ServletException, IOException;

	/**
	 * 将正文提取为给定类型的对象。
	 *
	 * @param bodyType 返回值的类型
	 * @param <T>      正文的类型
	 * @return 正文
	 * @throws ServletException 如果解析失败
	 * @throws IOException      如果发生 I/O 错误
	 */
	<T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException;

	/**
	 * 获取请求属性值（如果存在）。
	 *
	 * @param name 属性名称
	 * @return 属性值
	 */
	default Optional<Object> attribute(String name) {
		Map<String, Object> attributes = attributes();
		if (attributes.containsKey(name)) {
			return Optional.of(attributes.get(name));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * 获取请求属性的可变映射。
	 *
	 * @return 请求属性
	 */
	Map<String, Object> attributes();

	/**
	 * 获取具有给定名称的第一个参数（如果存在）。Servlet 参数包含在查询字符串或发布的表单数据中。
	 *
	 * @param name 参数名称
	 * @return 参数值
	 * @see HttpServletRequest#getParameter(String)
	 */
	default Optional<String> param(String name) {
		List<String> paramValues = params().get(name);
		if (CollectionUtils.isEmpty(paramValues)) {
			return Optional.empty();
		} else {
			String value = paramValues.get(0);
			if (value == null) {
				value = "";
			}
			return Optional.of(value);
		}
	}

	/**
	 * 获取此请求的所有参数。Servlet 参数包含在查询字符串或发布的表单数据中。
	 *
	 * @see HttpServletRequest#getParameterMap()
	 */
	MultiValueMap<String, String> params();

	/**
	 * 获取多部分请求的部件，前提是 Content-Type 为 {@code "multipart/form-data"}，否则会抛出异常。
	 *
	 * @return 多部分数据，从名称到部件的映射
	 * @throws IOException      如果在检索期间发生 I/O 错误
	 * @throws ServletException 如果此请求不是类型 {@code "multipart/form-data"}
	 * @see HttpServletRequest#getParts()
	 * @since 5.3
	 */
	MultiValueMap<String, Part> multipartData() throws IOException, ServletException;

	/**
	 * 获取具有给定名称的路径变量（如果存在）。
	 *
	 * @param name 变量名称
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
	 * 获取此请求的 Web 会话。始终保证返回一个实例，该实例要么与客户端请求的会话 ID 匹配，
	 * 要么具有一个新的会话 ID，这是因为客户端没有指定一个或底层会话已过期。使用此方法不会自动创建会话。
	 */
	HttpSession session();

	/**
	 * 获取请求的认证用户（如果有）。
	 */
	Optional<Principal> principal();

	/**
	 * 获取此请求基于的 servlet 请求。
	 */
	HttpServletRequest servletRequest();

	/**
	 * 检查在应用程序确定的给定最后修改时间戳下，请求的资源是否已被修改。
	 * 如果未修改，此方法返回具有相应状态码和头部的响应，否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // 应用程序特定的计算
	 * 	 return request.checkNotModified(lastModified)
	 * 	   .orElseGet(() -&gt; {
	 * 	     // 进一步的请求处理，实际生成内容
	 * 		 return ServerResponse.ok().body(...);
	 *       });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，但也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>你可以使用此 {@code #checkNotModified(Instant)} 方法；或 {@link #checkNotModified(String)}。
	 * 如果你想强制执行强实体标签和最后修改值，如 HTTP 规范推荐的那样，
	 * 那么你应该使用 {@link #checkNotModified(Instant, String)}。
	 *
	 * @param lastModified 应用程序为底层资源确定的最后修改时间戳
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(Instant lastModified) {
		Assert.notNull(lastModified, "LastModified must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), lastModified, null);
	}

	/**
	 * 检查在应用程序确定的给定 {@code ETag}（实体标签）下，请求的资源是否已被修改。
	 * 如果未修改，此方法返回具有相应状态码和头部的响应，否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   String eTag = // 应用程序特定的计算
	 * 	 return request.checkNotModified(eTag)
	 * 	   .orElseGet(() -&gt; {
	 * 	     // 进一步的请求处理，实际生成内容
	 * 		 return ServerResponse.ok().body(...);
	 *       });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，但也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>你可以使用此 {@link #checkNotModified(Instant)} 方法；或 {@code #checkNotModified(String)}。
	 * 如果你想强制执行强实体标签和最后修改值，如 HTTP 规范推荐的那样，
	 * 那么你应该使用 {@link #checkNotModified(Instant, String)}。
	 *
	 * @param etag 应用程序为底层资源确定的实体标签。此参数将根据需要用引号（"）填充。
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(String etag) {
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), null, etag);
	}

	/**
	 * 检查在应用程序确定的给定 {@code ETag}（实体标签）和最后修改时间戳下，请求的资源是否已被修改。
	 * 如果未修改，此方法返回具有相应状态码和头部的响应，否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // 应用程序特定的计算
	 *   String eTag = // 应用程序特定的计算
	 * 	 return request.checkNotModified(lastModified, eTag)
	 * 	   .orElseGet(() -&gt; {
	 * 	     // 进一步的请求处理，实际生成内容
	 * 		 return ServerResponse.ok().body(...);
	 *       });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，但也适用于条件 POST/PUT/DELETE 请求。
	 *
	 * @param lastModified 应用程序为底层资源确定的最后修改时间戳
	 * @param etag         应用程序为底层资源确定的实体标签。此参数将根据需要用引号（"）填充。
	 * @return 如果请求符合未修改的条件，则返回相应的响应；否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
		Assert.notNull(lastModified, "LastModified must not be null");
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), lastModified, etag);
	}

	// 静态方法

	/**
	 * 基于给定的 {@code HttpServletRequest} 和消息转换器创建一个新的 {@code ServerRequest}。
	 *
	 * @param servletRequest 请求
	 * @param messageReaders 消息读取器
	 * @return 创建的 {@code ServerRequest}
	 */
	static ServerRequest create(HttpServletRequest servletRequest, List<HttpMessageConverter<?>> messageReaders) {
		return new DefaultServerRequest(servletRequest, messageReaders);
	}

	/**
	 * 创建一个具有给定请求的状态、头部和 cookies 的构建器。
	 *
	 * @param other 要复制状态、头部和 cookies 的请求
	 * @return 创建的构建器
	 */
	static Builder from(ServerRequest other) {
		return new DefaultServerRequestBuilder(other);
	}


	/**
	 * 表示 HTTP 请求的头部。
	 *
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * 获取由 {@code Accept} 头部指定的可接受媒体类型列表。
		 * <p>如果未指定可接受的媒体类型，则返回空列表。
		 */
		List<MediaType> accept();

		/**
		 * 获取由 {@code Accept-Charset} 头部指定的可接受字符集列表。
		 */
		List<Charset> acceptCharset();

		/**
		 * 获取由 {@code Accept-Language} 头部指定的可接受语言列表。
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * 获取由 {@code Content-Length} 头部指定的主体长度（以字节为单位）。
		 */
		OptionalLong contentLength();

		/**
		 * 获取由 {@code Content-Type} 头部指定的主体媒体类型。
		 */
		Optional<MediaType> contentType();

		/**
		 * 获取 {@code Host} 头部的值（如果有）。
		 * <p>如果头部值不包含端口，则返回地址中的
		 * {@linkplain InetSocketAddress#getPort() 端口}将为 {@code 0}。
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * 获取 {@code Range} 头部的值。
		 * <p>当范围未知时返回空列表。
		 */
		List<HttpRange> range();

		/**
		 * 获取给定名称头部的值（如果有）。
		 * <p>如果未找到头部值，则返回空列表。
		 *
		 * @param headerName 头部名称
		 */
		List<String> header(String headerName);

		/**
		 * 获取给定名称头部的第一个值（如果有）。
		 * <p>如果未找到头部值，则返回 {@code null}。
		 *
		 * @param headerName 头部名称
		 * @since 5.2.5
		 */
		@Nullable
		default String firstHeader(String headerName) {
			List<String> list = header(headerName);
			return list.isEmpty() ? null : list.get(0);
		}

		/**
		 * 将头部作为 {@link HttpHeaders} 实例获取。
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * 定义一个请求的构建器。
	 */
	interface Builder {

		/**
		 * 设置请求的方法。
		 *
		 * @param method 新的方法
		 * @return 此构建器
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 URI。
		 *
		 * @param uri 新的 URI
		 * @return 此构建器
		 */
		Builder uri(URI uri);

		/**
		 * 添加给定名称下的给定头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的头部。
		 * <p>提供给消费者的头部是“实时”的，因此消费者可以用来
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有的头部值，
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 *
		 * @param headersConsumer 一个使用 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 添加具有给定名称和值的 cookie。
		 *
		 * @param name   cookie 名称
		 * @param values cookie 值
		 * @return 此构建器
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的 cookies。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以用来
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有的 cookies，
		 * {@linkplain MultiValueMap#remove(Object) 移除} cookies，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 *
		 * @param cookiesConsumer 一个使用 cookies 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置请求的主体。
		 * <p>调用此方法将
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器现有的主体。
		 *
		 * @param body 新的主体
		 * @return 此构建器
		 */
		Builder body(byte[] body);

		/**
		 * 将请求的主体设置为给定字符串的 UTF-8 编码字节。
		 * <p>调用此方法将
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器现有的主体。
		 *
		 * @param body 新的主体
		 * @return 此构建器
		 */
		Builder body(String body);

		/**
		 * 添加具有给定名称和值的属性。
		 *
		 * @param name  属性名称
		 * @param value 属性值
		 * @return 此构建器
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作此请求的属性。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以用来
		 * {@linkplain Map#put(Object, Object) 覆盖}现有的属性，
		 * {@linkplain Map#remove(Object) 移除}属性，或使用任何其他
		 * {@link Map} 方法。
		 *
		 * @param attributesConsumer 一个使用属性映射的函数
		 * @return 此构建器
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 添加具有给定名称和值的参数。
		 *
		 * @param name   参数名称
		 * @param values 参数值
		 * @return 此构建器
		 */
		Builder param(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的参数。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以用来
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有的参数，
		 * {@linkplain MultiValueMap#remove(Object) 移除}参数，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 *
		 * @param paramsConsumer 一个使用参数映射的函数
		 * @return 此构建器
		 */
		Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer);

		/**
		 * 设置请求的远程地址。
		 *
		 * @param remoteAddress 远程地址
		 * @return 此构建器
		 */
		Builder remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 构建请求。
		 *
		 * @return 构建的请求
		 */
		ServerRequest build();
	}

}
