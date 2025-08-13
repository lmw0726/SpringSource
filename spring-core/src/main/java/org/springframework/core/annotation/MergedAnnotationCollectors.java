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

package org.springframework.core.annotation;

import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

/**
 * {@link Collector} 实现，为 {@link MergedAnnotation} 实例提供各种归约操作。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class MergedAnnotationCollectors {

	private static final Characteristics[] NO_CHARACTERISTICS = {};

	private static final Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {Characteristics.IDENTITY_FINISH};


	private MergedAnnotationCollectors() {
	}


	/**
	 * 创建一个新的 {@link Collector}，它将合并的注解累积到一个
	 * 包含 {@linkplain MergedAnnotation#synthesize() 合成} 版本的 {@link LinkedHashSet} 中。
	 * <p>此方法返回的收集器实际上等同于
	 * {@code Collectors.mapping(MergedAnnotation::synthesize, Collectors.toCollection(LinkedHashSet::new))}
	 * 但避免了创建复合收集器。
	 * @param <A> 注解类型
	 * @return 一个 {@link Collector}，它收集并将注解合成到 {@link Set} 中
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
		return Collector.of(LinkedHashSet::new, (set, annotation) -> set.add(annotation.synthesize()),
				MergedAnnotationCollectors::combiner);
	}

	/**
	 * 创建一个新的 {@link Collector}，它将合并的注解累积到包含
	 * {@linkplain MergedAnnotation#synthesize() 合成} 版本的 {@link Annotation} 数组中。
	 * @param <A> 注解类型
	 * @return 一个 {@link Collector}，它收集并将注解合成到 {@code Annotation[]} 中
	 * @see #toAnnotationArray(IntFunction)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
		return toAnnotationArray(Annotation[]::new);
	}

	/**
	 * 创建一个新的 {@link Collector}，它将合并的注解累积到包含
	 * {@linkplain MergedAnnotation#synthesize() 合成} 版本的 {@link Annotation} 数组中。
	 * @param <A> 注解类型
	 * @param <R> 结果数组类型
	 * @param generator 一个函数，它生成所需类型和给定长度的新数组
	 * @return 一个 {@link Collector}，它收集并将注解合成到一个注解数组中
	 * @see #toAnnotationArray
	 */
	public static <R extends Annotation, A extends R> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(
			IntFunction<R[]> generator) {

		return Collector.of(ArrayList::new, (list, annotation) -> list.add(annotation.synthesize()),
				MergedAnnotationCollectors::combiner, list -> list.toArray(generator.apply(list.size())));
	}

	/**
	 * 创建一个新的 {@link Collector}，它将合并的注解累积到一个
	 * {@link MultiValueMap} 中，其中项目通过每个合并注解
	 * {@linkplain MultiValueMap#add(Object, Object) 添加} 为
	 * {@linkplain MergedAnnotation#asMap(Adapt...) 映射}。
	 * @param <A> 注解类型
	 * @param adaptations 应该应用于注解值的适配器
	 * @return 一个 {@link Collector}，它收集并将注解合成到
	 * {@link LinkedMultiValueMap} 中
	 * @see #toMultiValueMap(Function, MergedAnnotation.Adapt...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Adapt... adaptations) {

		return toMultiValueMap(Function.identity(), adaptations);
	}

	/**
	 * 创建一个新的 {@link Collector}，它将合并的注解累积到一个
	 * {@link MultiValueMap} 中，其中项目通过每个合并注解
	 * {@linkplain MultiValueMap#add(Object, Object) 添加} 为
	 * {@linkplain MergedAnnotation#asMap(Adapt...) 映射}。
	 * @param <A> 注解类型
	 * @param finisher 新 {@link MultiValueMap} 的完成函数
	 * @param adaptations 应该应用于注解值的适配器
	 * @return 一个 {@link Collector}，它收集并将注解合成到
	 * {@link LinkedMultiValueMap} 中
	 * @see #toMultiValueMap(MergedAnnotation.Adapt...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
			Adapt... adaptations) {

		Characteristics[] characteristics = (isSameInstance(finisher, Function.identity()) ?
				IDENTITY_FINISH_CHARACTERISTICS : NO_CHARACTERISTICS);
		return Collector.of(LinkedMultiValueMap::new,
				(map, annotation) -> annotation.asMap(adaptations).forEach(map::add),
				MergedAnnotationCollectors::combiner, finisher, characteristics);
	}


	private static boolean isSameInstance(Object instance, Object candidate) {
		return instance == candidate;
	}

	/**
	 * {@link Collector#combiner() 收集器组合器} 用于集合。
	 * <p>此方法仅在 {@link java.util.stream.Stream} 以
	 * {@linkplain java.util.stream.Stream#parallel() 并行} 方式处理时调用。
	 */
	private static <E, C extends Collection<E>> C combiner(C collection, C additions) {
		collection.addAll(additions);
		return collection;
	}

	/**
	 * {@link Collector#combiner() 收集器组合器} 用于多值映射。
	 * <p>此方法仅在 {@link java.util.stream.Stream} 以
	 * {@linkplain java.util.stream.Stream#parallel() 并行} 方式处理时调用。
	 */
	private static <K, V> MultiValueMap<K, V> combiner(MultiValueMap<K, V> map, MultiValueMap<K, V> additions) {
		map.addAll(additions);
		return map;
	}

}
