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

package org.springframework.http.client.reactive;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jetty ReactiveStreams HTTP 客户端的 {@link ClientHttpResponse} 实现。
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 * Jetty ReactiveStreams HttpClient</a>
 * @since 5.1
 */
class JettyClientHttpResponse implements ClientHttpResponse {

	/**
	 * 用于匹配 SameSite 属性的正则表达式模式。
	 */
	private static final Pattern SAMESITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

	/**
	 * JettyClientHttpResponse 类的类加载器。
	 */
	private static final ClassLoader classLoader = JettyClientHttpResponse.class.getClassLoader();

	/**
	 * 指示 Jetty 10 是否存在的布尔值。
	 */
	private static final boolean jetty10Present;


	/**
	 * Jetty 响应的响应式响应。
	 */
	private final ReactiveResponse reactiveResponse;

	/**
	 * Jetty 响应的内容流。
	 */
	private final Flux<DataBuffer> content;

	/**
	 * Jetty 响应的 HTTP 头部。
	 */
	private final HttpHeaders headers;


	static {
		try {
			// 加载 org.eclipse.jetty.http.HttpFields 类
			Class<?> httpFieldsClass = classLoader.loadClass("org.eclipse.jetty.http.HttpFields");
			// 判断是否为 Jetty 10 或更高版本
			jetty10Present = httpFieldsClass.isInterface();
		} catch (ClassNotFoundException ex) {
			// 如果找不到类，则抛出异常
			throw new IllegalStateException("No compatible Jetty version found", ex);
		}
	}


	public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Publisher<DataBuffer> content) {
		// 设置响应对象
		this.reactiveResponse = reactiveResponse;

		// 将内容转换为 Flux 流
		this.content = Flux.from(content);

		// 初始化响应头
		MultiValueMap<String, String> headers = (jetty10Present ?
				// 如果是 Jetty 10 或更高版本，则使用 Jetty10HttpFieldsHelper 获取响应头
				Jetty10HttpFieldsHelper.getHttpHeaders(reactiveResponse) :
				// 否则，使用 JettyHeadersAdapter 获取响应头
				new JettyHeadersAdapter(reactiveResponse.getHeaders()));

		// 创建只读的 HttpHeaders 对象
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.reactiveResponse.getStatus();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		// 创建一个空的 MultiValueMap，用于存储响应 cookie
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();

		// 获取响应头中的 Set-Cookie 值列表
		List<String> cookieHeader = getHeaders().get(HttpHeaders.SET_COOKIE);

		// 如果 Set-Cookie 值列表不为 null，则遍历每个值
		if (cookieHeader != null) {
			cookieHeader.forEach(header ->
					// 解析每个 Set-Cookie 值并将其转换为 ResponseCookie 对象，然后添加到 result 中
					HttpCookie.parse(header).forEach(cookie -> result.add(cookie.getName(),
							ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
									.domain(cookie.getDomain())
									.path(cookie.getPath())
									.maxAge(cookie.getMaxAge())
									.secure(cookie.getSecure())
									.httpOnly(cookie.isHttpOnly())
									.sameSite(parseSameSite(header))
									.build()))
			);
		}

		// 返回只读的 MultiValueMap
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Nullable
	private static String parseSameSite(String headerValue) {
		// 使用正则表达式匹配 SameSite 属性值
		Matcher matcher = SAMESITE_PATTERN.matcher(headerValue);

		// 如果匹配成功，则返回匹配的组的值，否则返回 null
		return (matcher.matches() ? matcher.group(1) : null);
	}


	@Override
	public Flux<DataBuffer> getBody() {
		return this.content;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}


	private static class Jetty10HttpFieldsHelper {

		/**
		 * 用于反射获取头部信息的方法。
		 */
		private static final Method getHeadersMethod;

		/**
		 * 用于反射获取名称信息的方法。
		 */
		private static final Method getNameMethod;

		/**
		 * 用于反射获取值信息的方法。
		 */
		private static final Method getValueMethod;

		static {
			try {
				// 获取 Response 类中的 getHeaders 方法
				getHeadersMethod = Response.class.getMethod("getHeaders");
				// 加载 HttpField 类
				Class<?> type = classLoader.loadClass("org.eclipse.jetty.http.HttpField");
				// 获取 HttpField 类中的 getName 和 getValue 方法
				getNameMethod = type.getMethod("getName");
				getValueMethod = type.getMethod("getValue");
			} catch (ClassNotFoundException | NoSuchMethodException ex) {
				// 捕获 ClassNotFoundException 和 NoSuchMethodException 异常，抛出 IllegalStateException
				throw new IllegalStateException("No compatible Jetty version found", ex);
			}
		}

		public static HttpHeaders getHttpHeaders(ReactiveResponse response) {
			// 创建一个新的 HttpHeaders 对象
			HttpHeaders headers = new HttpHeaders();
			// 调用 Response 对象的 getHeaders 方法获取响应头的迭代器
			Iterable<?> iterator = (Iterable<?>)
					ReflectionUtils.invokeMethod(getHeadersMethod, response.getResponse());
			// 断言迭代器不为空
			Assert.notNull(iterator, "Iterator must not be null");
			// 遍历迭代器，解析每个响应头字段并添加到 HttpHeaders 对象中
			for (Object field : iterator) {
				// 获取响应头字段的名称
				String name = (String) ReflectionUtils.invokeMethod(getNameMethod, field);
				// 断言字段名称不为空
				Assert.notNull(name, "Header name must not be null");
				// 获取响应头字段的值
				String value = (String) ReflectionUtils.invokeMethod(getValueMethod, field);
				// 将响应头字段添加到 HttpHeaders 对象中
				headers.add(name, value);
			}
			// 返回解析后的 HttpHeaders 对象
			return headers;
		}
	}

}
