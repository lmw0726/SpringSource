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

package org.springframework.core.convert;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 当实际类型转换尝试失败时抛出的异常。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ConversionFailedException extends ConversionException {

	@Nullable
	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	@Nullable
	private final Object value;


	/**
	 * 创建新的转换异常。
	 * @param sourceType 值的原始类型
	 * @param targetType 值的目标类型
	 * @param value 我们尝试转换的值
	 * @param cause 转换失败的原因
	 */
	public ConversionFailedException(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType,
			@Nullable Object value, Throwable cause) {

		super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
				"] for value '" + ObjectUtils.nullSafeToString(value) + "'", cause);
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.value = value;
	}


	/**
	 * 返回我们尝试转换值的源类型。
	 */
	@Nullable
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * 返回我们尝试转换值的目标类型。
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

	/**
	 * 返回有问题的值。
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

}
