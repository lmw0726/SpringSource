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

package org.springframework.util.comparator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Comparator;

/**
 * 基于任意类顺序比较对象的比较器。允许根据对象继承的类类型进行排序 &mdash;
 * 例如，此比较器可用于对{@code Number}列表排序，使{@code Long}排在{@code Integer}之前。
 *
 * <p>比较时仅考虑指定的{@code instanceOrder}类。如果两个对象都是已排序类型的实例，
 * 此比较器将返回{@code 0}。如果需要额外的排序，考虑结合使用{@link Comparator#thenComparing(Comparator)}。
 *
 * @author Phillip Webb
 * @since 3.2
 * @param <T> 此比较器可以比较的对象类型
 * @see Comparator#thenComparing(Comparator)
 */
public class InstanceComparator<T> implements Comparator<T> {

	private final Class<?>[] instanceOrder;


	/**
	 * 创建新的{@link InstanceComparator}实例。
	 * @param instanceOrder 用于比较对象的有序类列表。列表中靠前的类将获得更高优先级。
	 * @throws IllegalArgumentException 如果instanceOrder数组为null
	 */
	public InstanceComparator(Class<?>... instanceOrder) {
		Assert.notNull(instanceOrder, "'instanceOrder' array must not be null");
		this.instanceOrder = instanceOrder;
	}


	@Override
	public int compare(T o1, T o2) {
		int i1 = getOrder(o1);
		int i2 = getOrder(o2);
		return (Integer.compare(i1, i2));
	}

	private int getOrder(@Nullable T object) {
		if (object != null) {
			for (int i = 0; i < this.instanceOrder.length; i++) {
				if (this.instanceOrder[i].isInstance(object)) {
					return i;
				}
			}
		}
		return this.instanceOrder.length;
	}

}
