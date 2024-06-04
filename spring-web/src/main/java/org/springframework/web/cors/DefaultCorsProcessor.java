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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link CorsProcessor} 的默认实现，如 <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a> 中定义。
 *
 * <p>注意，当输入的 {@link CorsConfiguration} 为 {@code null} 时，此实现不会直接拒绝简单或实际请求，
 * 而是仅避免向响应添加 CORS 头。如果响应已经包含 CORS 头，也会跳过 CORS 处理。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultCorsProcessor implements CorsProcessor {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);


	@Override
	@SuppressWarnings("resource")
	public boolean processRequest(@Nullable CorsConfiguration config, HttpServletRequest request,
								  HttpServletResponse response) throws IOException {

		// 获取响应头中 "Vary" 头的集合
		Collection<String> varyHeaders = response.getHeaders(HttpHeaders.VARY);

		// 如果 "Vary" 头中不包含 "Origin"，则添加 "Origin"
		if (!varyHeaders.contains(HttpHeaders.ORIGIN)) {
			response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
		}

		// 如果 "Vary" 头中不包含 "Access-Control-Request-Method"，则添加 "Access-Control-Request-Method"
		if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)) {
			response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		}

		// 如果 "Vary" 头中不包含 "Access-Control-Request-Headers"，则添加 "Access-Control-Request-Headers"
		if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)) {
			response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		}

		if (!CorsUtils.isCorsRequest(request)) {
			// 如果不是 CORS 请求，则直接返回 true
			return true;
		}

		if (response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
			// 如果响应头中已经包含 "Access-Control-Allow-Origin"
			// 记录日志并返回 true
			logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
			return true;
		}

		// 判断是否为预检请求
		boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);

		// 如果配置为 null
		if (config == null) {
			// 如果是预检请求，则拒绝请求并返回 false
			if (preFlightRequest) {
				rejectRequest(new ServletServerHttpResponse(response));
				return false;
			} else {
				// 否则直接返回 true
				return true;
			}
		}

		// 处理 CORS 请求并返回结果
		return handleInternal(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response), config, preFlightRequest);
	}

	/**
	 * 在 CORS 检查失败时调用。
	 * 默认实现将响应状态设置为 403 并向响应写入 "Invalid CORS request"。
	 */
	protected void rejectRequest(ServerHttpResponse response) throws IOException {
		// 设置响应状态码为 403 Forbidden
		response.setStatusCode(HttpStatus.FORBIDDEN);

		// 向响应体写入 "Invalid CORS request" 的字节数据，使用 UTF-8 编码
		response.getBody().write("Invalid CORS request".getBytes(StandardCharsets.UTF_8));

		// 刷新响应输出流，确保所有数据被发送
		response.flush();
	}

	/**
	 * 处理给定的请求。
	 */
	protected boolean handleInternal(ServerHttpRequest request, ServerHttpResponse response,
									 CorsConfiguration config, boolean preFlightRequest) throws IOException {

		// 获取请求中的 Origin 头信息
		String requestOrigin = request.getHeaders().getOrigin();
		// 检查请求的 Origin 是否被允许，并获取允许的 Origin
		String allowOrigin = checkOrigin(config, requestOrigin);
		// 获取响应头信息
		HttpHeaders responseHeaders = response.getHeaders();

		// 如果允许的 Origin 为空，拒绝请求
		if (allowOrigin == null) {
			logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
			rejectRequest(response);
			return false;
		}

		// 获取要使用的 HTTP 方法
		HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
		// 检查配置中是否允许该方法
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
		// 如果方法不被允许，拒绝请求
		if (allowMethods == null) {
			logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
			rejectRequest(response);
			return false;
		}

		// 获取请求中的头信息
		List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
		// 检查配置中是否允许这些头信息
		List<String> allowHeaders = checkHeaders(config, requestHeaders);
		// 如果是预检请求且头信息不被允许，拒绝请求
		if (preFlightRequest && allowHeaders == null) {
			logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
			rejectRequest(response);
			return false;
		}

		// 设置响应头 Access-Control-Allow-Origin
		responseHeaders.setAccessControlAllowOrigin(allowOrigin);

		// 如果是预检请求，设置响应头 Access-Control-Allow-Methods
		if (preFlightRequest) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		// 如果是预检请求且允许的头信息不为空，设置响应头 Access-Control-Allow-Headers
		if (preFlightRequest && !allowHeaders.isEmpty()) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		// 如果配置中有要暴露的头信息，设置响应头 Access-Control-Expose-Headers
		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		// 如果配置允许凭据，设置响应头 Access-Control-Allow-Credentials
		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		// 如果是预检请求且配置中有最大存活时间，设置响应头 Access-Control-Max-Age
		if (preFlightRequest && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		// 刷新响应输出流，确保所有数据被发送
		response.flush();

		// 返回 true 表示处理成功
		return true;
	}

	/**
	 * 检查来源并确定响应的来源。
	 * 默认实现只是委托给 {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}。
	 */
	@Nullable
	protected String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * 检查 HTTP 方法并确定预检请求响应的方法。
	 * 默认实现只是委托给 {@link org.springframework.web.cors.CorsConfiguration#checkHttpMethod(HttpMethod)}。
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
	 * 检查头信息并确定预检请求响应的头信息。
	 * 默认实现只是委托给 {@link org.springframework.web.cors.CorsConfiguration#checkHeaders(List)}。
	 */
	@Nullable
	protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		// 获取请求头信息
		HttpHeaders headers = request.getHeaders();

		// 如果是预检请求，返回请求头中的 Access-Control-Request-Headers
		// 否则，返回请求头中所有键的集合
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<>(headers.keySet()));
	}

}
