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

package org.springframework.web.bind;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletRequest;

/**
 * PropertyValues implementation created from parameters in a ServletRequest.
 * Can look for all property values beginning with a certain prefix and
 * prefix separator (default is "_").
 *
 * <p>For example, with a prefix of "spring", "spring_param1" and
 * "spring_param2" result in a Map with "param1" and "param2" as keys.
 *
 * <p>This class is not immutable to be able to efficiently remove property
 * values that should be ignored for binding.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.util.WebUtils#getParametersStartingWith
 */
@SuppressWarnings("serial")
public class ServletRequestParameterPropertyValues extends MutablePropertyValues {

	/**
	 * 默认前缀分隔符。
	 */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * 使用没有前缀 (因此，没有前缀分隔符) 创建新的ServletRequestPropertyValues。
	 *
	 * @param request HTTP 请求
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request) {
		this(request, null, null);
	}

	/**
	 * 使用给定前缀和默认前缀分隔符 (下划线字符 “_”) 创建新的ServletRequestPropertyValues。
	 *
	 * @param request HTTP 请求
	 * @param prefix  参数的前缀 (完整前缀将由此加上分隔符组成)
	 * @see #DEFAULT_PREFIX_SEPARATOR
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request, @Nullable String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * 创建新的ServletRequestPropertyValues提供前缀和前缀分隔符。
	 *
	 * @param request         HTTP请求
	 * @param prefix          参数的前缀 (完整前缀将由此加上分隔符组成)
	 * @param prefixSeparator 分隔符分隔前缀 (例如 “spring”) 和参数名称的其余部分 (“param1”，“param2”)
	 */
	public ServletRequestParameterPropertyValues(
			ServletRequest request, @Nullable String prefix, @Nullable String prefixSeparator) {

		super(WebUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
