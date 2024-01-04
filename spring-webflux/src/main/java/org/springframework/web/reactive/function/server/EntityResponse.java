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

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 表示公开实体数据的 {@link ServerResponse} 的实体特定子类型。
 *
 * @param <T> 实体类型
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * 返回构成此响应的实体。
	 */
	T entity();

	/**
	 * 返回 {@code BodyInserter}，该插入器将实体写入输出流。
	 */
	BodyInserter<T, ? super ServerHttpResponse> inserter();

	// 静态构建器方法

	/**
	 * 使用给定对象创建一个构建器。
	 *
	 * @param body 对象，表示响应正文
	 * @param <T>  正文的类型
	 * @return 创建的构建器
	 */
	static <T> Builder<T> fromObject(T body) {
		return new DefaultEntityResponseBuilder<>(body, BodyInserters.fromValue(body));
	}

	/**
	 * 使用给定生产者创建一个构建器。
	 *
	 * @param producer     表示响应正文的生产者
	 * @param elementClass 发布器中包含的元素的类
	 * @return 创建的构建器
	 * @since 5.2
	 */
	static <T> Builder<T> fromProducer(T producer, Class<?> elementClass) {
		return new DefaultEntityResponseBuilder<>(producer,
				BodyInserters.fromProducer(producer, elementClass));
	}

	/**
	 * 使用给定生产者创建一个构建器。
	 *
	 * @param producer      表示响应正文的生产者
	 * @param typeReference 生产者中包含的元素的类型
	 * @return 创建的构建器
	 * @since 5.2
	 */
	static <T> Builder<T> fromProducer(T producer, ParameterizedTypeReference<?> typeReference) {
		return new DefaultEntityResponseBuilder<>(producer,
				BodyInserters.fromProducer(producer, typeReference));
	}

	/**
	 * 使用给定发布器创建一个构建器。
	 *
	 * @param publisher    表示响应正文的发布器
	 * @param elementClass 发布器中包含的元素的类
	 * @param <T>          发布器中包含的元素的类型
	 * @param <P>          发布器的类型
	 * @return 创建的构建器
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher, Class<T> elementClass) {
		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementClass));
	}

	/**
	 * 使用给定发布器创建一个构建器。
	 *
	 * @param publisher     表示响应正文的发布器
	 * @param typeReference 发布器中包含的元素的类型
	 * @param <T>           发布器中包含的元素的类型
	 * @param <P>           发布器的类型
	 * @return 创建的构建器
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher,
																ParameterizedTypeReference<T> typeReference) {

		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, typeReference));
	}


	/**
	 * 定义了 {@code EntityResponse} 的构建器。
	 *
	 * @param <T> 构建器类型的自引用
	 */
	interface Builder<T> {

		/**
		 * 在给定名称下添加给定的头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 该构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * 将给定的头部复制到实体的头部映射中。
		 *
		 * @param headers 要复制的现有 HttpHeaders
		 * @return 该构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> headers(HttpHeaders headers);

		/**
		 * 设置 HTTP 状态。
		 *
		 * @param status 响应状态
		 * @return 该构建器
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * 设置 HTTP 状态。
		 *
		 * @param status 响应状态
		 * @return 该构建器
		 * @since 5.0.3
		 */
		Builder<T> status(int status);

		/**
		 * 将给定的 cookie 添加到响应中。
		 *
		 * @param cookie 要添加的 cookie
		 * @return 该构建器
		 */
		Builder<T> cookie(ResponseCookie cookie);

		/**
		 * 使用给定的消费者操作此响应的 cookies。提供给消费者的 cookies 是“活动”的，
		 * 因此消费者可以用来覆盖现有的 cookies、移除 cookies，或使用任何其他 MultiValueMap 方法。
		 *
		 * @param cookiesConsumer 一个消费 cookies 的函数
		 * @return 该构建器
		 */
		Builder<T> cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法} 集，如 {@code Allow} 头部所指定。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 该构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法} 集，如 {@code Allow} 头部所指定。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 该构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置 body 的实体标签，由 {@code ETag} 头部指定。
		 *
		 * @param etag 新的实体标签
		 * @return 该构建器
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String etag);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 头部指定。
		 *
		 * @param lastModified 最后修改日期
		 * @return 该构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 头部指定。
		 *
		 * @param lastModified 最后修改日期
		 * @return 该构建器
		 * @see HttpHeaders#setLastModified(long)
		 * @since 5.1.4
		 */
		Builder<T> lastModified(Instant lastModified);

		/**
		 * 设置资源的位置，由 {@code Location} 头部指定。
		 *
		 * @param location 位置
		 * @return 该构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		Builder<T> location(URI location);

		/**
		 * 根据 HTTP 1.1 中 {@code Cache-Control} 头部的指令设置资源的缓存策略。
		 *
		 * @param cacheControl 用于缓存相关 HTTP 响应头部的构建器
		 * @return 该构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		Builder<T> cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到“Vary”响应头中，以通知客户端响应受内容协商的影响，
		 * 并根据给定请求头的值变化。仅在响应的“Vary”头中不存在的情况下才添加已配置的请求头名称。
		 *
		 * @param requestHeaders 请求头名称
		 * @return 该构建器
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * 设置正文的字节长度，由{@code Content-Length}头指定。
		 *
		 * @param contentLength 正文长度
		 * @return 该构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * 设置正文的 {@linkplain MediaType 媒体类型}，由{@code Content-Type}头指定。
		 *
		 * @param contentType 内容类型
		 * @return 该构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * 添加类似 {@link Jackson2CodecSupport#JSON_VIEW_HINT} 的序列化提示，以自定义正文的序列化方式。
		 *
		 * @param key   提示键
		 * @param value 提示值
		 */
		Builder<T> hint(String key, Object value);

		/**
		 * 使用给定的消费者定制序列化提示。
		 *
		 * @param hintsConsumer 消费提示的函数
		 * @return 该构建器
		 * @since 5.1.6
		 */
		Builder<T> hints(Consumer<Map<String, Object>> hintsConsumer);

		/**
		 * 构建响应。
		 *
		 * @return 构建的响应
		 */
		Mono<EntityResponse<T>> build();
	}

}
