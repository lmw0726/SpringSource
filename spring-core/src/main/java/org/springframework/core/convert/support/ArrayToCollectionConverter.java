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

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将数组转换为集合。
 *
 * <p>首先，创建一个请求的目标类型的新集合。
 * 然后将每个数组元素添加到目标集合中。
 * 如果需要，将从源组件类型转换为集合的参数化类型。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ArrayToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}

		// 获取源数组的长度和目标元素的类型描述符
		int length = Array.getLength(source);
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();

		// 创建目标集合
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), length);

		// 如果目标元素的描述符为null，则直接将源数组中的元素添加到目标集合中
		if (elementDesc == null) {
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				target.add(sourceElement);
			}
		} else {
			// 否则，将源数组中的每个元素转换为目标元素后添加到目标集合中
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				Object targetElement = this.conversionService.convert(sourceElement,
						sourceType.elementTypeDescriptor(sourceElement), elementDesc);
				target.add(targetElement);
			}
		}
		// 返回转换后的目标集合
		return target;
	}

}
