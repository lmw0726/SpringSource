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
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * {@link HttpEntity}的扩展，添加了一个{@link HttpStatus}状态码。
 * 在{@code RestTemplate}和{@code @Controller}方法中使用。
 *
 * <p>在{@code RestTemplate}中，此类由
 * {@link org.springframework.web.client.RestTemplate#getForEntity getForEntity()} 和
 * {@link org.springframework.web.client.RestTemplate#exchange exchange()} 方法返回：
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 *
 * <p>这也可以在Spring MVC中作为{@code @Controller}方法的返回值使用：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 * <p>
 * 或者，通过静态方法访问的构建器来使用：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * @param <T> 主体类型
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @see #getStatusCode()
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, Object...)
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, java.util.Map)
 * @see org.springframework.web.client.RestOperations#getForEntity(URI, Class)
 * @see RequestEntity
 * @since 3.0.2
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	/**
	 * 状态码。
	 */
	private final Object status;


	/**
	 * 创建仅具有状态码的{@code ResponseEntity}。
	 *
	 * @param status 状态码
	 */
	public ResponseEntity(HttpStatus status) {
		this(null, null, status);
	}

	/**
	 * 创建带有主体和状态码的{@code ResponseEntity}。
	 *
	 * @param body   实体主体
	 * @param status 状态码
	 */
	public ResponseEntity(@Nullable T body, HttpStatus status) {
		this(body, null, status);
	}

	/**
	 * 创建带有头部和状态码的{@code ResponseEntity}。
	 *
	 * @param headers 实体头部
	 * @param status  状态码
	 */
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
		this(null, headers, status);
	}

	/**
	 * 创建带有主体、头部和状态码的{@code ResponseEntity}。
	 *
	 * @param body    实体主体
	 * @param headers 实体头部
	 * @param status  状态码
	 */
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, HttpStatus status) {
		this(body, headers, (Object) status);
	}

	/**
	 * 创建带有主体、头部和原始状态码的{@code ResponseEntity}。
	 *
	 * @param body      实体主体
	 * @param headers   实体头部
	 * @param rawStatus 状态码值
	 * @since 5.3.2
	 */
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, int rawStatus) {
		this(body, headers, (Object) rawStatus);
	}

	/**
	 * 私有构造函数。
	 */
	private ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, Object status) {
		super(body, headers);
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status;
	}


	/**
	 * 返回响应的 HTTP 状态码。
	 *
	 * @return HTTP 状态作为 HttpStatus 枚举条目
	 */
	public HttpStatus getStatusCode() {
		// 如果状态码是HttpStatus类型，则直接返回
		if (this.status instanceof HttpStatus) {
			return (HttpStatus) this.status;
		} else {
			// 否则根据状态码的整数值获取对应的HttpStatus对象并返回
			return HttpStatus.valueOf((Integer) this.status);
		}
	}

	/**
	 * 返回响应的 HTTP 状态码。
	 *
	 * @return HTTP 状态作为 int 值
	 * @since 4.3
	 */
	public int getStatusCodeValue() {
		// 如果状态是 HttpStatus 类型的实例
		if (this.status instanceof HttpStatus) {
			// 返回 HttpStatus 对象的值
			return ((HttpStatus) this.status).value();
		} else {
			// 否则返回状态码的整数值
			return (Integer) this.status;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		// 创建一个字符串构建器对象
		StringBuilder builder = new StringBuilder("<");
		// 将状态码添加到字符串构建器中
		builder.append(this.status);
		// 如果状态码是HttpStatus类型
		if (this.status instanceof HttpStatus) {
			// 添加状态码的原因短语
			builder.append(' ');
			builder.append(((HttpStatus) this.status).getReasonPhrase());
		}
		// 添加逗号
		builder.append(',');
		// 获取消息体
		T body = getBody();
		// 获取消息头部
		HttpHeaders headers = getHeaders();
		// 如果消息体不为null，则将其添加到字符串构建器中
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}
		// 将消息头部添加到字符串构建器中
		builder.append(headers);
		// 添加结尾字符
		builder.append('>');
		// 返回构建的字符串
		return builder.toString();
	}


	// 静态生成器方法

	/**
	 * 使用给定状态创建一个构建器。
	 *
	 * @param status 响应状态
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		return new DefaultBuilder(status);
	}

	/**
	 * 使用给定状态创建一个构建器。
	 *
	 * @param status 响应状态
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder status(int status) {
		return new DefaultBuilder(status);
	}

	/**
	 * 创建状态设置为{@linkplain HttpStatus#OK OK}的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 创建一个带有给定主体和状态设置为{@linkplain HttpStatus#OK OK}的{@code ResponseEntity}的快捷方式。
	 *
	 * @param body 响应实体的主体（可能为空）
	 * @return 创建的{@code ResponseEntity}
	 * @since 4.1
	 */
	public static <T> ResponseEntity<T> ok(@Nullable T body) {
		return ok().body(body);
	}

	/**
	 * 创建一个带有给定主体的{@code ResponseEntity}的快捷方式，状态设置为{@linkplain HttpStatus#OK OK}，或者在{@linkplain Optional#empty()}参数的情况下，创建一个带有{@linkplain HttpStatus#NOT_FOUND NOT FOUND}状态和空主体的{@code ResponseEntity}。
	 *
	 * @return 创建的{@code ResponseEntity}
	 * @since 5.1
	 */
	public static <T> ResponseEntity<T> of(Optional<T> body) {
		Assert.notNull(body, "Body must not be null");
		return body.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#CREATED CREATED}状态和位置头设置为给定URI的构建器。
	 *
	 * @param location 位置URI
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder created(URI location) {
		return status(HttpStatus.CREATED).location(location);
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#ACCEPTED ACCEPTED}状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#NO_CONTENT NO_CONTENT}状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST}状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#NOT_FOUND NOT_FOUND}状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 创建一个带有{@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY}状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 4.1.3
	 */
	public static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * 创建一个具有 {@linkplain HttpStatus#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR} 状态的构建器。
	 *
	 * @return 创建的构建器
	 * @since 5.3.8
	 */
	public static BodyBuilder internalServerError() {
		return status(HttpStatus.INTERNAL_SERVER_ERROR);
	}


	/**
	 * 定义一个向响应实体添加头部的构建器。
	 *
	 * @param <B> 构建器子类
	 * @since 4.1
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的单个头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 将给定的头部复制到实体的头部映射中。
		 *
		 * @param headers 要复制的现有 HttpHeaders
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 * @since 4.1.2
		 */
		B headers(@Nullable HttpHeaders headers);

		/**
		 * 使用给定的消费者操作此实体的头部。提供给消费者的头部是“活动”的，
		 * 因此消费者可以用于 {@linkplain HttpHeaders#set(String, String) 覆盖}现有头部值、
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
		 *
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 * @since 5.2
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法} 集合，由 {@code Allow} 头部指定。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置主体的实体标记，由 {@code ETag} 头部指定。
		 *
		 * @param etag 新的实体标记
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String etag);

		/**
		 * 设置资源上次更改的时间，由 {@code Last-Modified} 头部指定。
		 *
		 * @param lastModified 上次修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(ZonedDateTime)
		 * @since 5.1.4
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源上次更改的时间，由 {@code Last-Modified} 头部指定。
		 *
		 * @param lastModified 上次修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(Instant)
		 * @since 5.1.4
		 */
		B lastModified(Instant lastModified);

		/**
		 * 设置资源上次更改的时间，由 {@code Last-Modified} 头部指定。
		 * <p>日期应该指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
		 *
		 * @param lastModified 上次修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(long lastModified);

		/**
		 * 设置资源的位置，由 {@code Location} 头部指定。
		 *
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1 {@code Cache-Control} 头部指定。
		 * <p>可以像 {@code CacheControl.maxAge(3600).cachePublic().noTransform()} 这样构建 {@code CacheControl} 实例。
		 *
		 * @param cacheControl 用于缓存相关的 HTTP 响应头部的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 第 5.2 节</a>
		 * @since 4.2
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头部名称（例如 "Accept-Language"），
		 * 以将其添加到 "Vary" 响应头部中，以通知客户端响应是根据给定请求头部的值进行内容协商和变化的。
		 * 仅在响应 "Vary" 头部中不存在的情况下才会添加配置的请求头部名称。
		 *
		 * @param requestHeaders 请求头部名称
		 * @since 4.3
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 使用无主体构建响应实体。
		 *
		 * @return 响应实体
		 * @see BodyBuilder#body(Object)
		 */
		<T> ResponseEntity<T> build();
	}


	/**
	 * 定义一个构建器，用于向响应实体添加主体。
	 *
	 * @since 4.1
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置主体的字节长度，由{@code Content-Length}头指定。
		 *
		 * @param contentLength 主体长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置主体的{@linkplain MediaType 媒体类型}，由{@code Content-Type}头指定。
		 *
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置响应实体的主体并返回它。
		 *
		 * @param <T>  主体类型
		 * @param body 响应实体的主体
		 * @return 构建的响应实体
		 */
		<T> ResponseEntity<T> body(@Nullable T body);
	}


	private static class DefaultBuilder implements BodyBuilder {
		/**
		 * 状态码
		 */
		private final Object statusCode;
		/**
		 * 响应头
		 */
		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBuilder(Object statusCode) {
			this.statusCode = statusCode;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			// 遍历头部值列表中的每个头部值
			for (String headerValue : headerValues) {
				// 将头部名和头部值添加到请求头中
				this.headers.add(headerName, headerValue);
			}

			// 返回当前构建器实例
			return this;
		}

		@Override
		public BodyBuilder headers(@Nullable HttpHeaders headers) {
			// 如果传入的头部不为null，则将其添加到当前头部中
			if (headers != null) {
				this.headers.putAll(headers);
			}
			// 返回当前对象
			return this;
		}

		@Override
		public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		@Override
		public BodyBuilder allow(HttpMethod... allowedMethods) {
			this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
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
		public BodyBuilder eTag(String etag) {
			// 如果ETag不以双引号或"W/"开头，则添加双引号
			if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
				etag = "\"" + etag;
			}
			// 如果ETag不以双引号结尾，则添加双引号
			if (!etag.endsWith("\"")) {
				etag = etag + "\"";
			}
			// 设置ETag头部
			this.headers.setETag(etag);
			// 返回当前对象
			return this;
		}

		@Override
		public BodyBuilder lastModified(ZonedDateTime date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(Instant date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(long date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder location(URI location) {
			this.headers.setLocation(location);
			return this;
		}

		@Override
		public BodyBuilder cacheControl(CacheControl cacheControl) {
			this.headers.setCacheControl(cacheControl);
			return this;
		}

		@Override
		public BodyBuilder varyBy(String... requestHeaders) {
			this.headers.setVary(Arrays.asList(requestHeaders));
			return this;
		}

		@Override
		public <T> ResponseEntity<T> build() {
			return body(null);
		}

		@Override
		public <T> ResponseEntity<T> body(@Nullable T body) {
			return new ResponseEntity<>(body, this.headers, this.statusCode);
		}
	}

}
