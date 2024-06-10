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
 * {@link ServletRequestBindingException} 的子类，指示在 {@code @RequestMapping} 方法的方法参数中期望的路径变量不存在于从 URL 中提取的 URI 变量中。
 * 通常这意味着 URI 模板与在方法参数上声明的路径变量名称不匹配。
 *
 * @author Rossen Stoyanchev
 * @see MissingMatrixVariableException
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MissingPathVariableException extends MissingRequestValueException {
	/**
	 * 变量名称
	 */
	private final String variableName;

	/**
	 * 方法参数
	 */
	private final MethodParameter parameter;


	/**
	 * MissingPathVariableException 的构造函数。
	 *
	 * @param variableName 缺失的路径变量的名称
	 * @param parameter    方法参数
	 */
	public MissingPathVariableException(String variableName, MethodParameter parameter) {
		this(variableName, parameter, false);
	}

	/**
	 * 在存在值但转换为 {@code null} 时使用的构造函数。
	 *
	 * @param variableName           缺失的路径变量的名称
	 * @param parameter              方法参数
	 * @param missingAfterConversion 在转换后值是否变为 null
	 * @since 5.3.6
	 */
	public MissingPathVariableException(
			String variableName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.variableName = variableName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Required URI template variable '" + this.variableName + "' for method parameter type " +
				this.parameter.getNestedParameterType().getSimpleName() + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回期望的路径变量的名称。
	 */
	public final String getVariableName() {
		return this.variableName;
	}

	/**
	 * 返回绑定到路径变量的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
