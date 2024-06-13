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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ServerHttpRequest} 实现的公共基类。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

	/**
	 * 用于解析查询字符串的正则表达式模式。
	 */
	private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	/**
	 * 请求的 URI。
	 */
	private final URI uri;

	/**
	 * 请求路径的结构化表示。
	 */
	private final RequestPath path;

	/**
	 * 请求的头信息。
	 */
	private final HttpHeaders headers;

	/**
	 * 查询参数的多值映射。
	 */
	@Nullable
	private MultiValueMap<String, String> queryParams;

	/**
	 * 客户端发送的 Cookie 的多值映射。
	 */
	@Nullable
	private MultiValueMap<String, HttpCookie> cookies;

	/**
	 * SSL 会话信息。
	 */
	@Nullable
	private SslInfo sslInfo;

	/**
	 * 请求的唯一标识符。
	 */
	@Nullable
	private String id;

	/**
	 * 用于记录日志的前缀。
	 */
	@Nullable
	private String logPrefix;


	/**
	 * 构造函数，包含请求的 URI 和头信息。
	 *
	 * @param uri         请求的 URI
	 * @param contextPath 请求的上下文路径
	 * @param headers     请求的头信息（作为 {@link MultiValueMap}）
	 * @since 5.3
	 */
	public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, MultiValueMap<String, String> headers) {
		this.uri = uri;
		// 解析请求路径
		this.path = RequestPath.parse(uri, contextPath);
		// 返回只读请求头
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}

	/**
	 * 构造函数，包含请求的 URI 和头信息。
	 *
	 * @param uri         请求的 URI
	 * @param contextPath 请求的上下文路径
	 * @param headers     请求的头信息（作为 {@link HttpHeaders}）
	 */
	public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, HttpHeaders headers) {
		this.uri = uri;
		// 解析请求路径
		this.path = RequestPath.parse(uri, contextPath);
		// 返回只读请求头
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public String getId() {
		// 如果当前对象的id为null
		if (this.id == null) {
			// 初始化id
			this.id = initId();
			// 如果初始化的id仍然为null
			if (this.id == null) {
				// 使用ObjectUtils获取该对象的十六进制标识字符串作为id
				this.id = ObjectUtils.getIdentityHexString(this);
			}
		}
		// 返回当前对象的id
		return this.id;
	}

	/**
	 * 获取要使用的请求ID，或者返回 {@code null}，在这种情况下，将使用此请求实例的对象标识。
	 *
	 * @since 5.1
	 */
	@Nullable
	protected String initId() {
		return null;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public RequestPath getPath() {
		return this.path;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		// 如果查询参数未初始化
		if (this.queryParams == null) {
			// 初始化查询参数，并将其存储在一个不可修改的MultiValueMap中
			this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
		}
		// 返回查询参数
		return this.queryParams;
	}

	/**
	 * 用于将查询解析为名称-值对的方法。返回值将转换为不可变映射并缓存。
	 * <p>请注意，此方法是在第一次访问 {@link #getQueryParams()} 时延迟调用的。调用不是同步的，但解析是线程安全的。
	 */
	protected MultiValueMap<String, String> initQueryParams() {
		// 创建一个LinkedMultiValueMap来存储查询参数
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		// 获取URI中的原始查询字符串
		String query = getURI().getRawQuery();

		// 如果查询字符串不为空
		if (query != null) {
			// 使用QUERY_PATTERN模式匹配查询字符串中的参数
			Matcher matcher = QUERY_PATTERN.matcher(query);
			// 遍历所有匹配的查询参数
			while (matcher.find()) {
				// 解码查询参数的名称
				String name = decodeQueryParam(matcher.group(1));
				// 获取等号字符（如果存在）
				String eq = matcher.group(2);
				// 获取查询参数的值
				String value = matcher.group(3);
				// 如果值不为空，则解码值；如果值为空但存在等号，则将值设为空字符串；否则设为空值
				value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
				// 将解码后的查询参数名称和值添加到MultiValueMap中
				queryParams.add(name, value);
			}
		}
		// 返回包含查询参数的MultiValueMap
		return queryParams;
	}

	@SuppressWarnings("deprecation")
	private String decodeQueryParam(String value) {
		try {
			// 尝试使用UTF-8编码格式解码值
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			// 如果抛出UnsupportedEncodingException异常
			return URLDecoder.decode(value);
		}
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		// 如果Cookies未初始化
		if (this.cookies == null) {
			// 初始化Cookies，并将其存储在一个不可修改的MultiValueMap中
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(initCookies());
		}

		// 返回Cookies
		return this.cookies;
	}

	/**
	 * 从底层的“原生”请求中获取 cookie 并将其适配为 {@link HttpCookie} 映射。返回值将转换为不可变映射并缓存。
	 * <p>请注意，此方法是在访问 {@link #getCookies()} 时延迟调用的。如果底层的“原生”请求不提供线程安全的 cookie 数据访问，
	 * 子类应同步 cookie 初始化。
	 */
	protected abstract MultiValueMap<String, HttpCookie> initCookies();

	@Nullable
	@Override
	public SslInfo getSslInfo() {
		// 如果SSL信息尚未初始化
		if (this.sslInfo == null) {
			// 调用方法初始化SSL信息
			this.sslInfo = initSslInfo();
		}

		// 返回SSL信息
		return this.sslInfo;
	}

	/**
	 * 从底层的“原生”请求中获取 SSL 会话信息。
	 *
	 * @return 会话信息，如果没有则返回 {@code null}
	 * @since 5.0.2
	 */
	@Nullable
	protected abstract SslInfo initSslInfo();

	/**
	 * 返回底层的服务器响应。
	 * <p><strong>注意：</strong>这主要用于内部框架使用，如 spring-webflux 模块中的 WebSocket 升级。
	 */
	public abstract <T> T getNativeRequest();

	/**
	 * 用于在 HTTP 适配器层进行日志记录的内部使用。
	 *
	 * @since 5.1
	 */
	String getLogPrefix() {
		// 如果日志前缀尚未初始化
		if (this.logPrefix == null) {
			// 调用方法初始化日志前缀，并用初始化的结果包装在方括号中赋给logPrefix
			this.logPrefix = "[" + initLogPrefix() + "] ";
		}

		// 返回日志前缀
		return this.logPrefix;
	}

	/**
	 * 子类可以重写此方法以提供用于日志消息的前缀。
	 * <p>默认情况下，这是 {@link #getId()}。
	 *
	 * @since 5.3.15
	 */
	protected String initLogPrefix() {
		return getId();
	}

}
