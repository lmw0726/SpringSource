/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.method.annotation;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * 在解析控制器方法参数时引发的TypeMismatchException。
 * 提供对目标{@link org.springframework.core.MethodParameter MethodParameter}的访问。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class MethodArgumentTypeMismatchException extends TypeMismatchException {

	/**
	 * 方法参数的名称
	 */
	private final String name;

	/**
	 * 目标方法参数
	 */
	private final MethodParameter parameter;

	/**
	 * 使用给定的值、所需类型、名称、方法参数和原因构造一个MethodArgumentTypeMismatchException实例。
	 *
	 * @param value 转换失败的值
	 * @param requiredType 所需的类型
	 * @param name 方法参数的名称
	 * @param param 目标方法参数
	 * @param cause 引起转换失败的异常
	 */
	public MethodArgumentTypeMismatchException(@Nullable Object value,
											   @Nullable Class<?> requiredType, String name, MethodParameter param, Throwable cause) {

		super(value, requiredType, cause);
		this.name = name;
		this.parameter = param;
	}


	/**
	 * 返回方法参数的名称。
	 *
	 * @return 方法参数的名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回目标方法参数。
	 *
	 * @return 目标方法参数
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

}
