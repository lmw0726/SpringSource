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
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 将逗号分隔的字符串转换为集合。
 * 如果目标集合元素类型已声明，则只有当{@code String.class}可以转换为它时才匹配。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class StringToCollectionConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public StringToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (targetType.getElementTypeDescriptor() == null ||
				this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor()));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}

		// 将源对象转换为字符串类型
		String string = (String) source;

		// 将逗号分隔的字符串转换为字符串数组
		String[] fields = StringUtils.commaDelimitedListToStringArray(string);

		// 获取目标类型的元素类型描述符
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();

		// 创建一个目标集合对象
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), fields.length);

		// 如果目标元素类型描述符为null，则将每个字符串元素直接添加到目标集合中
		if (elementDesc == null) {
			for (String field : fields) {
				target.add(field.trim());
			}
		} else {
			// 否则，将每个字符串元素转换为目标类型的元素，然后添加到目标集合中
			for (String field : fields) {
				Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
				target.add(targetElement);
			}
		}
		return target;
	}

}
