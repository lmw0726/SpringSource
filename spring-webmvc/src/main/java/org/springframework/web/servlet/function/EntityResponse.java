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

package org.springframework.web.servlet.function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Consumer;

/**
 * {@link ServerResponse}的实体特定子类型，暴露实体数据。
 *
 * @param <T> 实体类型
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * 返回组成此响应的实体。
	 */
	T entity();


	// Static builder methods

	/**
	 * 使用给定对象创建一个构建器。
	 *
	 * @param t   要表示响应正文的对象
	 * @param <T> 实体中包含的元素类型
	 * @return 创建的构建器
	 */
	static <T> Builder<T> fromObject(T t) {
		return DefaultEntityResponseBuilder.fromObject(t);
	}

	/**
	 * 使用给定对象和类型引用创建一个构建器。
	 *
	 * @param t          要表示响应正文的对象
	 * @param entityType 实体的类型，用于捕获泛型类型
	 * @param <T>        实体中包含的元素类型
	 * @return 创建的构建器
	 */
	static <T> Builder<T> fromObject(T t, ParameterizedTypeReference<T> entityType) {
		return DefaultEntityResponseBuilder.fromObject(t, entityType);
	}


	/**
	 * 定义用于{@code EntityResponse}的构建器。
	 *
	 * @param <T> 实体类型
	 */
	interface Builder<T> {

		/**
		 * 将给定的头值添加到给定名称下。
		 *
		 * @param headerName   头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此响应的标头。提供给消费者的标头是“实时”的，因此消费者可以用于{@linkplain HttpHeaders#set(String, String)覆盖}现有标头值，
		 * {@linkplain HttpHeaders#remove(Object)删除}值，或使用任何其他{@link HttpHeaders}方法。
		 *
		 * @param headersConsumer 消费{@code HttpHeaders}的函数
		 * @return 此构建器
		 */
		Builder<T> headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置HTTP状态。
		 *
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * 设置HTTP状态。
		 *
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder<T> status(int status);

		/**
		 * 将给定的Cookie添加到响应中。
		 *
		 * @param cookie 要添加的Cookie
		 * @return 此构建器
		 */
		Builder<T> cookie(Cookie cookie);

		/**
		 * 使用给定的消费者操作此响应的Cookie。提供给消费者的Cookie是“实时”的，因此消费者可以用于{@linkplain MultiValueMap#set(Object, Object)覆盖}现有Cookie，
		 * {@linkplain MultiValueMap#remove(Object)删除}Cookie，或使用任何其他{@link MultiValueMap}方法。
		 *
		 * @param cookiesConsumer 消费Cookie的函数
		 * @return 此构建器
		 */
		Builder<T> cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置允许的{@link HttpMethod HTTP方法}集，如{@code Allow}头指定的那样。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的{@link HttpMethod HTTP方法}集，如{@code Allow}头指定的那样。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置正文的实体标记，如{@code ETag}头所指定的那样。
		 *
		 * @param etag 新的实体标记
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String etag);

		/**
		 * 设置资源上次更改的时间，如{@code Last-Modified}头所指定的那样。
		 * <p>日期应该以自1970年1月1日GMT以来的毫秒数指定。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源上次更改的时间，如{@code Last-Modified}头所指定的那样。
		 * <p>日期应该以自1970年1月1日GMT以来的毫秒数指定。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 * @since 5.1.4
		 */
		Builder<T> lastModified(Instant lastModified);

		/**
		 * 设置资源的位置，如{@code Location}头所指定的那样。
		 *
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		Builder<T> location(URI location);

		/**
		 * 设置资源的缓存指令，如HTTP 1.1 {@code Cache-Control}头所指定的那样。
		 * <p>{@code CacheControl}实例可以像{@code CacheControl.maxAge(3600).cachePublic().noTransform()}这样构建。
		 *
		 * @param cacheControl 用于缓存相关HTTP响应头的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		Builder<T> cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如“Accept-Language”），以添加到“Vary”响应头，
		 * 以通知客户端响应受内容协商的影响，并且根据给定请求头的值而变化。
		 * 仅当配置的请求头名称尚未存在于响应的“Vary”头中时，才会添加它们。
		 *
		 * @param requestHeaders 请求头名称
		 * @return 此构建器
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * 设置正文的字节长度，如{@code Content-Length}头所指定的那样。
		 *
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * 设置正文的{@linkplain MediaType 媒体类型}，如{@code Content-Type}头所指定的那样。
		 *
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * 构建响应。
		 *
		 * @return 构建的响应
		 */
		EntityResponse<T> build();
	}

}
