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
import java.util.Collections;
import java.util.Set;

/**
 * 将对象转换为包含该对象的单元素数组。
 * 如果需要，将对象转换为目标数组的组件类型。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToArrayConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ObjectToArrayConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(),
				this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为空，则返回空
		if (source == null) {
			return null;
		}

		// 获取目标元素类型描述符，确保不为空
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		Assert.state(targetElementType != null, "No target element type");

		// 创建一个长度为1的目标数组
		Object target = Array.newInstance(targetElementType.getType(), 1);

		// 将源对象转换为目标元素类型并放入目标数组中
		Object targetElement = this.conversionService.convert(source, sourceType, targetElementType);
		Array.set(target, 0, targetElement);

		// 返回目标数组
		return target;
	}

}
