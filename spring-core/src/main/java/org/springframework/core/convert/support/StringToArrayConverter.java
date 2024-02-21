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
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * 将逗号分隔的字符串转换为数组。
 * 仅当 String.class 可以转换为目标数组元素类型时才匹配。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class StringToArrayConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public StringToArrayConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(),
				this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则返回null
		if (source == null) {
			return null;
		}
		// 将源对象转换为字符串
		String string = (String) source;
		// 将字符串按逗号分隔成字符串数组
		String[] fields = StringUtils.commaDelimitedListToStringArray(string);
		// 获取目标元素类型描述符
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		// 断言目标元素类型描述符不为null
		Assert.state(targetElementType != null, "No target element type");
		// 创建目标数组对象
		Object target = Array.newInstance(targetElementType.getType(), fields.length);
		// 遍历字段数组
		for (int i = 0; i < fields.length; i++) {
			// 获取源元素字符串
			String sourceElement = fields[i];
			// 将源元素字符串转换为目标元素对象，并设置到目标数组中
			Object targetElement = this.conversionService.convert(sourceElement.trim(), sourceType, targetElementType);
			Array.set(target, i, targetElement);
		}
		// 返回目标数组对象
		return target;
	}

}
