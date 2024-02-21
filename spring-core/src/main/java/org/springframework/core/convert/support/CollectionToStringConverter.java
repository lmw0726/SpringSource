/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.StringJoiner;

/**
 * 将一个 Collection 转换为逗号分隔的字符串。
 *
 * @since 3.0
 */
final class CollectionToStringConverter implements ConditionalGenericConverter {
	/**
	 * 分隔符
	 */
	private static final String DELIMITER = ",";
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public CollectionToStringConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则返回null
		if (source == null) {
			return null;
		}

		// 将源对象转换为Collection类型
		Collection<?> sourceCollection = (Collection<?>) source;

		// 如果源集合为空，则返回空字符串
		if (sourceCollection.isEmpty()) {
			return "";
		}

		// 创建StringJoiner对象，用于拼接字符串
		StringJoiner sj = new StringJoiner(DELIMITER);

		// 遍历源集合中的元素，进行转换并拼接成字符串
		for (Object sourceElement : sourceCollection) {
			// 转换源集合中的元素为目标类型并添加到StringJoiner中
			Object targetElement = this.conversionService.convert(
					sourceElement, sourceType.elementTypeDescriptor(sourceElement), targetType);
			sj.add(String.valueOf(targetElement));
		}

		// 返回拼接后的字符串
		return sj.toString();
	}

}
