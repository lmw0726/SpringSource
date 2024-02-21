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
 * 将集合转换为另一个集合。
 *
 * <p>首先，创建一个请求的目标类型的新集合，其大小等于源集合的大小。
 * 然后将源集合中的每个元素复制到目标集合中。
 * 如果需要，将从源集合的参数化类型转换为目标集合的参数化类型。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class CollectionToCollectionConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public CollectionToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Collection.class));
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

		// 将源对象强制转换为集合类型
		Collection<?> sourceCollection = (Collection<?>) source;

		// 检查是否需要进行集合复制
		boolean copyRequired = !targetType.getType().isInstance(source);
		// 如果不需要复制并且源集合为空，则直接返回源对象
		if (!copyRequired && sourceCollection.isEmpty()) {
			return source;
		}

		// 获取目标元素的类型描述符
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
		// 如果目标元素描述符为null且不需要复制，则直接返回源对象
		if (elementDesc == null && !copyRequired) {
			return source;
		}

		// 创建目标集合
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), sourceCollection.size());

		// 如果目标元素描述符为null，则直接将源集合中的元素添加到目标集合中
		if (elementDesc == null) {
			target.addAll(sourceCollection);
		} else {
			// 否则，将源集合中的每个元素转换为目标元素后添加到目标集合中
			for (Object sourceElement : sourceCollection) {
				Object targetElement = this.conversionService.convert(sourceElement,
						sourceType.elementTypeDescriptor(sourceElement), elementDesc);
				target.add(targetElement);
				// 如果源元素和目标元素不相同，则需要进行复制
				if (sourceElement != targetElement) {
					copyRequired = true;
				}
			}
		}

		// 如果需要复制，则返回目标集合，否则直接返回源对象
		return (copyRequired ? target : source);
	}

}
