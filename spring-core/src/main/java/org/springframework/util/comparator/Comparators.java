/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Comparator;

/**
 * 提供通用类型工厂方法的便捷入口点，
 * 用于创建常见的Spring {@link Comparator}变体。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class Comparators {

	/**
	 * 返回一个{@link Comparable}适配器。
	 * @see ComparableComparator#INSTANCE
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> comparable() {
		return ComparableComparator.INSTANCE;
	}

	/**
	 * 返回一个接受null值并将其排序在非null值之下的{@link Comparable}适配器。
	 * @see NullSafeComparator#NULLS_LOW
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> nullsLow() {
		return NullSafeComparator.NULLS_LOW;
	}

	/**
	 * 返回给定比较器的装饰器，接受null值并将其排序在非null值之下。
	 * @param comparator 要装饰的比较器
	 * @see NullSafeComparator#NullSafeComparator(boolean)
	 */
	public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
		return new NullSafeComparator<>(comparator, true);
	}

	/**
	 * 返回一个接受null值并将其排序在非null值之上的{@link Comparable}适配器。
	 * @see NullSafeComparator#NULLS_HIGH
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> nullsHigh() {
		return NullSafeComparator.NULLS_HIGH;
	}

	/**
	 * 返回给定比较器的装饰器，接受null值并将其排序在非null值之上。
	 * @param comparator 要装饰的比较器
	 * @see NullSafeComparator#NullSafeComparator(boolean)
	 */
	public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
		return new NullSafeComparator<>(comparator, false);
	}

}
