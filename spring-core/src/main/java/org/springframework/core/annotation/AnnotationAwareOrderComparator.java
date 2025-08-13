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

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

/**
 * {@code AnnotationAwareOrderComparator} 是 {@link OrderComparator} 的扩展，
 * 它支持 Spring 的 {@link org.springframework.core.Ordered} 接口，
 * 以及 {@link Order @Order} 和 {@link javax.annotation.Priority @Priority} 注解，
 * 并且当存在 {@code Ordered} 实例时，其提供的顺序值会覆盖静态定义的注解值（如果有）。
 *
 * <p>关于非有序对象的排序语义，请参考 {@link OrderComparator} 的 Javadoc。
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 */
public class AnnotationAwareOrderComparator extends OrderComparator {

	/**
	 * {@code AnnotationAwareOrderComparator} 的共享默认实例。
	 */
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();


	/**
	 * 此实现会在父类中 {@link org.springframework.core.Ordered} 检查的基础上，
	 * 额外检查 {@link Order @Order} 或 {@link javax.annotation.Priority @Priority}
	 * 是否存在于各种元素上。
	 */
	@Override
	@Nullable
	protected Integer findOrder(Object obj) {
		Integer order = super.findOrder(obj);
		if (order != null) {
			return order;
		}
		return findOrderFromAnnotation(obj);
	}

	@Nullable
	private Integer findOrderFromAnnotation(Object obj) {
		AnnotatedElement element = (obj instanceof AnnotatedElement ? (AnnotatedElement) obj : obj.getClass());
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
		Integer order = OrderUtils.getOrderFromAnnotations(element, annotations);
		if (order == null && obj instanceof DecoratingProxy) {
			return findOrderFromAnnotation(((DecoratingProxy) obj).getDecoratedClass());
		}
		return order;
	}

	/**
	 * 此实现会获取 @{@link javax.annotation.Priority} 的值，
	 * 以便在常规 @{@link Order} 注解的基础上提供额外的语义：
	 * 通常用于在多个匹配对象中选择一个对象返回的场景。
	 */
	@Override
	@Nullable
	public Integer getPriority(Object obj) {
		if (obj instanceof Class) {
			return OrderUtils.getPriority((Class<?>) obj);
		}
		Integer priority = OrderUtils.getPriority(obj.getClass());
		if (priority == null  && obj instanceof DecoratingProxy) {
			return getPriority(((DecoratingProxy) obj).getDecoratedClass());
		}
		return priority;
	}


	/**
	 * 使用默认的 {@link AnnotationAwareOrderComparator} 对给定的 List 进行排序。
	 * <p>针对大小为 0 或 1 的列表进行了优化，跳过排序以避免不必要的数组提取。
	 * @param list 要排序的 List
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * 使用默认的 AnnotationAwareOrderComparator 对给定的数组进行排序。
	 * <p>针对长度为 0 或 1 的数组进行了优化，跳过排序以避免不必要的数组提取。
	 * @param array 要排序的数组
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 如果需要，使用默认的 AnnotationAwareOrderComparator 对给定的数组或 List 进行排序。
	 * 如果传入的值不是数组或 List，则直接跳过排序。
	 * <p>针对大小为 0 或 1 的列表进行了优化，跳过排序以避免不必要的数组提取。
	 * @param value 要排序的数组或 List
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}

}
