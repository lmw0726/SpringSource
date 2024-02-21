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
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 将数组转换为另一个数组。首先将源数组适配为列表，然后委托给 {@link CollectionToArrayConverter} 来执行目标数组转换。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
final class ArrayToArrayConverter implements ConditionalGenericConverter {
	/**
	 * 集合转数组转换器
	 */
	private final CollectionToArrayConverter helperConverter;
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ArrayToArrayConverter(ConversionService conversionService) {
		this.helperConverter = new CollectionToArrayConverter(conversionService);
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.helperConverter.matches(sourceType, targetType);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果转换服务是通用转换服务，则检查是否可以绕过转换
		if (this.conversionService instanceof GenericConversionService) {
			// 获取目标类型的元素描述符
			TypeDescriptor targetElement = targetType.getElementTypeDescriptor();
			// 如果目标元素不为null，并且可以绕过转换，则直接返回源对象
			if (targetElement != null &&
					((GenericConversionService) this.conversionService).canBypassConvert(
							sourceType.getElementTypeDescriptor(), targetElement)) {
				return source;
			}
		}
		// 将源数组转换为列表，并使用辅助转换器进行转换
		List<Object> sourceList = Arrays.asList(ObjectUtils.toObjectArray(source));
		return this.helperConverter.convert(sourceList, sourceType, targetType);
	}

}
