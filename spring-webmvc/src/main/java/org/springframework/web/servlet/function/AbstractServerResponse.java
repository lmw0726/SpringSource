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

package org.springframework.web.servlet.function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@link ServerResponse} 实现的抽象基类。
 * <p>
 * 抽象基类提供了一些基本功能和属性，供具体的 {@link ServerResponse} 实现类继承和扩展。
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
abstract class AbstractServerResponse extends ErrorHandlingServerResponse {

	/**
	 * 安全的Http方法（GET、HEAD）
	 */
	private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

	/**
	 * 状态码
	 */
	final int statusCode;

	/**
	 * 响应标头
	 */
	private final HttpHeaders headers;

	/**
	 * Cookie
	 */
	private final MultiValueMap<String, Cookie> cookies;

	protected AbstractServerResponse(
			int statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies) {

		this.statusCode = statusCode;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		this.cookies =
				CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
	}

	@Override
	public final HttpStatus statusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	@Override
	public int rawStatusCode() {
		return this.statusCode;
	}

	@Override
	public final HttpHeaders headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return this.cookies;
	}

	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response,
								Context context) throws ServletException, IOException {

		try {
			// 写入状态码和响应头
			writeStatusAndHeaders(response);

			// 获取最后修改时间
			long lastModified = headers().getLastModified();
			// 创建 ServletWebRequest 对象
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);
			// 解析 HTTP 方法
			HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
			// 检查是否为安全方法且未修改
			if (SAFE_METHODS.contains(httpMethod) &&
					servletWebRequest.checkNotModified(headers().getETag(), lastModified)) {
				// 如果未修改，返回 null
				return null;
			} else {
				// 否则写入内部内容
				return writeToInternal(request, response, context);
			}
		} catch (Throwable throwable) {
			// 处理异常
			return handleError(throwable, request, response, context);
		}
	}

	private void writeStatusAndHeaders(HttpServletResponse response) {
		// 设置响应状态码
		response.setStatus(this.statusCode);
		// 写入响应头
		writeHeaders(response);
		// 写入响应 cookies
		writeCookies(response);
	}

	private void writeHeaders(HttpServletResponse servletResponse) {
		// 将所有响应头写入 HttpServletResponse
		// 遍历 headers 中的每个键值对
		this.headers.forEach((headerName, headerValues) -> {
			// 遍历当前键对应的值列表
			for (String headerValue : headerValues) {
				// 将当前值作为响应头添加到 servletResponse 中
				servletResponse.addHeader(headerName, headerValue);
			}
		});

		// 如果 HttpServletResponse 中未设置 Content-Type，并且响应头中设置了 Content-Type
		if (servletResponse.getContentType() == null && this.headers.getContentType() != null) {
			// 设置 Content-Type
			servletResponse.setContentType(this.headers.getContentType().toString());
		}
		// 如果 HttpServletResponse 中未设置字符编码
		// 并且响应头中设置了 Content-Type 及其字符编码
		if (servletResponse.getCharacterEncoding() == null &&
				this.headers.getContentType() != null &&
				this.headers.getContentType().getCharset() != null) {
			// 则设置字符编码
			servletResponse.setCharacterEncoding(this.headers.getContentType().getCharset().name());
		}
	}

	private void writeCookies(HttpServletResponse servletResponse) {
		// 将 cookies 中的值进行流式处理
		this.cookies.values().stream()
				// 将每个值列表展开为单个值的流
				.flatMap(Collection::stream)
				// 将每个值作为 cookie 添加到 servletResponse 中
				.forEach(servletResponse::addCookie);
	}

	@Nullable
	protected abstract ModelAndView writeToInternal(
			HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException;

}
