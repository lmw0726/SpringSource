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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将对象转换为包含该对象的单元素集合。
 * 如果需要，将对象转换为目标集合的参数化类型。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToCollectionConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ObjectToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}

		// 获取目标元素的类型描述符
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();

		// 创建目标集合
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), 1);

		// 如果目标元素描述符为null或者目标元素是集合类型，则直接将源对象添加到目标集合中
		if (elementDesc == null || elementDesc.isCollection()) {
			target.add(source);
		} else {
			// 否则，将源对象转换为目标元素后添加到目标集合中
			Object singleElement = this.conversionService.convert(source, sourceType, elementDesc);
			target.add(singleElement);
		}

		// 返回目标集合
		return target;
	}

}
