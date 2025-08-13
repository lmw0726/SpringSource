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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Comparator;

/**
 * 一个安全比较null值的比较器，可以将null视为比其他对象小或大。
 * 可以装饰给定的Comparator或直接比较Comparable对象。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> 此比较器可以比较的对象类型
 * @see Comparable
 */
public class NullSafeComparator<T> implements Comparator<T> {

	/**
	 * 此比较器的共享默认实例，将null视为比非null对象小。
	 * @see Comparators#nullsLow()
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

	/**
	 * 此比较器的共享默认实例，将null视为比非null对象大。
	 * @see Comparators#nullsHigh()
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);


	private final Comparator<T> nonNullComparator;

	private final boolean nullsLow;


	/**
	 * 创建一个NullSafeComparator，根据提供的标志对null进行排序，适用于Comparable对象。
	 * <p>当比较两个非null对象时，将使用它们的Comparable实现：这意味着此比较器
	 * 将要应用的非null元素需要实现Comparable接口。
	 * <p>为了方便，您可以使用默认的共享实例：
	 * {@code NullSafeComparator.NULLS_LOW}和
	 * {@code NullSafeComparator.NULLS_HIGH}。
	 * @param nullsLow 是否将null视为比非null对象小
	 * @see Comparable
	 * @see #NULLS_LOW
	 * @see #NULLS_HIGH
	 */
	@SuppressWarnings("unchecked")
	private NullSafeComparator(boolean nullsLow) {
		this.nonNullComparator = ComparableComparator.INSTANCE;
		this.nullsLow = nullsLow;
	}

	/**
	 * 创建一个NullSafeComparator，根据提供的标志对null进行排序，装饰给定的Comparator。
	 * <p>当比较两个非null对象时，将使用指定的Comparator。
	 * 给定的底层Comparator必须能够处理此比较器将要应用的元素。
	 * @param comparator 用于比较两个非null对象的比较器
	 * @param nullsLow 是否将null视为比非null对象小
	 */
	public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
		Assert.notNull(comparator, "Non-null Comparator is required");
		this.nonNullComparator = comparator;
		this.nullsLow = nullsLow;
	}


	@Override
	public int compare(@Nullable T o1, @Nullable T o2) {
		if (o1 == o2) {
			return 0;
		}
		if (o1 == null) {
			return (this.nullsLow ? -1 : 1);
		}
		if (o2 == null) {
			return (this.nullsLow ? 1 : -1);
		}
		return this.nonNullComparator.compare(o1, o2);
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NullSafeComparator)) {
			return false;
		}
		NullSafeComparator<T> otherComp = (NullSafeComparator<T>) other;
		return (this.nonNullComparator.equals(otherComp.nonNullComparator) && this.nullsLow == otherComp.nullsLow);
	}

	@Override
	public int hashCode() {
		return this.nonNullComparator.hashCode() * (this.nullsLow ? -1 : 1);
	}

	@Override
	public String toString() {
		return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
				(this.nullsLow ? "nulls low" : "nulls high");
	}

}
