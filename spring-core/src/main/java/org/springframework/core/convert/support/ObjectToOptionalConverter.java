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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 如果需要，使用 {@code ConversionService} 将源对象转换为 {@code java.util.Optional<T>}，其中 T 是可选项的泛型类型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
final class ObjectToOptionalConverter implements ConditionalGenericConverter {

	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ObjectToOptionalConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<>(4);
		convertibleTypes.add(new ConvertiblePair(Collection.class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object[].class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object.class, Optional.class));
		return convertibleTypes;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getResolvableType().hasGenerics()) {
			return this.conversionService.canConvert(sourceType, new GenericTypeDescriptor(targetType));
		} else {
			return true;
		}
	}

	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			// 如果源为null，则返回一个空的Optional对象
			return Optional.empty();
		} else if (source instanceof Optional) {
			// 如果源已经是Optional类型，则直接返回
			return source;
		} else if (targetType.getResolvableType().hasGenerics()) {
			// 如果目标类型具有泛型信息
			// 使用ConversionService将源转换为目标类型
			Object target = this.conversionService.convert(source, sourceType, new GenericTypeDescriptor(targetType));
			// 如果转换后的目标对象为null，或者是空数组，或者是空集合，则返回一个空的Optional对象
			if (target == null || (target.getClass().isArray() && Array.getLength(target) == 0) ||
					(target instanceof Collection && ((Collection<?>) target).isEmpty())) {
				return Optional.empty();
			}
			// 否则，返回一个包含转换后目标对象的Optional对象
			return Optional.of(target);
		} else {
			// 否则，直接返回一个包含源对象的Optional对象
			return Optional.of(source);
		}
	}


	@SuppressWarnings("serial")
	private static class GenericTypeDescriptor extends TypeDescriptor {

		public GenericTypeDescriptor(TypeDescriptor typeDescriptor) {
			super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
		}
	}

}
