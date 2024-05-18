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

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 表示由{@linkplain HandlerFunction 处理程序函数}或{@linkplain HandlerFilterFunction 过滤器函数}返回的类型化的服务器端HTTP响应。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerResponse {

	/**
	 * 返回此响应的状态码。
	 *
	 * @return 作为HttpStatus枚举值的状态码
	 * @throws IllegalArgumentException 如果是未知的HTTP状态码
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能是非标准的）状态码。
	 *
	 * @return 整数形式的状态码
	 * @see #statusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的头信息。
	 */
	HttpHeaders headers();

	/**
	 * 返回此响应的cookie。
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * 将此响应写入给定的servlet响应。
	 *
	 * @param request  当前请求
	 * @param response 要写入的响应
	 * @param context  写入时使用的上下文
	 * @return 一个用于渲染的{@code ModelAndView}，如果直接处理则返回{@code null}
	 */
	@Nullable
	ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException;


	// 静态方法

	/**
	 * 使用给定响应的状态码和头信息创建一个构建器。
	 *
	 * @param other 要复制状态和头信息的响应
	 * @return 创建的构建器
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * 使用给定的HTTP状态创建一个构建器。
	 *
	 * @param status 响应状态
	 * @return 创建的构建器
	 */
	static BodyBuilder status(HttpStatus status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 使用给定的HTTP状态创建一个构建器。
	 *
	 * @param status 响应状态
	 * @return 创建的构建器
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 使用状态设置为{@linkplain HttpStatus#OK 200 OK}创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 使用状态设置为{@linkplain HttpStatus#CREATED 201 Created}和位置标头设置为给定的URI创建一个构建器。
	 *
	 * @param location 位置URI
	 * @return 创建的构建器
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * 使用状态设置为{@linkplain HttpStatus#ACCEPTED 202 Accepted}创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 使用状态设置为{@linkplain HttpStatus#NO_CONTENT 204 No Content}创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 使用{@linkplain HttpStatus#SEE_OTHER 303 See Other}状态和位置标头设置为给定的URI创建一个构建器。
	 *
	 * @param location 位置URI
	 * @return 创建的构建器
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * 使用{@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}状态和位置标头设置为给定的URI创建一个构建器。
	 *
	 * @param location 位置URI
	 * @return 创建的构建器
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 使用{@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}状态和位置标头设置为给定的URI创建一个构建器。
	 *
	 * @param location 位置URI
	 * @return 创建的构建器
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 使用{@linkplain HttpStatus#BAD_REQUEST 400 Bad Request}状态创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 使用{@linkplain HttpStatus#NOT_FOUND 404 Not Found}状态创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 使用{@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity}状态创建一个构建器。
	 *
	 * @return 创建的构建器
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * 使用给定的异步响应创建（构建的）响应。
	 * 参数{@code asyncResponse}可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;}或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或任何
	 * 单个{@code ServerResponse}的异步生产者，可以通过
	 * {@link ReactiveAdapterRegistry}进行适配）。
	 *
	 * <p>此方法可用于基于异步结果设置响应状态码、头部和主体。
	 * 如果只有主体是异步的，可以使用{@link BodyBuilder#body(Object)}代替。
	 *
	 * @param asyncResponse 一个{@code CompletableFuture<ServerResponse>}或{@code Publisher<ServerResponse>}
	 * @return 异步响应
	 * @since 5.3
	 */
	static ServerResponse async(Object asyncResponse) {
		return DefaultAsyncServerResponse.create(asyncResponse, null);
	}

	/**
	 * 使用给定的异步响应创建（构建的）响应。
	 * 参数{@code asyncResponse}可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;}或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或任何
	 * 单个{@code ServerResponse}的异步生产者，可以通过
	 * {@link ReactiveAdapterRegistry}进行适配）。
	 *
	 * <p>此方法可用于基于异步结果设置响应状态码、头部和主体。
	 * 如果只有主体是异步的，可以使用{@link BodyBuilder#body(Object)}代替。
	 *
	 * @param asyncResponse 一个{@code CompletableFuture<ServerResponse>}或{@code Publisher<ServerResponse>}
	 * @param timeout       最长等待超时时间
	 * @return 异步响应
	 * @since 5.3.2
	 */
	static ServerResponse async(Object asyncResponse, Duration timeout) {
		return DefaultAsyncServerResponse.create(asyncResponse, timeout);
	}

	/**
	 * 创建一个服务器发送事件响应。提供给{@code consumer}的{@link SseBuilder}可用于构建和发送事件。
	 *
	 * <p>例如：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>或者，设置id和事件类型：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42")
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * @param consumer 一个消费者，将提供一个事件构建器
	 * @return 服务器端事件响应
	 * @see <a href="https://www.w3.org/TR/eventsource/">服务器发送事件</a>
	 * @since 5.3.2
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer) {
		return SseServerResponse.create(consumer, null);
	}

	/**
	 * 创建一个服务器发送事件响应。提供给{@code consumer}的{@link SseBuilder}可用于构建和发送事件。
	 *
	 * <p>例如：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>或者，设置id和事件类型：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42")
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * @param consumer 一个消费者，将提供一个事件构建器
	 * @param timeout  最长等待超时时间
	 * @return 服务器端事件响应
	 * @see <a href="https://www.w3.org/TR/eventsource/">服务器发送事件</a>
	 * @since 5.3.2
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer, Duration timeout) {
		return SseServerResponse.create(consumer, timeout);
	}


	/**
	 * 定义一个用于向响应添加头部的构建器。
	 *
	 * @param <B> 构建器子类
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者处理此响应的头部。
		 * 提供给消费者的头部是“活动的”，因此消费者可以用于
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有的头部值，
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 *
		 * @param headersConsumer 消费{@code HttpHeaders}的函数
		 * @return 此构建器
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将给定的cookie添加到响应中。
		 *
		 * @param cookie 要添加的cookie
		 * @return 此构建器
		 */
		B cookie(Cookie cookie);

		/**
		 * 使用给定的消费者处理此响应的cookie。
		 * 提供给消费者的cookie是“活动的”，因此消费者可以用于
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有的cookie，
		 * {@linkplain MultiValueMap#remove(Object) 删除}cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 *
		 * @param cookiesConsumer 消费cookie的函数
		 * @return 此构建器
		 */
		B cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置允许的{@link HttpMethod HTTP方法}集，如{@code Allow}头所指定。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的{@link HttpMethod HTTP方法}集，如{@code Allow}头所指定。
		 *
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置主体的实体标签，如{@code ETag}头所指定。
		 *
		 * @param eTag 新实体标签
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * 设置资源上次更改的时间，如{@code Last-Modified}头所指定。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源上次更改的时间，如{@code Last-Modified}头所指定。
		 *
		 * @param lastModified 最后修改的日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(Instant lastModified);

		/**
		 * 设置资源的位置，如{@code Location}头所指定。
		 *
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令，如HTTP 1.1的{@code Cache-Control}头所指定。
		 * <p>可以像{@code CacheControl.maxAge(3600).cachePublic().noTransform()}这样构建{@code CacheControl}实例。
		 *
		 * @param cacheControl 用于缓存相关的HTTP响应头的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如“Accept-Language”），
		 * 添加到“Vary”响应头以通知客户端响应受内容协商和基于给定请求头值的差异的影响。
		 * 仅在响应“Vary”头中不存在时才添加配置的请求头名称。
		 *
		 * @param requestHeaders 请求头名称
		 * @return 此构建器
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 使用没有主体的响应实体构建。
		 */
		ServerResponse build();

		/**
		 * 使用自定义写函数构建响应实体。
		 *
		 * @param writeFunction 用于写入到{@link HttpServletResponse}的函数
		 */
		ServerResponse build(BiFunction<HttpServletRequest, HttpServletResponse,
				ModelAndView> writeFunction);

	}


	/**
	 * 定义一个用于向响应添加主体的构建器。
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置主体的字节长度，如{@code Content-Length}头所指定。
		 *
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置主体的{@linkplain MediaType 媒体类型}，如{@code Content-Type}头所指定。
		 *
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 将响应的主体设置为给定的{@code Object}并返回它。
		 *
		 * <p>通过提供{@link CompletionStage}或{@link Publisher}作为主体来支持异步响应主体
		 * （或通过{@link ReactiveAdapterRegistry}适配的任何单个实体的异步生产者）。
		 *
		 * @param body 响应的主体
		 * @return 构建的响应
		 */
		ServerResponse body(Object body);

		/**
		 * 将响应的主体设置为给定的{@code Object}并返回它。参数{@code bodyType}用于捕获泛型类型。
		 *
		 * @param body     响应的主体
		 * @param bodyType 主体的类型，用于捕获泛型类型
		 * @return 构建的响应
		 */
		<T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType);

		/**
		 * 使用给定的{@code name}和{@code modelAttributes}渲染模板。
		 * 当使用此方法时，模型属性将映射到
		 * {@linkplain org.springframework.core.Conventions#getVariableName 生成的名称}下。
		 * <p><em>注意：使用此方法时，空的{@link Collection Collections}不会添加到模型中，
		 * 因为我们无法正确确定真实的约定名称。</em>
		 *
		 * @param name            要渲染的模板的名称
		 * @param modelAttributes 用于渲染模板的模型属性
		 * @return 构建的响应
		 */
		ServerResponse render(String name, Object... modelAttributes);

		/**
		 * 使用给定的{@code name}和{@code model}渲染模板。
		 *
		 * @param name  要渲染的模板的名称
		 * @param model 用于渲染模板的模型
		 * @return 构建的响应
		 */
		ServerResponse render(String name, Map<String, ?> model);
	}


	/**
	 * 定义用于发送服务器发送事件的响应体的构建器。
	 *
	 * @since 5.3.2
	 */
	interface SseBuilder {

		/**
		 * 将给定对象作为服务器发送事件发送。
		 * 字符串将作为UTF-8编码的字节发送，其他对象将使用
		 * {@linkplain HttpMessageConverter 消息转换器}转换为JSON。
		 *
		 * <p>此便捷方法与 {@link #data(Object)} 具有相同效果。
		 *
		 * @param object 要发送的对象
		 * @throws IOException 发生I/O错误时
		 */
		void send(Object object) throws IOException;

		/**
		 * 添加一个SSE "id"行。
		 *
		 * @param id 事件标识符
		 * @return 此构建器
		 */
		SseBuilder id(String id);

		/**
		 * 添加一个SSE "event"行。
		 *
		 * @param eventName 事件名称
		 * @return 此构建器
		 */
		SseBuilder event(String eventName);

		/**
		 * 添加一个SSE "retry"行。
		 *
		 * @param duration 要转换为毫秒的持续时间
		 * @return 此构建器
		 */
		SseBuilder retry(Duration duration);

		/**
		 * 添加一个SSE注释。
		 *
		 * @param comment 注释
		 * @return 此构建器
		 */
		SseBuilder comment(String comment);

		/**
		 * 为给定对象添加一个SSE "data"行，并将构建的服务器发送事件发送到客户端。
		 * 字符串将作为UTF-8编码的字节发送，其他对象将使用
		 * {@linkplain HttpMessageConverter 消息转换器}转换为JSON。
		 *
		 * @param object 要作为数据发送的对象
		 * @throws IOException 发生I/O错误时
		 */
		void data(Object object) throws IOException;

		/**
		 * 使用给定错误完成事件流。
		 *
		 * <p>Throwable被发送回Spring MVC，并传递给
		 * 其异常处理机制。由于此时已提交响应，
		 * 因此响应状态不能更改。
		 *
		 * @param t 要分派的Throwable
		 */
		void error(Throwable t);

		/**
		 * 完成事件流。
		 */
		void complete();

		/**
		 * 注册在SSE请求超时时调用的回调。
		 *
		 * @param onTimeout 超时时要调用的回调
		 * @return 此构建器
		 */
		SseBuilder onTimeout(Runnable onTimeout);

		/**
		 * 注册在SSE处理过程中发生错误时调用的回调。
		 *
		 * @param onError 发生错误时要调用的回调
		 * @return 此构建器
		 */
		SseBuilder onError(Consumer<Throwable> onError);

		/**
		 * 注册在SSE请求完成时调用的回调。
		 *
		 * @param onCompletion 完成时要调用的回调
		 * @return 此构建器
		 */
		SseBuilder onComplete(Runnable onCompletion);
	}


	/**
	 * 定义在 {@link #writeTo(HttpServletRequest, HttpServletResponse, Context)} 中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回要用于响应体转换的 {@link HttpMessageConverter HttpMessageConverters}。
		 *
		 * @return 消息转换器的列表
		 */
		List<HttpMessageConverter<?>> messageConverters();

	}

}
