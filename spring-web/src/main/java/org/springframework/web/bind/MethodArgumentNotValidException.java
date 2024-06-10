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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * 在使用 {@code @Valid} 注解的参数上的验证失败时抛出的异常。从 5.3 版开始扩展 {@link BindException}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends BindException {
	/**
	 * 方法参数
	 */
	private final MethodParameter parameter;


	/**
	 * MethodArgumentNotValidException 的构造函数。
	 *
	 * @param parameter     验证失败的参数
	 * @param bindingResult 验证结果
	 */
	public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
		super(bindingResult);
		this.parameter = parameter;
	}


	/**
	 * 返回验证失败的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

	@Override
	public String getMessage() {
		// 创建一个 字符串构建器 对象
		StringBuilder sb = new StringBuilder("Validation failed for argument [")
				// 添加参数索引
				.append(this.parameter.getParameterIndex()).append("] in ")
				// 添加方法的泛型字符串
				.append(this.parameter.getExecutable().toGenericString());

		// 获取绑定结果
		BindingResult bindingResult = getBindingResult();

		// 如果有多个验证错误
		if (bindingResult.getErrorCount() > 1) {
			// 添加错误数
			sb.append(" with ").append(bindingResult.getErrorCount()).append(" errors");
		}

		// 添加分隔符
		sb.append(": ");

		// 遍历所有验证错误
		for (ObjectError error : bindingResult.getAllErrors()) {
			// 将其添加到结果字符串中
			sb.append('[').append(error).append("] ");
		}

		// 返回构建的完整字符串
		return sb.toString();
	}

}
