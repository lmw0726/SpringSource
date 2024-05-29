/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.server.session;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于 Cookie 的 {@link WebSessionIdResolver}。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CookieWebSessionIdResolver implements WebSessionIdResolver {
	/**
	 * Cookie名称
	 */
	private String cookieName = "SESSION";

	/**
	 * Cookie的最大生命周期
	 */
	private Duration cookieMaxAge = Duration.ofSeconds(-1);

	/**
	 * Cookie初始化器
	 */
	@Nullable
	private Consumer<ResponseCookie.ResponseCookieBuilder> cookieInitializer = null;


	/**
	 * 设置用于会话 ID 的 Cookie 的名称。
	 * <p>默认设置为 "SESSION"。
	 *
	 * @param cookieName Cookie 名称
	 */
	public void setCookieName(String cookieName) {
		Assert.hasText(cookieName, "'cookieName' must not be empty");
		this.cookieName = cookieName;
	}

	/**
	 * 获取配置的 Cookie 名称。
	 */
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 设置持有会话 ID 的 Cookie 的 "Max-Age" 属性的值。
	 * <p>有关值的范围，请参见 {@link ResponseCookie#getMaxAge()}。
	 * <p>默认设置为 -1。
	 *
	 * @param maxAge 最大年龄持续时间值
	 */
	public void setCookieMaxAge(Duration maxAge) {
		this.cookieMaxAge = maxAge;
	}

	/**
	 * 获取会话 Cookie 的配置的 "Max-Age" 属性值。
	 */
	public Duration getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * 添加一个 {@link Consumer}，用于 {@code ResponseCookieBuilder} 的每个正在构建的 Cookie，
	 * 在调用 {@code build()} 之前将调用该方法。
	 *
	 * @param initializer 用于 Cookie 构建器的消费者
	 * @since 5.1
	 */
	public void addCookieInitializer(Consumer<ResponseCookie.ResponseCookieBuilder> initializer) {
		this.cookieInitializer = this.cookieInitializer != null ?
				this.cookieInitializer.andThen(initializer) : initializer;
	}


	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		// 获取请求中的Cookie映射
		MultiValueMap<String, HttpCookie> cookieMap = exchange.getRequest().getCookies();
		// 获取指定名称的Cookie列表
		List<HttpCookie> cookies = cookieMap.get(getCookieName());
		// 如果不存在该名称的Cookie，则返回空列表
		if (cookies == null) {
			return Collections.emptyList();
		}
		// 提取Cookie列表中的值并返回
		return cookies.stream().map(HttpCookie::getValue).collect(Collectors.toList());
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required");
		// 初始化会话Cookie
		ResponseCookie cookie = initSessionCookie(exchange, id, getCookieMaxAge());
		// 将Cookie设置到响应中
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		// 使用空字符串和零持续时间初始化会话Cookie
		ResponseCookie cookie = initSessionCookie(exchange, "", Duration.ZERO);
		// 将Cookie设置到响应中
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	private ResponseCookie initSessionCookie(
			ServerWebExchange exchange, String id, Duration maxAge) {

		// 使用提供的ID和最大年龄初始化响应Cookie构建器
		ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(this.cookieName, id)
				// 设置Cookie的路径
				.path(exchange.getRequest().getPath().contextPath().value() + "/")
				// 设置Cookie的最大年龄
				.maxAge(maxAge)
				// 设置Cookie为HTTP Only
				.httpOnly(true)
				// 根据请求的协议设置Secure属性
				.secure("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme()))
				// 设置SameSite属性为Lax
				.sameSite("Lax");

		// 如果提供了Cookie初始化器，则调用它来定制Cookie
		if (this.cookieInitializer != null) {
			this.cookieInitializer.accept(cookieBuilder);
		}

		// 构建并返回响应Cookie
		return cookieBuilder.build();
	}

}
