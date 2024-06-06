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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link CorsProcessor} 的默认实现，由<a href="https://www.w3.org/TR/cors/">CORS W3C推荐</a>定义。
 *
 * <p>注意，当输入的{@link CorsConfiguration}为{@code null}时，此实现不会直接拒绝简单请求或实际请求，
 * 而是简单地避免向响应添加CORS头。如果响应已经包含CORS头，CORS处理也将被跳过。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultCorsProcessor implements CorsProcessor {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);

	/**
	 * 可变请求头字符串
	 */
	private static final List<String> VARY_HEADERS = Arrays.asList(
			HttpHeaders.ORIGIN, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);


	@Override
	public boolean process(@Nullable CorsConfiguration config, ServerWebExchange exchange) {

		// 获取请求和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		// 获取响应头信息
		HttpHeaders responseHeaders = response.getHeaders();

		// 获取响应中已有的 Vary 头信息列表
		List<String> varyHeaders = responseHeaders.get(HttpHeaders.VARY);
		// 如果 Vary 头信息为空，则添加 Vary 头信息列表
		if (varyHeaders == null) {
			responseHeaders.addAll(HttpHeaders.VARY, VARY_HEADERS);
		} else {
			// 遍历预定义的 Vary 头信息列表
			for (String header : VARY_HEADERS) {
				if (!varyHeaders.contains(header)) {
					// 如果响应头中没有该 Vary 头，则添加
					responseHeaders.add(HttpHeaders.VARY, header);
				}
			}
		}

		// 如果不是 CORS 请求，则直接返回 true
		if (!CorsUtils.isCorsRequest(request)) {
			return true;
		}

		// 如果响应头中已包含 Access-Control-Allow-Origin 头，则跳过
		if (responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
			logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
			return true;
		}

		// 判断是否为预检请求
		boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);
		// 如果配置为空
		if (config == null) {
			// 如果是预检请求，则拒绝请求；
			if (preFlightRequest) {
				rejectRequest(response);
				return false;
			} else {
				// 否则返回 true
				return true;
			}
		}

		// 处理 CORS 请求
		return handleInternal(exchange, config, preFlightRequest);
	}

	/**
	 * 当其中一个跨域检查失败时调用
	 */
	protected void rejectRequest(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.FORBIDDEN);
	}

	/**
	 * 处理给定请求
	 */
	protected boolean handleInternal(ServerWebExchange exchange,
									 CorsConfiguration config, boolean preFlightRequest) {

		// 获取请求对象和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		// 获取响应头
		HttpHeaders responseHeaders = response.getHeaders();

		// 获取请求的 来源 头信息
		String requestOrigin = request.getHeaders().getOrigin();
		// 检查 来源 是否允许
		String allowOrigin = checkOrigin(config, requestOrigin);
		// 如果 来源 不允许，则拒绝请求，并返回 false
		if (allowOrigin == null) {
			logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
			rejectRequest(response);
			return false;
		}

		// 获取请求的方法，以确定是否允许
		HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
		// 检查请求的方法是否允许，如果不允许，则拒绝请求，并返回 false
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
		if (allowMethods == null) {
			logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
			rejectRequest(response);
			return false;
		}

		// 获取请求的头信息，以确定是否允许
		List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
		// 检查请求的头信息是否允许，如果不允许且为预检请求，则拒绝请求，并返回 false
		List<String> allowHeaders = checkHeaders(config, requestHeaders);
		if (preFlightRequest && allowHeaders == null) {
			logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
			rejectRequest(response);
			return false;
		}

		// 设置响应头信息，允许的 Origin
		responseHeaders.setAccessControlAllowOrigin(allowOrigin);

		// 如果为预检请求，则设置允许的方法
		if (preFlightRequest) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		// 如果为预检请求且有允许的头信息，则设置允许的头信息
		if (preFlightRequest && !allowHeaders.isEmpty()) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		// 如果配置中有暴露的头信息，则设置响应头中的暴露头信息
		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		// 如果允许携带凭证，则设置响应头中允许携带凭证
		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		// 如果为预检请求且配置了最大缓存时间，则设置响应头中的最大缓存时间
		if (preFlightRequest && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		// 返回 true，表示请求通过了 CORS 检查
		return true;
	}

	/**
	 * 检查源并确定响应的来源。默认实现简单地委托给{@link CorsConfiguration#checkOrigin(String)}。
	 *
	 * @param config        CORS配置
	 * @param requestOrigin 请求的源
	 * @return 响应的源，如果未找到匹配的源则返回{@code null}
	 */
	@Nullable
	protected String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * 检查HTTP方法并确定预检请求的响应方法。默认实现简单地委托给{@link CorsConfiguration#checkHttpMethod(HttpMethod)}。
	 *
	 * @param config        CORS配置
	 * @param requestMethod 请求的方法
	 * @return 预检请求的响应方法列表，如果未找到匹配的方法则返回{@code null}
	 */
	@Nullable
	protected List<HttpMethod> checkMethods(CorsConfiguration config, @Nullable HttpMethod requestMethod) {
		return config.checkHttpMethod(requestMethod);
	}

	@Nullable
	private HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
		return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
	}

	/**
	 * 检查头信息并确定预检请求的响应头。默认实现简单地委托给{@link CorsConfiguration#checkHeaders(List)}。
	 *
	 * @param config         CORS配置
	 * @param requestHeaders 请求的头信息列表
	 * @return 预检请求的响应头信息列表，如果未找到匹配的头信息则返回{@code null}
	 */
	@Nullable
	protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		// 获取请求头信息
		HttpHeaders headers = request.getHeaders();
		// 如果为预检请求，则返回 Access-Control-Request-Headers 头信息，否则返回全部头信息的键集合
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<>(headers.keySet()));
	}

}
