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

package org.springframework.web.cors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * 基于 <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a> 的实用类，用于处理 CORS 请求。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public abstract class CorsUtils {

	/**
	 * 如果通过检查 {@code Origin} 头的存在并确保源不同，返回 {@code true} 表示请求是有效的 CORS 请求。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 如果是有效的 CORS 请求则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean isCorsRequest(HttpServletRequest request) {
		// 从请求头中获取 Origin 头的值
		String origin = request.getHeader(HttpHeaders.ORIGIN);

		// 如果 Origin 头为空，则返回 false
		if (origin == null) {
			return false;
		}

		// 使用 UriComponentsBuilder 解析 Origin 头中的 URL
		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();

		// 获取请求的方案（http 或 https）
		String scheme = request.getScheme();

		// 获取请求的主机名
		String host = request.getServerName();

		// 获取请求的端口号
		int port = request.getServerPort();

		// 判断请求的方案、主机名和端口号是否与 Origin 头中的相同，如果不相同，则返回 true；
		// 否则返回 false
		return !(ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme()) &&
				ObjectUtils.nullSafeEquals(host, originUrl.getHost()) &&
				getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));

	}

	/**
	 * 返回根据给定的方案和端口确定的端口号。如果端口为 -1，则根据方案返回默认端口。
	 *
	 * @param scheme 当前的协议方案
	 * @param port   当前的端口号
	 * @return 确定的端口号
	 */
	private static int getPort(@Nullable String scheme, int port) {
		// 如果端口号为 -1（未指定端口号）
		if (port == -1) {
			// 如果方案是 "http" 或 "ws"，设置端口号为 80
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			} else if ("https".equals(scheme) || "wss".equals(scheme)) {
				// 如果方案是 "https" 或 "wss"，设置端口号为 443
				port = 443;
			}
		}
		// 返回端口号
		return port;
	}

	/**
	 * 如果通过检查 {code OPTIONS} 方法和 {@code Origin} 及 {@code Access-Control-Request-Method} 头的存在，返回 {@code true} 表示请求是有效的 CORS 预检请求。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 如果是有效的 CORS 预检请求则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean isPreFlightRequest(HttpServletRequest request) {
		// 判断是否为 HTTP OPTIONS 方法
		return (HttpMethod.OPTIONS.matches(request.getMethod()) &&
				// 判断请求头中是否包含 Origin 头信息
				request.getHeader(HttpHeaders.ORIGIN) != null &&
				// 判断请求头中是否包含 Access-Control-Request-Method 头信息
				request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null);
	}

}
