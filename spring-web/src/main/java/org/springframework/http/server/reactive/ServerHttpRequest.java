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

package org.springframework.http.server.reactive;

import org.springframework.http.*;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

/**
 * 表示一个响应式服务器端 HTTP 请求。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	/**
	 * 如果可用，则返回表示基础连接的 id，否则返回用于关联日志消息的请求 id。
	 *
	 * @see org.springframework.web.server.ServerWebExchange#getLogPrefix()
	 * @since 5.1
	 */
	String getId();

	/**
	 * 返回一个结构化表示的完整请求路径，但不包括 {@link #getQueryParams() 查询}。
	 * <p>返回的路径分为 {@link RequestPath#contextPath()} 部分和其余的 {@link RequestPath#pathWithinApplication() pathWithinApplication} 部分。
	 * 后者可以传递给 {@link org.springframework.web.util.pattern.PathPattern} 的方法进行路径匹配。
	 */
	RequestPath getPath();

	/**
	 * 返回一个只读映射，其中包含解析和解码的查询参数值。
	 */
	MultiValueMap<String, String> getQueryParams();

	/**
	 * 返回客户端发送的 cookie 的只读映射。
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * 返回请求被接受的本地地址，如果可用。
	 *
	 * @since 5.2.3
	 */
	@Nullable
	default InetSocketAddress getLocalAddress() {
		return null;
	}

	/**
	 * 返回请求连接的远程地址，如果可用。
	 */
	@Nullable
	default InetSocketAddress getRemoteAddress() {
		return null;
	}

	/**
	 * 返回 SSL 会话信息，如果请求是通过安全协议传输的，包括 SSL 证书，则返回可用。
	 *
	 * @return 会话信息，如果没有可用则返回 {@code null}
	 * @since 5.0.2
	 */
	@Nullable
	default SslInfo getSslInfo() {
		return null;
	}

	/**
	 * 返回一个构建器，通过包装它并返回已更改的值或委托回此实例的 {@link ServerHttpRequestDecorator}，以改变此请求的属性。
	 */
	default ServerHttpRequest.Builder mutate() {
		return new DefaultServerHttpRequestBuilder(this);
	}


	/**
	 * 用于修改现有 {@link ServerHttpRequest} 的构建器。
	 */
	interface Builder {

		/**
		 * 设置要返回的 HTTP 方法。
		 */
		Builder method(HttpMethod httpMethod);

		/**
		 * 设置要使用的 URI，并具有以下条件：
		 * <ul>
		 * <li>如果 {@link #path(String) path} 也被设置了，则它会覆盖此处提供的 URI 的路径。
		 * <li>如果 {@link #contextPath(String) contextPath} 也被设置了，或者已经存在，则它必须匹配此处提供的 URI 的路径的起始部分。
		 * </ul>
		 */
		Builder uri(URI uri);

		/**
		 * 设置要使用的路径，而不是请求 URI 的 {@code "rawPath"}，具有以下条件：
		 * <ul>
		 * <li>如果 {@link #uri(URI) uri} 也被设置了，则此处给定的路径会覆盖给定 URI 的路径。
		 * <li>如果 {@link #contextPath(String) contextPath} 也被设置了，或者已经存在，则它必须匹配此处给定的路径的起始部分。
		 * <li>给定的值必须以斜杠开头。
		 * </ul>
		 */
		Builder path(String path);

		/**
		 * 设置要使用的上下文路径。
		 * <p>给定的值必须是一个有效的 {@link RequestPath#contextPath() contextPath}，并且它必须与请求的 URI 的路径的起始部分匹配。
		 * 这意味着更改上下文路径，也意味着通过 {@link #path(String)} 更改路径。
		 */
		Builder contextPath(String contextPath);

		/**
		 * 设置或覆盖给定名称下的指定头部值。
		 * <p>如果需要添加头部值、删除头部等，请使用 {@link #headers(Consumer)} 来进行更精细的控制。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @see #headers(Consumer)
		 * @since 5.1.9
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 操作请求头部。提供的 {@code HttpHeaders} 包含当前的请求头部，
		 * 因此 {@code Consumer} 可以 {@linkplain HttpHeaders#set(String, String) 覆盖} 或 {@linkplain HttpHeaders#remove(Object) 删除} 现有值，
		 * 或使用任何其他 {@link HttpHeaders} 方法。
		 *
		 * @see #header(String, String...)
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置 SSL 会话信息。这在 TLS 终止是在路由器上完成，但 SSL 信息通过其他方式（如通过头部）提供的环境中可能很有用。
		 *
		 * @since 5.0.7
		 */
		Builder sslInfo(SslInfo sslInfo);

		/**
		 * 设置远程客户端的地址。
		 *
		 * @since 5.3
		 */
		Builder remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 使用修改后的属性构建一个 {@link ServerHttpRequest} 装饰器。
		 */
		ServerHttpRequest build();
	}

}
