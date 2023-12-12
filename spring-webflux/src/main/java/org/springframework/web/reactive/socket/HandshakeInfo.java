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

package org.springframework.web.reactive.socket;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * Simple container of information related to the handshake request that started
 * the {@link WebSocketSession} session.
 *
 * @author Rossen Stoyanchev
 * @see WebSocketSession#getHandshakeInfo()
 * @since 5.0
 */
public class HandshakeInfo {
	// 空的 Cookie Map，用于初始化
	private static final MultiValueMap<String, HttpCookie> EMPTY_COOKIES =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());

	// URI
	private final URI uri;

	// 主体 Mono
	private final Mono<Principal> principalMono;

	// 头部信息
	private final HttpHeaders headers;

	// Cookie Map
	private final MultiValueMap<String, HttpCookie> cookies;

	// 协议
	@Nullable
	private final String protocol;

	// 远程地址
	@Nullable
	private final InetSocketAddress remoteAddress;

	// 属性
	private final Map<String, Object> attributes;

	// 日志前缀
	@Nullable
	private final String logPrefix;


	/**
	 * 构造函数，包含关于握手的基本信息。
	 *
	 * @param uri       终端点 URL
	 * @param headers   服务器的请求头或响应头或客户端的请求头
	 * @param principal 会话的主体
	 * @param protocol  协商的子协议（可能为 null）
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal, @Nullable String protocol) {
		this(uri, headers, EMPTY_COOKIES, principal, protocol, null, Collections.emptyMap(), null);
	}

	/**
	 * 针对服务器端使用的构造函数，包含远程地址、属性和日志前缀等额外信息。
	 *
	 * @param uri           终端点 URL
	 * @param headers       服务器的请求头
	 * @param principal     会话的主体
	 * @param protocol      协商的子协议（可能为 null）
	 * @param remoteAddress 客户端的远程地址
	 * @param attributes    WebSocket 会话的初始属性
	 * @param logPrefix     握手请求的日志前缀
	 * @since 5.1
	 * @deprecated 自 5.3.5 起已弃用，推荐使用 {@link #HandshakeInfo(URI, HttpHeaders, MultiValueMap, Mono, String, InetSocketAddress, Map, String)}
	 */
	@Deprecated
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal,
						 @Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
						 Map<String, Object> attributes, @Nullable String logPrefix) {

		this(uri, headers, EMPTY_COOKIES, principal, protocol, remoteAddress, attributes, logPrefix);
	}

	/**
	 * 针对服务器端使用的构造函数，包含 Cookies、远程地址、属性和日志前缀等额外信息。
	 *
	 * @param uri           终端点 URL
	 * @param headers       服务器的请求头
	 * @param cookies       服务器的请求 Cookies
	 * @param principal     会话的主体
	 * @param protocol      协商的子协议（可能为 null）
	 * @param remoteAddress 客户端的远程地址
	 * @param attributes    WebSocket 会话的初始属性
	 * @param logPrefix     握手请求的日志前缀
	 * @since 5.3.5
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
						 Mono<Principal> principal, @Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
						 Map<String, Object> attributes, @Nullable String logPrefix) {

		Assert.notNull(uri, "URI is required");
		Assert.notNull(headers, "HttpHeaders are required");
		Assert.notNull(cookies, "`cookies` are required");
		Assert.notNull(principal, "Principal is required");
		Assert.notNull(attributes, "'attributes' is required");

		this.uri = uri;
		this.headers = headers;
		this.cookies = cookies;
		this.principalMono = principal;
		this.protocol = protocol;
		this.remoteAddress = remoteAddress;
		this.attributes = attributes;
		this.logPrefix = logPrefix;
	}


	/**
	 * 返回 WebSocket 终端点的 URL。
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * 返回 WebSocket 握手请求中的 HTTP 头部，对于服务器会话是服务器请求的头部，
	 * 对于客户端会话是客户端响应的头部。
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 对于服务器会话，返回握手请求中的服务器请求 Cookie。
	 * 对于客户端会话，返回一个空映射。
	 *
	 * @since 5.3.5
	 */
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.cookies;
	}

	/**
	 * 返回与握手请求关联的主体，如果有的话。
	 */
	public Mono<Principal> getPrincipal() {
		return this.principalMono;
	}

	/**
	 * 握手时协商的子协议，如果没有则返回 null。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">
	 * https://tools.ietf.org/html/rfc6455#section-1.9</a>
	 */
	@Nullable
	public String getSubProtocol() {
		return this.protocol;
	}


	/**
	 * 对于服务器会话，返回握手请求的远程地址。
	 * 对于客户端会话，返回 null。
	 *
	 * @since 5.1
	 */
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * 从握手请求中提取的属性，用于添加到会话中。
	 *
	 * @since 5.1
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * 握手中用于关联日志消息的日志前缀，如果未指定则返回 null。
	 *
	 * @return 日志前缀，如果未指定则返回 null
	 * @since 5.1
	 */
	@Nullable
	public String getLogPrefix() {
		return this.logPrefix;
	}

	/**
	 * 返回此对象的字符串表示形式，包括 URI 信息。
	 */
	@Override
	public String toString() {
		return "HandshakeInfo[uri=" + this.uri + "]";
	}

}
