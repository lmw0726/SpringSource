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
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 表示一个有类型的服务器端 HTTP 响应，由处理函数或过滤器函数返回。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerResponse {

	/**
	 * 返回此响应的状态码。
	 *
	 * @return HttpStatus 枚举值表示的状态
	 * @throws IllegalArgumentException 如果是未知的 HTTP 状态码
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能是非标准的）状态码。
	 *
	 * @return 整数形式的状态码
	 * @see #statusCode()
	 * @see HttpStatus#resolve(int)
	 * @since 5.2
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的头部信息。
	 */
	HttpHeaders headers();

	/**
	 * 返回此响应的 cookies。
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * 将此响应写入给定的 web exchange。
	 *
	 * @param exchange 要写入的 web exchange
	 * @param context  写入时使用的上下文
	 * @return {@code Mono<Void>} 表示写入完成的信号
	 */
	Mono<Void> writeTo(ServerWebExchange exchange, Context context);


	// 静态方法

	/**
	 * 使用给定响应的状态码和头部信息创建一个构建器。
	 *
	 * @param other 要复制状态码和头部信息的响应
	 * @return 创建的构建器
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * 使用给定的 HTTP 状态码创建一个构建器。
	 *
	 * @param status 响应状态
	 * @return 创建的构建器
	 */
	static BodyBuilder status(HttpStatus status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 使用给定的 HTTP 状态码创建一个构建器。
	 *
	 * @param status 响应状态码
	 * @return 创建的构建器
	 * @since 5.0.3
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#OK 200 OK} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#CREATED 201 Created}，且具有给定 URI 作为位置头部信息的构建器。
	 *
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#ACCEPTED 202 Accepted} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#NO_CONTENT 204 No Content} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#SEE_OTHER 303 See Other}，
	 * 并且具有给定 URI 作为位置头部信息的构建器。
	 *
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}，
	 * 并且具有给定 URI 作为位置头部信息的构建器。
	 *
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}，
	 * 并且具有给定 URI 作为位置头部信息的构建器。
	 *
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#NOT_FOUND 404 Not Found} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 创建一个状态为 {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} 的构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}


	/**
	 * 定义一个向响应添加头信息的构建器。
	 *
	 * @param <B> 构建器子类
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的头值。
		 *
		 * @param headerName   头名称
		 * @param headerValues 头值
		 * @return 该构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者来处理该响应的头信息。提供给消费者的头信息是“活动的”，因此消费者可以用于覆盖现有的头值、删除值，
		 * 或使用任何其他 {@link HttpHeaders} 方法。
		 *
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 该构建器
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 向响应中添加给定的 cookie。
		 *
		 * @param cookie 要添加的 cookie
		 * @return 该构建器
		 */
		B cookie(ResponseCookie cookie);

		/**
		 * 使用给定的消费者来处理该响应的 cookies。提供给消费者的 cookies 是“活动的”，因此消费者可以用于覆盖现有的 cookies、
		 * 删除 cookies，或使用任何其他 {@link MultiValueMap} 方法。
		 *
		 * @param cookiesConsumer 消费 cookies 的函数
		 * @return 该构建器
		 */
		B cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 设置 {@code Allow} 头信息中允许的 {@link HttpMethod HTTP 方法} 集合。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 该构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置 {@code Allow} 头信息中允许的 {@link HttpMethod HTTP 方法} 集合。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 该构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置 {@code ETag} 头信息的实体标签。
		 *
		 * @param eTag 新的实体标签
		 * @return 该构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * 设置 {@code Last-Modified} 头信息的资源最后修改时间。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 该构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置 {@code Last-Modified} 头信息的资源最后修改时间。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 该构建器
		 * @see HttpHeaders#setLastModified(long)
		 * @since 5.1.4
		 */
		B lastModified(Instant lastModified);

		/**
		 * 设置 {@code Location} 头信息的资源位置。
		 *
		 * @param location 位置
		 * @return 该构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置 {@code Cache-Control} 头信息的缓存指令。
		 * <p>可以像这样构建 {@code CacheControl} 实例：
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}。
		 *
		 * @param cacheControl 用于缓存相关的 HTTP 响应头的构建器
		 * @return 该构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到“Vary”响应头中，
		 * 以通知客户端响应受内容协商影响并根据给定请求头的值变化而异。仅当响应“Vary”头中尚未存在配置的请求头名称时，才会添加配置的请求头名称。
		 *
		 * @param requestHeaders 请求头名称
		 * @return 该构建器
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 生成不带响应体的响应实体。
		 */
		Mono<ServerResponse> build();

		/**
		 * 生成不带响应体的响应实体。
		 * <p>当给定的 {@code voidPublisher} 完成时，响应将被提交。
		 *
		 * @param voidPublisher 表示响应何时被提交的发布者
		 */
		Mono<ServerResponse> build(Publisher<Void> voidPublisher);

		/**
		 * 使用自定义写入函数生成响应实体。
		 *
		 * @param writeFunction 写入到 {@link ServerWebExchange} 的函数
		 */
		Mono<ServerResponse> build(BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction);
	}


	/**
	 * 定义一个向响应添加主体的构建器。
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置主体的字节长度，由 {@code Content-Length} 头信息指定。
		 *
		 * @param contentLength 内容长度
		 * @return 该构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置主体的 {@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头信息指定。
		 *
		 * @param contentType 内容类型
		 * @return 该构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 添加序列化提示，如 {@link Jackson2CodecSupport#JSON_VIEW_HINT}，以自定义主体的序列化方式。
		 *
		 * @param key   提示键
		 * @param value 提示值
		 */
		BodyBuilder hint(String key, Object value);

		/**
		 * 使用给定的消费者自定义序列化提示。
		 *
		 * @param hintsConsumer 消费提示的函数
		 * @return 该构建器
		 * @since 5.1.6
		 */
		BodyBuilder hints(Consumer<Map<String, Object>> hintsConsumer);

		/**
		 * 将响应的主体设置为给定的 {@code Object} 并返回它。这是使用 {@link #body(BodyInserter)} 和
		 * {@linkplain BodyInserters#fromValue value inserter} 的快捷方式。
		 *
		 * @param body 响应的主体
		 * @return 构建的响应
		 * @throws IllegalArgumentException 如果 {@code body} 是已知的 {@link Publisher} 或由 {@link ReactiveAdapterRegistry} 生产的
		 * @since 5.2
		 */
		Mono<ServerResponse> bodyValue(Object body);

		/**
		 * 从给定的 {@code Publisher} 设置主体。是 {@link #body(BodyInserter)} 的快捷方式，
		 * 使用 {@linkplain BodyInserters#fromPublisher Publisher inserter}。
		 *
		 * @param publisher    要写入响应的 {@code Publisher}
		 * @param elementClass 发布的元素类型
		 * @param <T>          发布的元素类型
		 * @param <P>          {@code Publisher} 的类型
		 * @return 构建的响应
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许使用通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 *
		 * @param publisher      要用于写入响应的 {@code Publisher}
		 * @param elementTypeRef 产生的元素类型
		 * @param <T>            发布的元素类型
		 * @param <P>            {@code Publisher} 的类型
		 * @return 构建的响应
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher,
															  ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许使用通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 *
		 * @param producer     要用于写入请求的生产者
		 * @param elementClass 产生的元素类型
		 * @return 构建的响应
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, Class<?> elementClass);

		/**
		 * {@link #body(Publisher, ParameterizedTypeReference)} 的变体，允许使用通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 *
		 * @param producer       要用于写入响应的生产者
		 * @param elementTypeRef 产生的元素类型
		 * @return 构建的响应
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * 将响应的主体设置为给定的 {@code BodyInserter} 并返回它。
		 *
		 * @param inserter 写入响应的 {@code BodyInserter}
		 * @return 构建的响应
		 */
		Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter);

		/**
		 * 将响应的主体设置为给定的 {@code Object} 并返回它。从 Spring Framework 5.2 开始，此方法委托给 {@link #bodyValue(Object)}。
		 *
		 * @deprecated 自 Spring Framework 5.2 起，建议使用 {@link #bodyValue(Object)}
		 */
		@Deprecated
		Mono<ServerResponse> syncBody(Object body);

		/**
		 * 使用给定的 {@code name} 和 {@code modelAttributes} 渲染模板。
		 * 模型属性将映射在 {@linkplain org.springframework.core.Conventions#getVariableName 生成的名称} 下。
		 * <p><em>注意：当使用此方法时，空 {@link Collection 集合} 不会添加到模型中，因为我们无法正确确定真实的约定名称。</em>
		 *
		 * @param name            要渲染的模板名称
		 * @param modelAttributes 用于渲染模板的模型属性
		 * @return 构建的响应
		 */
		Mono<ServerResponse> render(String name, Object... modelAttributes);

		/**
		 * 使用给定的 {@code name} 和 {@code model} 渲染模板。
		 *
		 * @param name  要渲染的模板名称
		 * @param model 用于渲染模板的模型
		 * @return 构建的响应
		 */
		Mono<ServerResponse> render(String name, Map<String, ?> model);
	}


	/**
	 * 定义在 {@link #writeTo(ServerWebExchange, Context)} 过程中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于响应主体转换的 {@link HttpMessageWriter HttpMessageWriters}。
		 *
		 * @return 消息写入器列表
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * 返回用于视图名称解析的 {@link ViewResolver ViewResolvers}。
		 *
		 * @return 视图解析器列表
		 */
		List<ViewResolver> viewResolvers();
	}

}
