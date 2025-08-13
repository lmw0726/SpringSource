/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.comparator.Comparators;

import java.util.Comparator;
import java.util.Map;

/**
 * 一个在比较值之前先转换值的 {@link Comparator}。
 *
 * <p>指定的 {@link Converter} 将用于在每个值传递给底层 {@code Comparator} 之前转换该值。
 *
 * @author Phillip Webb
 * @since 3.2
 * @param <S> 源类型
 * @param <T> 目标类型
 */
public class ConvertingComparator<S, T> implements Comparator<S> {

	private final Comparator<T> comparator;

	private final Converter<S, T> converter;


	/**
	 * 创建新的 {@link ConvertingComparator} 实例。
	 * @param converter 转换器
	 */
	public ConvertingComparator(Converter<S, T> converter) {
		this(Comparators.comparable(), converter);
	}

	/**
	 * 创建新的 {@link ConvertingComparator} 实例。
	 * @param comparator 用于比较转换后值的底层比较器
	 * @param converter 转换器
	 */
	public ConvertingComparator(Comparator<T> comparator, Converter<S, T> converter) {
		Assert.notNull(comparator, "Comparator must not be null");
		Assert.notNull(converter, "Converter must not be null");
		this.comparator = comparator;
		this.converter = converter;
	}

	/**
	 * 创建新的 {@code ConvertingComparator} 实例。
	 * @param comparator 底层比较器
	 * @param conversionService 转换服务
	 * @param targetType 目标类型
	 */
	public ConvertingComparator(
			Comparator<T> comparator, ConversionService conversionService, Class<? extends T> targetType) {

		this(comparator, new ConversionServiceConverter<>(conversionService, targetType));
	}


	@Override
	public int compare(S o1, S o2) {
		T c1 = this.converter.convert(o1);
		T c2 = this.converter.convert(o2);
		return this.comparator.compare(c1, c2);
	}

	/**
	 * 创建新的 {@link ConvertingComparator}，该比较器基于 {@linkplain java.util.Map.Entry#getKey() 键}
	 * 比较 {@linkplain java.util.Map.Entry 映射条目}。
	 * @param comparator 用于比较键的底层比较器
	 * @return 新的 {@link ConvertingComparator} 实例
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, K> mapEntryKeys(Comparator<K> comparator) {
		return new ConvertingComparator<>(comparator, Map.Entry::getKey);
	}

	/**
	 * 创建新的 {@link ConvertingComparator}，该比较器基于 {@linkplain java.util.Map.Entry#getValue() 值}
	 * 比较 {@linkplain java.util.Map.Entry 映射条目}。
	 * @param comparator 用于比较值的底层比较器
	 * @return 新的 {@link ConvertingComparator} 实例
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, V> mapEntryValues(Comparator<V> comparator) {
		return new ConvertingComparator<>(comparator, Map.Entry::getValue);
	}


	/**
	 * 将 {@link ConversionService} 和 <tt>targetType</tt> 适配为 {@link Converter}。
	 */
	private static class ConversionServiceConverter<S, T> implements Converter<S, T> {

		private final ConversionService conversionService;

		private final Class<? extends T> targetType;

		public ConversionServiceConverter(ConversionService conversionService, Class<? extends T> targetType) {
			Assert.notNull(conversionService, "ConversionService must not be null");
			Assert.notNull(targetType, "TargetType must not be null");
			this.conversionService = conversionService;
			this.targetType = targetType;
		}

		@Override
		@Nullable
		public T convert(S source) {
			return this.conversionService.convert(source, this.targetType);
		}
	}

}
