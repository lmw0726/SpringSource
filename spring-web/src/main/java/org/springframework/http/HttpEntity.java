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

/**
 * 表示 HTTP 请求或响应实体，包括头部和主体。
 *
 * <p>通常与 {@link org.springframework.web.client.RestTemplate} 结合使用，如下所示：
 * <pre class="code">
 * HttpHeaders headers = new HttpHeaders();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;&gt;("Hello World", headers);
 * URI location = template.postForLocation("https://example.com", entity);
 * </pre>
 * 或者
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * 还可以在 Spring MVC 中作为 @Controller 方法的返回值使用：
 * <pre class="code">
 * &#64;GetMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 *
 * @param <T> 主体类型
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see org.springframework.web.client.RestTemplate
 * @see #getBody()
 * @see #getHeaders()
 * @since 3.0.2
 */
public class HttpEntity<T> {

	/**
	 * 空的 {@code HttpEntity}，没有主体或头部。
	 */
	public static final HttpEntity<?> EMPTY = new HttpEntity<>();

	/**
	 * 实体头部
	 */
	private final HttpHeaders headers;

	/**
	 * 实体主体
	 */
	@Nullable
	private final T body;


	/**
	 * 创建一个新的空 {@code HttpEntity}。
	 */
	protected HttpEntity() {
		this(null, null);
	}

	/**
	 * 创建一个具有给定主体但没有头部的新 {@code HttpEntity}。
	 *
	 * @param body 实体主体
	 */
	public HttpEntity(T body) {
		this(body, null);
	}

	/**
	 * 创建一个具有给定头部但没有主体的新 {@code HttpEntity}。
	 *
	 * @param headers 实体头部
	 */
	public HttpEntity(MultiValueMap<String, String> headers) {
		this(null, headers);
	}

	/**
	 * 创建一个具有给定主体和头部的新 {@code HttpEntity}。
	 *
	 * @param body    实体主体
	 * @param headers 实体头部
	 */
	public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
		this.body = body;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers != null ? headers : new HttpHeaders());
	}


	/**
	 * 返回此实体的头部。
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 返回此实体的主体。
	 */
	@Nullable
	public T getBody() {
		return this.body;
	}

	/**
	 * 表示此实体是否具有主体。
	 */
	public boolean hasBody() {
		return (this.body != null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		HttpEntity<?> otherEntity = (HttpEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.headers, otherEntity.headers) &&
				ObjectUtils.nullSafeEquals(this.body, otherEntity.body));
	}

	@Override
	public int hashCode() {
		return (ObjectUtils.nullSafeHashCode(this.headers) * 29 + ObjectUtils.nullSafeHashCode(this.body));
	}

	@Override
	public String toString() {
		// 创建一个 字符串构建器 对象
		StringBuilder builder = new StringBuilder("<");
		if (this.body != null) {
			// 如果消息体不为空，则将消息体添加到字符串构建器中
			builder.append(this.body);
			builder.append(',');
		}
		// 将消息头部添加到字符串构建器中
		builder.append(this.headers);
		// 添加结尾字符
		builder.append('>');
		// 返回构建的字符串
		return builder.toString();
	}

}
