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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 将 {@link Stream} 转换为集合或数组，并在必要时转换元素类型。
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
class StreamConverter implements ConditionalGenericConverter {
	/**
	 * 流类型描述符
	 */
	private static final TypeDescriptor STREAM_TYPE = TypeDescriptor.valueOf(Stream.class);
	/**
	 * 可转换的类型对
	 */
	private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = createConvertibleTypes();
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public StreamConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源类型可以赋值给流类型，则调用从流类型进行匹配的方法
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return matchesFromStream(sourceType.getElementTypeDescriptor(), targetType);
		}

		// 如果目标类型可以赋值给流类型，则调用到流类型进行匹配的方法
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return matchesToStream(targetType.getElementTypeDescriptor(), sourceType);
		}

		// 否则返回false，表示无法进行流类型之间的匹配
		return false;
	}

	/**
	 * 验证流中的元素的 {@link Collection} 是否可以转换为指定的 {@code targetType}。
	 * @param elementType 流元素的类型
	 * @param targetType 要转换为的类型
	 */
	public boolean matchesFromStream(@Nullable TypeDescriptor elementType, TypeDescriptor targetType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(collectionOfElement, targetType);
	}

	/**
	 * 验证指定的 {@code sourceType} 是否可以转换为流元素类型的 {@link Collection}。
	 * @param elementType 流元素的类型
	 * @param sourceType 要转换的类型
	 */
	public boolean matchesToStream(@Nullable TypeDescriptor elementType, TypeDescriptor sourceType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(sourceType, collectionOfElement);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源类型可以赋值给流类型，则调用从流类型进行转换的方法
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return convertFromStream((Stream<?>) source, sourceType, targetType);
		}

		// 如果目标类型可以赋值给流类型，则调用到流类型进行转换的方法
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return convertToStream(source, sourceType, targetType);
		}

		// 如果源和目标类型都不是流类型，表示出现了意外情况，抛出IllegalStateException
		throw new IllegalStateException("Unexpected source/target types");
	}

	@Nullable
	private Object convertFromStream(@Nullable Stream<?> source, TypeDescriptor streamType, TypeDescriptor targetType) {
		// 如果源不为null，则将流中的元素收集到列表中，否则返回空列表
		List<Object> content = (source != null ? source.collect(Collectors.<Object>toList()) : Collections.emptyList());

		// 创建列表类型的类型描述符，指定列表的元素类型
		TypeDescriptor listType = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());

		// 使用ConversionService将收集到的内容转换为目标类型
		return this.conversionService.convert(content, listType, targetType);
	}

	private Object convertToStream(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor streamType) {
		// 创建目标集合的类型描述符，指定集合类型为列表，元素类型为流的元素类型
		TypeDescriptor targetCollection = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());

		// 使用ConversionService将源转换为目标集合
		List<?> target = (List<?>) this.conversionService.convert(source, sourceType, targetCollection);

		// 如果目标集合为null，则将其设置为空列表
		if (target == null) {
			target = Collections.emptyList();
		}

		// 返回目标集合的流
		return target.stream();
	}


	private static Set<ConvertiblePair> createConvertibleTypes() {
		Set<ConvertiblePair> convertiblePairs = new HashSet<>();
		convertiblePairs.add(new ConvertiblePair(Stream.class, Collection.class));
		convertiblePairs.add(new ConvertiblePair(Stream.class, Object[].class));
		convertiblePairs.add(new ConvertiblePair(Collection.class, Stream.class));
		convertiblePairs.add(new ConvertiblePair(Object[].class, Stream.class));
		return convertiblePairs;
	}

}
