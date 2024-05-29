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

package org.springframework.web.multipart.support;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest} 接口的默认实现。
 * 提供预生成参数值的管理。
 *
 * <p>由 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver} 使用。
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @see org.springframework.web.multipart.MultipartResolver
 * @since 29.09.2003
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {
	/**
	 * 内容类型
	 */
	private static final String CONTENT_TYPE = "Content-Type";

	/**
	 * 多部分 —— 参数名称映射
	 */
	@Nullable
	private Map<String, String[]> multipartParameters;

	/**
	 * 多部分 —— 内容类型映射
	 */
	@Nullable
	private Map<String, String> multipartParameterContentTypes;


	/**
	 * 使用给定的 HttpServletRequest 包装一个 MultipartHttpServletRequest。
	 *
	 * @param request  要包装的 servlet 请求
	 * @param mpFiles  多部分文件的映射
	 * @param mpParams 要公开的参数的映射，键为字符串，值为字符串数组
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles,
											  Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

		super(request);
		setMultipartFiles(mpFiles);
		setMultipartParameters(mpParams);
		setMultipartParameterContentTypes(mpParamContentTypes);
	}

	/**
	 * 使用给定的 HttpServletRequest 包装一个 MultipartHttpServletRequest。
	 *
	 * @param request 要包装的 servlet 请求
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	@Nullable
	public String getParameter(String name) {
		// 获取参数值数组
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			// 如果参数值数组不为空，则返回第一个值，否则返回null
			return (values.length > 0 ? values[0] : null);
		}
		// 如果参数值数组为空，则调用父类的getParameter方法
		return super.getParameter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		// 调用父类的getParameterValues方法获取普通参数的值数组
		String[] parameterValues = super.getParameterValues(name);
		// 获取多部分参数的值数组
		String[] mpValues = getMultipartParameters().get(name);
		if (mpValues == null) {
			// 如果多部分参数值数组为空，则直接返回普通参数值数组
			return parameterValues;
		}
		if (parameterValues == null || getQueryString() == null) {
			// 如果普通参数值数组为空，或者查询字符串为空，则直接返回多部分参数值数组
			return mpValues;
		} else {
			// 否则，将多部分参数值数组和普通参数值数组合并成一个数组
			String[] result = new String[mpValues.length + parameterValues.length];
			System.arraycopy(mpValues, 0, result, 0, mpValues.length);
			System.arraycopy(parameterValues, 0, result, mpValues.length, parameterValues.length);
			return result;
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
		// 获取多部分参数的映射
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			// 如果多部分参数映射为空，则直接返回父类的getParameterNames方法的枚举结果
			return super.getParameterNames();
		}

		// 创建一个LinkedHashSet来存储参数名
		Set<String> paramNames = new LinkedHashSet<>();
		// 将父类getParameterNames方法返回的枚举结果添加到paramNames中
		paramNames.addAll(Collections.list(super.getParameterNames()));
		// 将多部分参数映射的键集合添加到paramNames中
		paramNames.addAll(multipartParameters.keySet());
		// 返回合并后的参数名集合的枚举结果
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		// 创建一个LinkedHashMap来存储参数名和对应的参数值数组
		Map<String, String[]> result = new LinkedHashMap<>();
		// 获取所有参数名的枚举
		Enumeration<String> names = getParameterNames();
		// 遍历枚举中的每个参数名
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			// 将参数名和对应的参数值数组存储到result中
			result.put(name, getParameterValues(name));
		}
		// 返回存储参数名和参数值数组的结果
		return result;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		// 根据参数或者文件名称获取多部分文件
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			// 如果文件不为空，则返回文件的内容类型
			return file.getContentType();
		} else {
			// 否则，返回多部分参数名对应的内容类型
			return getMultipartParameterContentTypes().get(paramOrFileName);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		// 获取内容类型
		String contentType = getMultipartContentType(paramOrFileName);
		if (contentType != null) {
			// 如果内容类型不为空，则创建HttpHeaders对象并设置Content-Type头部信息
			HttpHeaders headers = new HttpHeaders();
			headers.add(CONTENT_TYPE, contentType);
			return headers;
		} else {
			// 否则，返回null
			return null;
		}
	}


	/**
	 * 设置参数名称作为键和字符串数组对象作为值的映射。
	 * 由子类在初始化时调用。
	 */
	protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
		this.multipartParameters = multipartParameters;
	}

	/**
	 * 获取多部分参数映射以进行检索，如有必要则延迟初始化。
	 *
	 * @see #initializeMultipart()
	 */
	protected Map<String, String[]> getMultipartParameters() {
		if (this.multipartParameters == null) {
			// 如果多部分参数映射为空，则初始化多部分内容
			initializeMultipart();
		}
		// 返回多部分参数映射
		return this.multipartParameters;
	}

	/**
	 * 设置参数名称作为键和内容类型字符串作为值的映射。
	 * 由子类在初始化时调用。
	 */
	protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
		this.multipartParameterContentTypes = multipartParameterContentTypes;
	}

	/**
	 * 获取多部分参数内容类型映射以进行检索，如有必要则延迟初始化。
	 *
	 * @see #initializeMultipart()
	 */
	protected Map<String, String> getMultipartParameterContentTypes() {
		if (this.multipartParameterContentTypes == null) {
			// 如果多部分参数内容类型映射为空，则初始化多部分内容
			initializeMultipart();
		}
		// 返回多部分参数内容类型映射
		return this.multipartParameterContentTypes;
	}

}
