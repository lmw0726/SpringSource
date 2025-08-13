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

/**
 * 当在给定的转换服务中找不到合适的转换器时抛出的异常。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ConverterNotFoundException extends ConversionException {

	@Nullable
	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;


	/**
	 * 创建新的转换执行器未找到异常。
	 * @param sourceType 请求转换的源类型
	 * @param targetType 请求转换的目标类型
	 */
	public ConverterNotFoundException(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
		this.sourceType = sourceType;
		this.targetType = targetType;
	}


	/**
	 * 返回请求转换的源类型。
	 */
	@Nullable
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * 返回请求转换的目标类型。
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

}
