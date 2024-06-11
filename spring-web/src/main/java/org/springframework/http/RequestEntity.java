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

package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 扩展 {@link HttpEntity} ，同时公开 HTTP 方法和目标 URL。
 * 用于 {@code RestTemplate} 中准备请求和 {@code @Controller} 方法中表示请求输入。
 *
 * <p>在 {@code RestTemplate} 中的使用示例：
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity
 *     .post(&quot;https://example.com/{foo}&quot;, &quot;bar&quot;)
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 *
 * <p>在 {@code @Controller} 中的使用示例：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public void handle(RequestEntity&lt;String&gt; request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * </pre>
 *
 * @param <T> 身体类型
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Parviz Rozikov
 * @see #getMethod()
 * @see #getUrl()
 * @see org.springframework.web.client.RestOperations#exchange(RequestEntity, Class)
 * @see ResponseEntity
 * @since 4.1
 */
public class RequestEntity<T> extends HttpEntity<T> {
	/**
	 * 请求方法
	 */
	@Nullable
	private final HttpMethod method;

	/**
	 * URI
	 */
	@Nullable
	private final URI url;

	/**
	 * 类型
	 */
	@Nullable
	private final Type type;

	/**
	 * 带有方法和 URL 的构造函数，但没有 body 和 headers。
	 *
	 * @param method 方法
	 * @param url    URL
	 */
	public RequestEntity(HttpMethod method, URI url) {
		this(null, null, method, url, null);
	}

	/**
	 * 带有方法，URL 和 body 的构造函数，但没有 headers。
	 *
	 * @param body   body
	 * @param method 方法
	 * @param url    URL
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url) {
		this(body, null, method, url, null);
	}

	/**
	 * 带有方法，URL，body 和类型的构造函数，但没有 headers。
	 *
	 * @param body   body
	 * @param method 方法
	 * @param url    URL
	 * @param type   用于泛型类型解析的类型
	 * @since 4.3
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url, Type type) {
		this(body, null, method, url, type);
	}

	/**
	 * 带有方法，URL 和 headers 的构造函数，但没有 body。
	 *
	 * @param headers headers
	 * @param method  方法
	 * @param url     URL
	 */
	public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	/**
	 * 带有方法，URL，headers 和 body 的构造函数。
	 *
	 * @param body    body
	 * @param headers headers
	 * @param method  方法
	 * @param url     URL
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
						 @Nullable HttpMethod method, URI url) {

		this(body, headers, method, url, null);
	}

	/**
	 * 带有方法，URL，headers，body 和类型的构造函数。
	 *
	 * @param body    body
	 * @param headers headers
	 * @param method  方法
	 * @param url     URL
	 * @param type    用于泛型类型解析的类型
	 * @since 4.3
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
						 @Nullable HttpMethod method, @Nullable URI url, @Nullable Type type) {

		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}


	/**
	 * 返回请求的 HTTP 方法。
	 *
	 * @return HTTP 方法作为 {@code HttpMethod} 枚举值
	 */
	@Nullable
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * 返回请求的 URL。
	 */
	public URI getUrl() {
		if (this.url == null) {
			// 如果UR为空，抛出异常
			throw new UnsupportedOperationException();
		}
		return this.url;
	}


	/**
	 * 返回请求体的类型。
	 *
	 * @return 请求体的类型，如果未知则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public Type getType() {
		// 如果类型为null
		if (this.type == null) {
			// 获取消息体
			T body = getBody();
			// 如果消息体不为null，则返回其类类型
			if (body != null) {
				return body.getClass();
			}
		}
		// 否则返回已设置的类型
		return this.type;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		RequestEntity<?> otherEntity = (RequestEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.method, otherEntity.method) &&
				ObjectUtils.nullSafeEquals(this.url, otherEntity.url));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.method);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.url);
		return hashCode;
	}

	@Override
	public String toString() {
		return format(getMethod(), getUrl().toString(), getBody(), getHeaders());
	}

	static <T> String format(@Nullable HttpMethod httpMethod, String url, @Nullable T body, HttpHeaders headers) {
		// 创建一个 字符串构建器 对象，并初始化为 "<"
		StringBuilder builder = new StringBuilder("<");

		// 添加 HTTP 方法和 URL 到 字符串构建器 中
		builder.append(httpMethod);
		builder.append(' ');
		builder.append(url);
		builder.append(',');

		// 如果请求体不为 null，则添加请求体到 字符串构建器 中
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}

		// 添加请求头到 字符串构建器 中
		builder.append(headers);

		// 添加 ">" 到 字符串构建器 中，并将 字符串构建器 转换为字符串返回
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * 创建一个具有给定方法和 URL 的构建器。
	 *
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url    URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * 创建一个具有给定 HTTP 方法、URI 模板和变量的构建器。
	 *
	 * @param method       HTTP 方法（GET、POST 等）
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, Object... uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 HTTP 方法、URI 模板和变量的构建器。
	 *
	 * @param method       HTTP 方法（GET、POST 等）
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, Map<String, ?> uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}


	/**
	 * 创建一个具有给定 URL 的 HTTP GET 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> get(URI url) {
		return method(HttpMethod.GET, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP GET 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> get(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.GET, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP HEAD 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> head(URI url) {
		return method(HttpMethod.HEAD, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP HEAD 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> head(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.HEAD, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP POST 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder post(URI url) {
		return method(HttpMethod.POST, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP POST 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder post(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.POST, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP PUT 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder put(URI url) {
		return method(HttpMethod.PUT, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP PUT 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder put(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.PUT, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP PATCH 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder patch(URI url) {
		return method(HttpMethod.PATCH, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP PATCH 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder patch(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.PATCH, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP DELETE 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> delete(URI url) {
		return method(HttpMethod.DELETE, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP DELETE 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> delete(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.DELETE, uriTemplate, uriVariables);
	}

	/**
	 * 创建一个具有给定 URL 的 HTTP OPTIONS 构建器。
	 *
	 * @param url URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> options(URI url) {
		return method(HttpMethod.OPTIONS, url);
	}

	/**
	 * 创建一个具有给定字符串基础 URI 模板的 HTTP OPTIONS 构建器。
	 *
	 * @param uriTemplate  要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> options(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.OPTIONS, uriTemplate, uriVariables);
	}


	/**
	 * 定义一个向请求实体添加头部信息的构建器。
	 *
	 * @param <B> 构建器子类
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定的名称下添加给定的单个头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 当前构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 将给定的头部复制到实体的头部映射中。
		 *
		 * @param headers 要复制的现有 HttpHeaders
		 * @return 当前构建器
		 * @see HttpHeaders#add(String, String)
		 * @since 5.2
		 */
		B headers(@Nullable HttpHeaders headers);

		/**
		 * 使用给定的消费者操作此实体的头部。提供给消费者的头部是"实时"的，
		 * 因此可以使用消费者来{@linkplain HttpHeaders#set(String, String) 覆盖}现有的头部值，
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用其他任何{@link HttpHeaders}方法。
		 *
		 * @param headersConsumer 一个用于操作 {@code HttpHeaders} 的函数
		 * @return 当前构建器
		 * @since 5.2
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置可接受的{@linkplain MediaType 媒体类型}列表，具体由 {@code Accept} 头部指定。
		 *
		 * @param acceptableMediaTypes 可接受的媒体类型
		 * @return 当前构建器
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的{@linkplain Charset 字符集}列表，具体由 {@code Accept-Charset} 头部指定。
		 *
		 * @param acceptableCharsets 可接受的字符集
		 * @return 当前构建器
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * 设置 {@code If-Modified-Since} 头部的值。
		 *
		 * @param ifModifiedSince 头部的新值
		 * @return 当前构建器
		 * @since 5.1.4
		 */
		B ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * 设置 {@code If-Modified-Since} 头部的值。
		 *
		 * @param ifModifiedSince 头部的新值
		 * @return 当前构建器
		 * @since 5.1.4
		 */
		B ifModifiedSince(Instant ifModifiedSince);

		/**
		 * 设置 {@code If-Modified-Since} 头部的值。
		 * <p>日期应指定为自1970年1月1日 GMT 以来的毫秒数。
		 *
		 * @param ifModifiedSince 头部的新值
		 * @return 当前构建器
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头部的值。
		 *
		 * @param ifNoneMatches 头部的新值
		 * @return 当前构建器
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * 构建没有主体的请求实体。
		 *
		 * @return 请求实体
		 * @see BodyBuilder#body(Object)
		 */
		RequestEntity<Void> build();
	}


	/**
	 * 定义一个向响应实体添加主体的构建器。
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置主体的字节长度，如 {@code Content-Length} 头部所指定。
		 *
		 * @param contentLength 主体长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置主体的 {@linkplain MediaType 媒体类型}，如 {@code Content-Type} 头部所指定。
		 *
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置请求实体的主体并构建 RequestEntity。
		 *
		 * @param <T>  主体的类型
		 * @param body 请求实体的主体
		 * @return 构建的请求实体
		 */
		<T> RequestEntity<T> body(T body);

		/**
		 * 设置请求实体的主体和类型并构建 RequestEntity。
		 *
		 * @param <T>  主体的类型
		 * @param body 请求实体的主体
		 * @param type 主体的类型，用于泛型类型解析
		 * @return 构建的请求实体
		 * @since 4.3
		 */
		<T> RequestEntity<T> body(T body, Type type);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		/**
		 * 请求的HTTP方法。
		 */
		private final HttpMethod method;

		/**
		 * 请求的HTTP头部信息。
		 */
		private final HttpHeaders headers = new HttpHeaders();

		/**
		 * 请求的URI。
		 */
		@Nullable
		private final URI uri;

		/**
		 * 请求的URI模板。
		 */
		@Nullable
		private final String uriTemplate;

		/**
		 * 用于展开URI模板的变量数组。
		 */
		@Nullable
		private final Object[] uriVarsArray;

		/**
		 * 用于展开URI模板的变量映射。
		 */
		@Nullable
		private final Map<String, ?> uriVarsMap;


		DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.uri = url;
			this.uriTemplate = null;
			this.uriVarsArray = null;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, Object... uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVars;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, Map<String, ?> uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = null;
			this.uriVarsMap = uriVars;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			// 遍历请求头值
			for (String headerValue : headerValues) {
				// 逐个添加请求头和请求头值
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(@Nullable HttpHeaders headers) {
			if (headers != null) {
				// 如果请求头不为空，则将所有请求头添加当前请求头中
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(Instant ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestEntity<Void> build() {
			return buildInternal(null, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return buildInternal(body, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return buildInternal(body, type);
		}

		private <T> RequestEntity<T> buildInternal(@Nullable T body, @Nullable Type type) {
			// 如果URI不为空
			if (this.uri != null) {
				// 返回一个包含body、headers、method和uri的新RequestEntity
				return new RequestEntity<>(body, this.headers, this.method, this.uri, type);
			} else if (this.uriTemplate != null) {
				// 如果URI模板不为空，返回一个新UriTemplateRequestEntity
				return new UriTemplateRequestEntity<>(body, this.headers, this.method, type,
						this.uriTemplate, this.uriVarsArray, this.uriVarsMap);
			} else {
				// 如果URI和URI模板都为空，抛出IllegalStateException
				throw new IllegalStateException("Neither URI nor URI template");
			}
		}
	}


	/**
	 * 用 URI 模板和变量初始化的 RequestEntity，而不是用 {@link URI}。
	 *
	 * @param <T> 主体类型
	 * @since 5.3
	 */
	public static class UriTemplateRequestEntity<T> extends RequestEntity<T> {
		/**
		 * URI模板
		 */
		private final String uriTemplate;

		/**
		 * URI变量数组
		 */
		@Nullable
		private final Object[] uriVarsArray;

		/**
		 * URI变量映射
		 */
		@Nullable
		private final Map<String, ?> uriVarsMap;

		UriTemplateRequestEntity(
				@Nullable T body, @Nullable MultiValueMap<String, String> headers,
				@Nullable HttpMethod method, @Nullable Type type, String uriTemplate,
				@Nullable Object[] uriVarsArray, @Nullable Map<String, ?> uriVarsMap) {

			super(body, headers, method, null, type);
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVarsArray;
			this.uriVarsMap = uriVarsMap;
		}

		public String getUriTemplate() {
			return this.uriTemplate;
		}

		@Nullable
		public Object[] getVars() {
			return this.uriVarsArray;
		}

		@Nullable
		public Map<String, ?> getVarsMap() {
			return this.uriVarsMap;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			UriTemplateRequestEntity<?> otherEntity = (UriTemplateRequestEntity<?>) other;
			return (ObjectUtils.nullSafeEquals(this.uriTemplate, otherEntity.uriTemplate) &&
					ObjectUtils.nullSafeEquals(this.uriVarsArray, otherEntity.uriVarsArray) &&
					ObjectUtils.nullSafeEquals(this.uriVarsMap, otherEntity.uriVarsMap));
		}

		@Override
		public int hashCode() {
			return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.uriTemplate));
		}

		@Override
		public String toString() {
			return format(getMethod(), getUriTemplate(), getBody(), getHeaders());
		}
	}

}
