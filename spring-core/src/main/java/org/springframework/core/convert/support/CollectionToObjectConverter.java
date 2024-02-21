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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将集合转换为对象，通过将第一个集合元素转换为所需的目标类型后返回。
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToObjectConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;

	public CollectionToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}

		// 如果源类型可以赋值给目标类型，则直接返回源对象
		if (sourceType.isAssignableTo(targetType)) {
			return source;
		}

		// 将源对象转换为集合类型
		Collection<?> sourceCollection = (Collection<?>) source;

		// 如果源集合为空，则返回null
		if (sourceCollection.isEmpty()) {
			return null;
		}

		// 获取源集合中的第一个元素
		Object firstElement = sourceCollection.iterator().next();

		// 将第一个元素转换为目标类型并返回
		return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
	}

}
