/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * 通过将第一个数组元素转换为所需的目标类型后返回，将数组转换为对象。
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ArrayToObjectConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ArrayToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则返回null
		if (source == null) {
			return null;
		}
		// 如果源类型可分配给目标类型，则直接返回源对象
		if (sourceType.isAssignableTo(targetType)) {
			return source;
		}
		// 如果源数组长度为0，则返回null
		if (Array.getLength(source) == 0) {
			return null;
		}
		// 获取源数组的第一个元素
		Object firstElement = Array.get(source, 0);
		// 将源数组的第一个元素转换为目标类型，并返回结果
		return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
	}

}
