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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * {@link Validator} 接口的扩展变体，增加了对验证"提示"的支持。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public interface SmartValidator extends Validator {

	/**
	 * 验证传入的 {@code target} 对象，该对象必须是 {@link Class} 类型，
	 * 且其 {@link #supports(Class)} 方法通常返回 {@code true}。
	 * <p>传入的 {@link Errors errors} 实例可用于报告任何产生的验证错误。
	 * <p><b>此版本的 {@code validate()} 方法支持验证提示（hints），例如针对
	 * JSR-303 提供程序的验证组</b>（此时，提供的提示对象需要是 {@code Class} 类型的注解参数）。
	 * <p>注意：验证提示可能会被实际的目标 {@code Validator} 忽略，
	 * 此时此方法的行为应与其常规的 {@link #validate(Object, Errors)} 方法保持一致。
	 * @param target 要验证的对象
	 * @param errors 验证过程的上下文状态
	 * @param validationHints 要传递给验证引擎的一个或多个提示对象
	 * @see javax.validation.Validator#validate(Object, Class[])
	 */
	void validate(Object target, Errors errors, Object... validationHints);

	/**
	 * 验证指定目标类型上字段的传入值，报告与将该值绑定到目标类实例时相同的验证错误。
	 * @param targetType 目标类型
	 * @param fieldName 字段名称
	 * @param value 候选值
	 * @param errors 验证过程的上下文状态
	 * @param validationHints 要传递给验证引擎的一个或多个提示对象
	 * @since 5.1
	 * @see javax.validation.Validator#validateValue(Class, String, Object, Class[])
	 */
	default void validateValue(
			Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {

		throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
	}

}
