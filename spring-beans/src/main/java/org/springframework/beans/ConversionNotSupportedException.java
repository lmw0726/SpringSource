/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.PropertyChangeEvent;

/**
 * 当无法为 bean 属性找到合适的编辑器或转换器时抛出的异常。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ConversionNotSupportedException extends TypeMismatchException {

	/**
	 * 创建一个新的 ConversionNotSupportedException。
	 * @param propertyChangeEvent 导致问题的 PropertyChangeEvent
	 * @param requiredType 所需的目标类型（如果未知，则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent,
			@Nullable Class<?> requiredType, @Nullable Throwable cause) {
		super(propertyChangeEvent, requiredType, cause);
	}

	/**
	 * 创建一个新的 ConversionNotSupportedException。
	 * @param value 无法转换的值（可能为 {@code null}）
	 * @param requiredType 所需的目标类型（如果未知，则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public ConversionNotSupportedException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
		super(value, requiredType, cause);
	}

}
