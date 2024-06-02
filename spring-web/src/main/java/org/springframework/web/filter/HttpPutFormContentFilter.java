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

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * {@link javax.servlet.Filter} 用于在HTTP PUT或PATCH请求期间通过
 * {@code ServletRequest.getParameter*()} 方法系列使表单编码的数据可用。
 *
 * <p>Servlet规范要求表单数据对于HTTP POST可用，但对于HTTP PUT或PATCH请求则不需要。
 * 此过滤器拦截内容类型为{@code 'application/x-www-form-urlencoded'}的HTTP PUT和PATCH请求，
 * 从请求体中读取表单编码的内容，并包装ServletRequest，以使表单数据像HTTP POST请求一样
 * 可用作请求参数。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @deprecated 从5.1开始，不推荐使用此类，建议使用{@link FormContentFilter}，该类功能相同，
 * 但也处理DELETE请求。
 */
@Deprecated
public class HttpPutFormContentFilter extends OncePerRequestFilter {
	/**
	 * 用于解析表单内容的转换器
	 */
	private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();


	/**
	 * 设置用于解析表单内容的转换器。
	 * <p>默认情况下，这是一个{@link AllEncompassingFormHttpMessageConverter}实例。
	 */
	public void setFormConverter(FormHttpMessageConverter converter) {
		Assert.notNull(converter, "FormHttpMessageConverter is required.");
		this.formConverter = converter;
	}

	public FormHttpMessageConverter getFormConverter() {
		return this.formConverter;
	}

	/**
	 * 用于读取表单数据的默认字符集。
	 * 这是一个快捷方式：<br>
	 * {@code getFormConverter.setCharset(charset)}。
	 */
	public void setCharset(Charset charset) {
		this.formConverter.setCharset(charset);
	}


	@Override
	protected void doFilterInternal(final HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		// 如果请求方法是 PUT 或 PATCH 并且内容类型是表单
		if (("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) && isFormContentType(request)) {
			// 创建一个 HttpInputMessage 实例，用于读取请求的输入流
			HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
				@Override
				public InputStream getBody() throws IOException {
					return request.getInputStream();
				}
			};
			// 使用 表单内容转换器 读取表单参数
			MultiValueMap<String, String> formParameters = this.formConverter.read(null, inputMessage);
			// 如果表单参数不为空
			if (!formParameters.isEmpty()) {
				// 创建一个新的 HttpServletRequest 包装器，用于处理 PUT 表单内容
				HttpServletRequest wrapper = new HttpPutFormContentRequestWrapper(request, formParameters);
				// 调用过滤器链的下一个过滤器，使用新的请求包装器和原始响应对象
				filterChain.doFilter(wrapper, response);
				// 结束方法执行
				return;
			}
		}

		// 如果请求方法不是 PUT 或 PATCH，或者内容类型不是表单，直接调用过滤器链的下一个过滤器
		filterChain.doFilter(request, response);
	}

	private boolean isFormContentType(HttpServletRequest request) {
		// 获取请求的内容类型
		String contentType = request.getContentType();

		// 如果内容类型不为空
		if (contentType != null) {
			try {
				// 解析内容类型为 MediaType 对象
				MediaType mediaType = MediaType.parseMediaType(contentType);
				// 检查内容类型是否包括 application/x-www-form-urlencoded 类型
				return (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType));
			} catch (IllegalArgumentException ex) {
				// 如果解析内容类型时发生异常，返回 false
				return false;
			}
		} else {
			// 如果内容类型为空，返回 false
			return false;
		}
	}


	private static class HttpPutFormContentRequestWrapper extends HttpServletRequestWrapper {
		/**
		 * 表单参数
		 */
		private MultiValueMap<String, String> formParameters;

		public HttpPutFormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> parameters) {
			super(request);
			this.formParameters = parameters;
		}

		@Override
		@Nullable
		public String getParameter(String name) {
			// 从查询字符串中获取参数值
			String queryStringValue = super.getParameter(name);
			// 从表单参数中获取参数值
			String formValue = this.formParameters.getFirst(name);
			// 如果查询字符串中的值不为空，返回查询字符串中的值，否则返回表单参数中的值
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			// 创建一个有序的 Map 来存储结果
			Map<String, String[]> result = new LinkedHashMap<>();

			// 获取所有参数名称的枚举
			Enumeration<String> names = getParameterNames();

			// 遍历所有参数名称
			while (names.hasMoreElements()) {
				// 获取下一个参数名称
				String name = names.nextElement();
				// 将参数名称及其对应的值数组放入结果 Map 中
				result.put(name, getParameterValues(name));
			}

			// 返回结果 Map
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			// 创建一个有序的 Set 来存储所有参数名称
			Set<String> names = new LinkedHashSet<>();

			// 将查询字符串中的参数名称添加到 Set 中
			names.addAll(Collections.list(super.getParameterNames()));

			// 将表单参数中的参数名称添加到 Set 中
			names.addAll(this.formParameters.keySet());

			// 返回一个包含所有参数名称的枚举
			return Collections.enumeration(names);
		}

		@Override
		@Nullable
		public String[] getParameterValues(String name) {
			// 从查询字符串中获取参数值数组
			String[] parameterValues = super.getParameterValues(name);

			// 从表单参数中获取参数值列表
			List<String> formParam = this.formParameters.get(name);

			// 如果表单参数中没有相应的参数值，直接返回查询字符串中的参数值数组
			if (formParam == null) {
				return parameterValues;
			}

			// 如果查询字符串中的参数值数组为空或者查询字符串为空，返回表单参数值数组
			if (parameterValues == null || getQueryString() == null) {
				return StringUtils.toStringArray(formParam);
			} else {
				// 合并查询字符串中的参数值和表单参数中的参数值
				List<String> result = new ArrayList<>(parameterValues.length + formParam.size());
				// 将查询字符串中的参数值添加到结果列表中
				result.addAll(Arrays.asList(parameterValues));
				// 将表单参数中的参数值添加到结果列表中
				result.addAll(formParam);
				// 返回合并后的参数值数组
				return StringUtils.toStringArray(result);
			}
		}
	}

}
