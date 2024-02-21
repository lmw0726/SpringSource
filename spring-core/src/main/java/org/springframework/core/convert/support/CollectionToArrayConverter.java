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
import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将集合转换为数组。
 *
 * <p> 首先，创建一个新的目标类型的数组，其长度等于源集合的大小。然后将每个集合元素设置到数组中。
 * 如果必要，将执行从集合的参数化类型到数组的组件类型的元素转换。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class CollectionToArrayConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public CollectionToArrayConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(),
				targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}

		// 将源对象转换为集合类型
		Collection<?> sourceCollection = (Collection<?>) source;

		// 获取目标类型的元素类型描述符
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		Assert.state(targetElementType != null, "No target element type");

		// 创建一个目标数组对象
		Object array = Array.newInstance(targetElementType.getType(), sourceCollection.size());
		int i = 0;

		// 遍历源集合中的每个元素，将其转换为目标类型的元素，并添加到目标数组中
		for (Object sourceElement : sourceCollection) {
			Object targetElement = this.conversionService.convert(sourceElement,
					sourceType.elementTypeDescriptor(sourceElement), targetElementType);
			Array.set(array, i++, targetElement);
		}
		return array;
	}

}
