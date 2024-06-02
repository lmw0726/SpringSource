/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.filter;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@link javax.servlet.Filter}，用于将发布的方法参数转换为 HTTP 方法，
 * 可通过 {@link HttpServletRequest#getMethod()} 检索。由于当前浏览器仅支持 GET 和 POST，
 * 一个常见的技术（例如，Prototype 库使用的技术）是使用一个带有额外隐藏表单字段（{@code _method}）的普通 POST 来传递“真实”的 HTTP 方法。
 * 此过滤器读取该参数并相应地更改 {@link HttpServletRequestWrapper#getMethod()} 的返回值。
 * 仅允许 {@code "PUT"}、{@code "DELETE"} 和 {@code "PATCH"} HTTP 方法。
 *
 * <p>请求参数的名称默认为 {@code _method}，但可以通过 {@link #setMethodParam(String) methodParam} 属性进行调整。
 *
 * <p><b>注意：在多部分 POST 请求的情况下，此过滤器需要在多部分处理之后运行，因为它固有地需要检查 POST 主体参数。</b>
 * 因此，通常将一个 Spring {@link org.springframework.web.multipart.support.MultipartFilter} 放置在 {@code web.xml} 过滤器链中的此 HiddenHttpMethodFilter <i>之前</i>。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class HiddenHttpMethodFilter extends OncePerRequestFilter {
	/**
	 * 允许的方法，PUT、DELETE、PATCH
	 */
	private static final List<String> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT.name(),
					HttpMethod.DELETE.name(), HttpMethod.PATCH.name()));

	/**
	 * 默认方法参数：{@code _method}。
	 */
	public static final String DEFAULT_METHOD_PARAM = "_method";

	/**
	 * 方法参数名称
	 */
	private String methodParam = DEFAULT_METHOD_PARAM;


	/**
	 * 设置要查找 HTTP 方法的参数名称。
	 *
	 * @see #DEFAULT_METHOD_PARAM
	 */
	public void setMethodParam(String methodParam) {
		Assert.hasText(methodParam, "'methodParam' must not be empty");
		this.methodParam = methodParam;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 将请求对象赋值给 要使用的请求对象
		HttpServletRequest requestToUse = request;

		// 如果请求方法是 "POST" 并且请求属性中不存在错误异常属性
		if ("POST".equals(request.getMethod()) && request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
			// 获取请求参数 方法参数 的值
			String paramValue = request.getParameter(this.methodParam);
			// 如果值不为空
			if (StringUtils.hasLength(paramValue)) {
				// 将值转换为大写字母形式
				String method = paramValue.toUpperCase(Locale.ENGLISH);
				// 如果转换后的值在允许的方法列表中
				if (ALLOWED_METHODS.contains(method)) {
					// 创建一个新的 HttpMethodRequestWrapper 对象，并将请求方法修改为该值
					requestToUse = new HttpMethodRequestWrapper(request, method);
				}
			}
		}

		// 调用过滤器链的下一个过滤器，传递修改后的请求对象和响应对象
		filterChain.doFilter(requestToUse, response);
	}


	/**
	 * 简单的{@link HttpServletRequest}包装器，为{@link HttpServletRequest#getMethod()}返回提供的方法。
	 */
	private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

		/**
		 * 方法名称
		 */
		private final String method;

		public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
			super(request);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return this.method;
		}
	}

}
