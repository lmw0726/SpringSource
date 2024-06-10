/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.core.MethodParameter;

/**
 * 表示在 {@code @RequestMapping} 方法的参数中预期的请求头不存在的 {@link ServletRequestBindingException} 子类。
 *
 * @author Juergen Hoeller
 * @see MissingRequestCookieException
 * @since 5.1
 */
@SuppressWarnings("serial")
public class MissingRequestHeaderException extends MissingRequestValueException {
	/**
	 * 请求头名称
	 */
	private final String headerName;

	/**
	 * 方法参数
	 */
	private final MethodParameter parameter;


	/**
	 * MissingRequestHeaderException 的构造函数。
	 *
	 * @param headerName 缺少的请求头的名称
	 * @param parameter  方法参数
	 */
	public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
		this(headerName, parameter, false);
	}

	/**
	 * 当值存在但转换为 {@code null} 时使用的构造函数。
	 *
	 * @param headerName             缺少的请求头的名称
	 * @param parameter              方法参数
	 * @param missingAfterConversion 值在转换后变为 null
	 * @since 5.3.6
	 */
	public MissingRequestHeaderException(
			String headerName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.headerName = headerName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		// 获取参数的嵌套参数类型的简单名称
		String typeName = this.parameter.getNestedParameterType().getSimpleName();

		// 构造描述请求头缺失的字符串
		return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回预期的请求头名称。
	 */
	public final String getHeaderName() {
		return this.headerName;
	}

	/**
	 * 返回绑定到请求头的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
