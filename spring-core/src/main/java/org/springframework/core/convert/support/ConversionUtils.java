/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 转换包的内部工具。
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
abstract class ConversionUtils {

	@Nullable
	public static Object invokeConverter(GenericConverter converter, @Nullable Object source,
										 TypeDescriptor sourceType, TypeDescriptor targetType) {

		try {
			//调用converter的convert方法，参数为两个类型描述符以及源对象
			return converter.convert(source, sourceType, targetType);
		} catch (ConversionFailedException ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}
	}

	public static boolean canConvertElements(@Nullable TypeDescriptor sourceElementType,
											 @Nullable TypeDescriptor targetElementType, ConversionService conversionService) {

		// 如果目标元素类型为null，则直接返回true，因为不需要进行转换
		if (targetElementType == null) {
			// 是
			return true;
		}
		// 如果源元素类型为null，则可能需要进行转换，返回true
		if (sourceElementType == null) {
			// 可能是
			return true;
		}
		// 如果转换服务可以将源元素类型转换为目标元素类型，则返回true
		if (conversionService.canConvert(sourceElementType, targetElementType)) {
			// 是
			return true;
		}
		// 如果源元素类型可以分配给目标元素类型，则可能需要进行转换，返回true
		if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
			// 可能是
			return true;
		}
		// 否则，不需要进行转换，返回false
		// 否
		return false;

	}

	public static Class<?> getEnumType(Class<?> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return enumType;
	}

}
