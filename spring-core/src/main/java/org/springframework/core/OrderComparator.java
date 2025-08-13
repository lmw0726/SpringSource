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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * {@link Comparator} 接口的实现，用于 {@link Ordered} 对象，按照 order 值升序排序，
 * 如果相同，则按照优先级降序排序。
 *
 * <h3>{@code PriorityOrdered} 对象</h3>
 * <p>{@link PriorityOrdered} 对象的排序优先级高于普通的 {@code Ordered} 对象。
 *
 * <h3>相同 order 的对象</h3>
 * <p>对于具有相同 order 值的对象，其排序顺序对于其他同 order 值的对象是任意的。
 *
 * <h3>无序对象</h3>
 * <p>任何没有提供自身 order 值的对象，隐式赋值为 {@link Ordered#LOWEST_PRECEDENCE}，
 * 因此在排序集合中会位于末尾，且对于其他相同 order 值的对象排序顺序任意。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see Ordered
 * @see PriorityOrdered
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 */
public class OrderComparator implements Comparator<Object> {

	/**
	 * 共享的默认 {@code OrderComparator} 实例。
	 */
	public static final OrderComparator INSTANCE = new OrderComparator();


	/**
	 * 使用给定的 order 源提供者构建一个适配的排序比较器。
	 * @param sourceProvider 要使用的 order 源提供者
	 * @return 适配后的比较器
	 * @since 4.1
	 */
	public Comparator<Object> withSourceProvider(OrderSourceProvider sourceProvider) {
		return (o1, o2) -> doCompare(o1, o2, sourceProvider);
	}

	@Override
	public int compare(@Nullable Object o1, @Nullable Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return Integer.compare(i1, i2);
	}

	/**
	 * 确定给定对象的 order 值。
	 * <p>默认实现会根据给定的 {@link OrderSourceProvider} 使用 {@link #findOrder} 查找，
	 * 若无结果则回退调用 {@link #getOrder(Object)}。
	 * @param obj 要检查的对象
	 * @return order 值，若未找到则返回 {@code Ordered.LOWEST_PRECEDENCE}
	 */
	private int getOrder(@Nullable Object obj, @Nullable OrderSourceProvider sourceProvider) {
		Integer order = null;
		if (obj != null && sourceProvider != null) {
			Object orderSource = sourceProvider.getOrderSource(obj);
			if (orderSource != null) {
				if (orderSource.getClass().isArray()) {
					for (Object source : ObjectUtils.toObjectArray(orderSource)) {
						order = findOrder(source);
						if (order != null) {
							break;
						}
					}
				}
				else {
					order = findOrder(orderSource);
				}
			}
		}
		return (order != null ? order : getOrder(obj));
	}

	/**
	 * 确定给定对象的 order 值。
	 * <p>默认实现通过委托给 {@link #findOrder} 检查是否实现了 {@link Ordered} 接口。
	 * 子类可覆盖此方法。
	 * @param obj 要检查的对象
	 * @return order 值，若未找到则返回 {@code Ordered.LOWEST_PRECEDENCE}
	 */
	protected int getOrder(@Nullable Object obj) {
		if (obj != null) {
			Integer order = findOrder(obj);
			if (order != null) {
				return order;
			}
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 查找给定对象的 order 值。
	 * <p>默认实现检查是否实现了 {@link Ordered} 接口。
	 * 子类可覆盖此方法。
	 * @param obj 要检查的对象
	 * @return order 值，若未找到返回 {@code null}
	 */
	@Nullable
	protected Integer findOrder(Object obj) {
		return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
	}

	/**
	 * 确定给定对象的优先级值（如果有）。
	 * <p>默认实现总是返回 {@code null}。
	 * 子类可覆盖此方法为特定类型赋予“优先级”特征，除了“order”语义之外。
	 * 优先级表示除了在列表或数组中排序外，还可用于选择对象。
	 * @param obj 要检查的对象
	 * @return 优先级值，若无返回 {@code null}
	 * @since 4.1
	 */
	@Nullable
	public Integer getPriority(Object obj) {
		return null;
	}


	/**
	 * 使用默认的 OrderComparator 对给定的 List 进行排序。
	 * <p>对大小为0或1的列表优化，避免不必要的数组抽取。
	 * @param list 要排序的列表
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * 使用默认的 OrderComparator 对给定的数组进行排序。
	 * <p>对大小为0或1的数组优化，避免不必要的数组抽取。
	 * @param array 要排序的数组
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 如果需要，使用默认的 OrderComparator 对给定的数组或列表进行排序。
	 * 对于其他类型的值则跳过排序。
	 * <p>对大小为0或1的列表优化，避免不必要的数组抽取。
	 * @param value 要排序的数组或列表
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


	/**
	 * 策略接口，为给定对象提供 order 源。
	 * @since 4.1
	 */
	@FunctionalInterface
	public interface OrderSourceProvider {

		/**
		 * 返回指定对象的 order 源，即用于替代该对象进行 order 值检查的对象。
		 * <p>也可以是 order 源对象数组。
		 * <p>如果返回的对象未指示任何 order，比较器将回退检查原始对象。
		 * @param obj 要查找 order 源的对象
		 * @return 该对象的 order 源，若未找到返回 {@code null}
		 */
		@Nullable
		Object getOrderSource(Object obj);
	}

}
