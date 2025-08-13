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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link Predicate} 实现，为 {@link MergedAnnotation MergedAnnotations} 提供各种测试操作。
 *
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class MergedAnnotationPredicates {

	private MergedAnnotationPredicates() {
	}


	/**
	 * 创建一个新的 {@link Predicate}，如果 {@linkplain MergedAnnotation#getType() 合并注解类型} 的名称包含在
	 * 指定数组中，则该谓词的计算结果为 {@code true}。
	 * @param <A> 注解类型
	 * @param typeNames 应匹配的类型名称
	 * @return 用于测试注解类型的 {@link Predicate}
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(String... typeNames) {
		return annotation -> ObjectUtils.containsElement(typeNames, annotation.getType().getName());
	}

	/**
	 * 创建一个新的 {@link Predicate}，如果 {@linkplain MergedAnnotation#getType() 合并注解类型} 包含在
	 * 指定数组中，则该谓词的计算结果为 {@code true}。
	 * @param <A> 注解类型
	 * @param types 应匹配的类型
	 * @return 用于测试注解类型的 {@link Predicate}
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Class<?>... types) {
		return annotation -> ObjectUtils.containsElement(types, annotation.getType());
	}

	/**
	 * 创建一个新的 {@link Predicate}，如果 {@linkplain MergedAnnotation#getType() 合并注解类型} 包含在
	 * 指定集合中，则该谓词的计算结果为 {@code true}。
	 * @param <A> 注解类型
	 * @param types 应匹配的类型名称或类
	 * @return 用于测试注解类型的 {@link Predicate}
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Collection<?> types) {
		return annotation -> types.stream()
				.map(type -> type instanceof Class ? ((Class<?>) type).getName() : type.toString())
				.anyMatch(typeName -> typeName.equals(annotation.getType().getName()));
	}

	/**
	 * 创建一个新的有状态、一次性 {@link Predicate}，它只匹配提取值的第一次运行。例如，
	 * {@code MergedAnnotationPredicates.firstRunOf(MergedAnnotation::distance)}
	 * 将匹配第一个注解以及任何具有相同距离的后续运行。
	 * <p>注意：此谓词仅匹配第一次运行。一旦提取值发生变化，该谓词总是返回 {@code false}。例如，
	 * 如果您有一组距离为 {@code [1, 1, 2, 1]} 的注解，则只有前两个会匹配。
	 * @param valueExtractor 用于提取要检查的值的函数
	 * @return 匹配提取值第一次运行的 {@link Predicate}
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<A>> firstRunOf(
			Function<? super MergedAnnotation<A>, ?> valueExtractor) {

		return new FirstRunOfPredicate<>(valueExtractor);
	}

	/**
	 * 创建一个新的有状态、一次性 {@link Predicate}，它根据提取的键匹配唯一的注解。例如
	 * {@code MergedAnnotationPredicates.unique(MergedAnnotation::getType)} 将在第一次遇到唯一类型时匹配。
	 * @param keyExtractor 用于提取用于测试唯一性的键的函数
	 * @return 根据提取的键匹配唯一注解的 {@link Predicate}
	 */
	public static <A extends Annotation, K> Predicate<MergedAnnotation<A>> unique(
			Function<? super MergedAnnotation<A>, K> keyExtractor) {

		return new UniquePredicate<>(keyExtractor);
	}


	/**
	 * {@link Predicate} 实现，用于
	 * {@link MergedAnnotationPredicates#firstRunOf(Function)}。
	 */
	private static class FirstRunOfPredicate<A extends Annotation> implements Predicate<MergedAnnotation<A>> {

		private final Function<? super MergedAnnotation<A>, ?> valueExtractor;

		private boolean hasLastValue;

		@Nullable
		private Object lastValue;

		FirstRunOfPredicate(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
			Assert.notNull(valueExtractor, "Value extractor must not be null");
			this.valueExtractor = valueExtractor;
		}

		@Override
		public boolean test(@Nullable MergedAnnotation<A> annotation) {
			if (!this.hasLastValue) {
				this.hasLastValue = true;
				this.lastValue = this.valueExtractor.apply(annotation);
			}
			Object value = this.valueExtractor.apply(annotation);
			return ObjectUtils.nullSafeEquals(value, this.lastValue);

		}
	}


	/**
	 * {@link Predicate} 实现，用于
	 * {@link MergedAnnotationPredicates#unique(Function)}。
	 */
	private static class UniquePredicate<A extends Annotation, K> implements Predicate<MergedAnnotation<A>> {

		private final Function<? super MergedAnnotation<A>, K> keyExtractor;

		private final Set<K> seen = new HashSet<>();

		UniquePredicate(Function<? super MergedAnnotation<A>, K> keyExtractor) {
			Assert.notNull(keyExtractor, "Key extractor must not be null");
			this.keyExtractor = keyExtractor;
		}

		@Override
		public boolean test(@Nullable MergedAnnotation<A> annotation) {
			K key = this.keyExtractor.apply(annotation);
			return this.seen.add(key);
		}
	}

}
