/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.cors.reactive;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 基于<a href="https://www.w3.org/TR/cors/">CORS W3C建议</a>的CORS响应式请求处理的实用工具类。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class CorsUtils {

	/**
	 * 通过检查{@code Origin}头的存在并通过{@link #isSameOrigin}确保来源不同来返回{@code true}，
	 * 如果请求是有效的CORS请求，则返回{@code true}。
	 */
	@SuppressWarnings("deprecation")
	public static boolean isCorsRequest(ServerHttpRequest request) {
		return request.getHeaders().containsKey(HttpHeaders.ORIGIN) && !isSameOrigin(request);
	}

	/**
	 * 通过检查{@code OPTIONS}方法与{@code Origin}和{@code Access-Control-Request-Method}头的存在来返回{@code true}，
	 * 如果请求是有效的CORS预检请求，则返回{@code true}。
	 */
	public static boolean isPreFlightRequest(ServerHttpRequest request) {
		// 获取请求头信息
		HttpHeaders headers = request.getHeaders();
		// 如果请求方法为OPTIONS，并且请求头包含ORIGIN和ACCESS_CONTROL_REQUEST_METHOD，则返回true，否则返回false
		return (request.getMethod() == HttpMethod.OPTIONS
				&& headers.containsKey(HttpHeaders.ORIGIN)
				&& headers.containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * 根据{@code Origin}和{@code Host}头检查请求是否是同源的。
	 *
	 * <p><strong>注意：</strong>从5.1开始，此方法忽略指定客户端发起的地址的{@code "Forwarded"}和{@code "X-Forwarded-*"}头。
	 * 考虑使用{@code ForwardedHeaderFilter}来提取和使用或丢弃这些头。
	 *
	 * @return 如果请求是同源的，则返回{@code true}；如果是跨源请求，则返回{@code false}
	 * @deprecated 从5.2开始，同源检查由{@link #isCorsRequest}直接执行
	 */
	@Deprecated
	public static boolean isSameOrigin(ServerHttpRequest request) {
		// 获取请求的 来源 头信息
		String origin = request.getHeaders().getOrigin();
		// 如果 来源 头信息为空，返回 true
		if (origin == null) {
			return true;
		}

		// 获取请求的 URI
		URI uri = request.getURI();
		// 获取请求的实际  方案 、主机地址、端口号
		String actualScheme = uri.getScheme();
		String actualHost = uri.getHost();
		int actualPort = getPort(uri.getScheme(), uri.getPort());
		// 断言实际请求的  方案 、主机地址、端口号 不为空，且端口不能为未定义
		Assert.notNull(actualScheme, "Actual request scheme must not be null");
		Assert.notNull(actualHost, "Actual request host must not be null");
		Assert.isTrue(actualPort != -1, "Actual request port must not be undefined");

		// 从 来源 头信息中构建 UriComponents
		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
		// 比较实际的  方案 、主机地址、端口号 是否与  来源  头信息中的相匹配，若匹配则返回 true，否则返回 false
		return (actualScheme.equals(originUrl.getScheme()) &&
				actualHost.equals(originUrl.getHost()) &&
				actualPort == getPort(originUrl.getScheme(), originUrl.getPort()));
	}

	private static int getPort(@Nullable String scheme, int port) {
		// 如果端口为未定义（-1）
		if (port == -1) {
			// 如果是 HTTP 或 WebSocket 请求，将端口设置为 80
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			}
			// 如果是 HTTPS 或 Secure WebSocket 请求，将端口设置为 443
			else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		// 返回确定的端口值
		return port;
	}

}
