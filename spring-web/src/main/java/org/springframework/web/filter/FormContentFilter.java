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
import org.springframework.util.CollectionUtils;
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
 * {@code Filter} 解析 HTTP PUT、PATCH 和 DELETE 请求的表单数据，并将其作为 Servlet 请求参数公开。默认情况下，
 * Servlet 规范仅要求这样做 HTTP POST 请求。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class FormContentFilter extends OncePerRequestFilter {
	/**
	 * Http方法列表
	 */
	private static final List<String> HTTP_METHODS = Arrays.asList("PUT", "PATCH", "DELETE");

	/**
	 * 表单转换器
	 */
	private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();


	/**
	 * 设置用于解析表单内容的转换器。
	 * <p>默认情况下，这是 {@link AllEncompassingFormHttpMessageConverter} 的实例。
	 */
	public void setFormConverter(FormHttpMessageConverter converter) {
		Assert.notNull(converter, "FormHttpMessageConverter is required");
		this.formConverter = converter;
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
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 解析请求参数
		MultiValueMap<String, String> params = parseIfNecessary(request);

		// 如果解析出的参数不为空，则使用包装器对象处理请求
		if (!CollectionUtils.isEmpty(params)) {
			filterChain.doFilter(new FormContentRequestWrapper(request, params), response);
		} else {
			// 如果解析出的参数为空，则直接处理请求
			filterChain.doFilter(request, response);
		}
	}

	@Nullable
	private MultiValueMap<String, String> parseIfNecessary(HttpServletRequest request) throws IOException {
		// 如果请求不应该被解析，则返回 null
		if (!shouldParse(request)) {
			return null;
		}

		// 创建 HttpInputMessage 对象，从请求中获取输入流
		HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
			@Override
			public InputStream getBody() throws IOException {
				return request.getInputStream();
			}
		};

		// 使用表单转换器读取输入消息并返回解析的参数
		return this.formConverter.read(null, inputMessage);
	}

	private boolean shouldParse(HttpServletRequest request) {
		// 获取请求的内容类型
		String contentType = request.getContentType();

		// 获取请求的方法
		String method = request.getMethod();

		// 检查内容类型是否有长度，并且请求方法是否在 Http方法 集合中
		if (StringUtils.hasLength(contentType) && HTTP_METHODS.contains(method)) {
			try {
				// 解析内容类型为 媒体类型 对象
				MediaType mediaType = MediaType.parseMediaType(contentType);

				// 检查是否是 application/x-www-form-urlencoded 类型
				return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
			} catch (IllegalArgumentException ex) {
				// 捕捉非法参数异常，但不做处理
			}
		}

		// 如果内容类型为空，或者请求方法不在 Http方法 集合中，则返回 false
		return false;
	}


	private static class FormContentRequestWrapper extends HttpServletRequestWrapper {
		/**
		 * 表单参数
		 */
		private MultiValueMap<String, String> formParams;

		public FormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> params) {
			super(request);
			this.formParams = params;
		}

		@Override
		@Nullable
		public String getParameter(String name) {
			// 从父类中获取指定参数名的查询字符串值
			String queryStringValue = super.getParameter(name);

			// 从表单参数中获取指定参数名的第一个值
			String formValue = this.formParams.getFirst(name);

			// 如果查询字符串值不为空，则返回查询字符串值；否则返回表单参数值
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			// 创建一个存储结果的 LinkedHashMap 对象，键为字符串，值为字符串数组
			Map<String, String[]> result = new LinkedHashMap<>();

			// 获取所有参数名称的枚举对象
			Enumeration<String> names = getParameterNames();

			// 遍历所有参数名称
			while (names.hasMoreElements()) {
				// 获取当前参数名称
				String name = names.nextElement();

				// 获取当前参数名称对应的所有值，并存储到结果映射中
				result.put(name, getParameterValues(name));
			}

			// 返回结果映射
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			// 创建一个存储参数名称的 LinkedHashSet 对象，保证参数名称的唯一性和顺序
			Set<String> names = new LinkedHashSet<>();

			// 将父类中的参数名称添加到 名称 集合中
			names.addAll(Collections.list(super.getParameterNames()));

			// 将表单参数的名称添加到 名称 集合中
			names.addAll(this.formParams.keySet());

			// 将 名称 集合转换为枚举对象并返回
			return Collections.enumeration(names);
		}

		@Override
		@Nullable
		public String[] getParameterValues(String name) {
			// 从父类获取指定参数名称的参数值数组
			String[] parameterValues = super.getParameterValues(name);

			// 从表单参数中获取指定参数名称的参数值列表
			List<String> formParam = this.formParams.get(name);

			// 如果表单参数值为 null，直接返回父类的参数值数组
			if (formParam == null) {
				return parameterValues;
			}

			// 如果父类的参数值数组为 null 或者查询字符串为 null
			if (parameterValues == null || getQueryString() == null) {
				// 将表单参数值列表转换为数组并返回
				return StringUtils.toStringArray(formParam);
			} else {
				// 创建一个新的列表用于存储合并后的参数值
				List<String> result = new ArrayList<>(parameterValues.length + formParam.size());

				// 将父类的参数值数组添加到结果列表中
				result.addAll(Arrays.asList(parameterValues));

				// 将表单参数值列表添加到结果列表中
				result.addAll(formParam);

				// 将结果列表转换为数组并返回
				return StringUtils.toStringArray(result);
			}
		}
	}

}
