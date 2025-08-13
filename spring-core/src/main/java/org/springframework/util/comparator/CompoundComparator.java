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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一个链式调用多个比较器的复合比较器。
 *
 * <p>复合比较器会依次调用每个比较器，直到某个比较器返回非零结果，
 * 或者所有比较器都已调用完毕并返回零。
 *
 * <p>这实现了类似SQL中多列排序的内存排序功能。
 * 列表中任何单个比较器的排序顺序也可以反转。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> 此比较器可以比较的对象类型
 * @deprecated 自Spring Framework 5.0起，推荐使用标准的JDK 8
 * {@link Comparator#thenComparing(Comparator)}
 */
@Deprecated
@SuppressWarnings({"serial", "rawtypes"})
public class CompoundComparator<T> implements Comparator<T>, Serializable {

	private final List<InvertibleComparator> comparators;


	/**
	 * 构造一个初始不包含任何比较器的CompoundComparator。
	 * 客户端在调用compare方法前必须添加至少一个比较器，
	 * 否则将抛出IllegalStateException。
	 */
	public CompoundComparator() {
		this.comparators = new ArrayList<>();
	}

	/**
	 * 从提供的数组中的比较器构建复合比较器。
	 * <p>所有比较器默认使用升序排序，
	 * 除非它们是InvertibleComparator。
	 * @param comparators 用于构建复合比较器的比较器数组
	 * @throws IllegalArgumentException 如果comparators为null
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public CompoundComparator(Comparator... comparators) {
		Assert.notNull(comparators, "Comparators must not be null");
		this.comparators = new ArrayList<>(comparators.length);
		for (Comparator comparator : comparators) {
			addComparator(comparator);
		}
	}


	/**
	 * 向比较器链末尾添加一个比较器。
	 * <p>该比较器默认使用升序排序，
	 * 除非它是InvertibleComparator。
	 * @param comparator 要添加到链末尾的比较器
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.add((InvertibleComparator) comparator);
		}
		else {
			this.comparators.add(new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用指定的排序顺序向比较器链末尾添加一个比较器。
	 * @param comparator 要添加到链末尾的比较器
	 * @param ascending 排序顺序：true表示升序，false表示降序
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator, boolean ascending) {
		this.comparators.add(new InvertibleComparator(comparator, ascending));
	}

	/**
	 * 替换指定索引处的比较器。
	 * <p>该比较器默认使用升序排序，
	 * 除非它是InvertibleComparator。
	 * @param index 要替换的比较器索引
	 * @param comparator 要放置在指定索引处的比较器
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public void setComparator(int index, Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.set(index, (InvertibleComparator) comparator);
		}
		else {
			this.comparators.set(index, new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用给定的排序顺序替换指定索引处的比较器。
	 * @param index 要替换的比较器索引
	 * @param comparator 要放置在指定索引处的比较器
	 * @param ascending 排序顺序：true表示升序，false表示降序
	 */
	public void setComparator(int index, Comparator<T> comparator, boolean ascending) {
		this.comparators.set(index, new InvertibleComparator<>(comparator, ascending));
	}

	/**
	 * 反转此复合比较器中包含的所有排序定义的顺序。
	 */
	public void invertOrder() {
		for (InvertibleComparator comparator : this.comparators) {
			comparator.invertOrder();
		}
	}

	/**
	 * 反转指定索引处的排序定义的顺序。
	 * @param index 要反转的比较器索引
	 */
	public void invertOrder(int index) {
		this.comparators.get(index).invertOrder();
	}

	/**
	 * 将指定索引处的排序顺序改为升序。
	 * @param index 要修改的比较器索引
	 */
	public void setAscendingOrder(int index) {
		this.comparators.get(index).setAscending(true);
	}

	/**
	 * 将指定索引处的排序顺序改为降序。
	 * @param index 要修改的比较器索引
	 */
	public void setDescendingOrder(int index) {
		this.comparators.get(index).setAscending(false);
	}

	/**
	 * 返回聚合比较器的数量。
	 * @return 比较器的总数
	 */
	public int getComparatorCount() {
		return this.comparators.size();
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Assert.state(!this.comparators.isEmpty(),
				"No sort definitions have been added to this CompoundComparator to compare");
		for (InvertibleComparator comparator : this.comparators) {
			int result = comparator.compare(o1, o2);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof CompoundComparator &&
				this.comparators.equals(((CompoundComparator<T>) other).comparators)));
	}

	@Override
	public int hashCode() {
		return this.comparators.hashCode();
	}

	@Override
	public String toString() {
		return "CompoundComparator: " + this.comparators;
	}

}
