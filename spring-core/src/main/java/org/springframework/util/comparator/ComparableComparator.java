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
 * 将Comparable适配到Comparator接口的比较器。
 * 主要用于其他比较器内部，当需要处理Comparable对象时。
 *
 * @author Keith Donald
 * @since 1.2.2
 * @param <T> 此比较器可以比较的可比较对象类型
 * @see Comparable
 */
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

	/**
	 * 此默认比较器的共享实例。
	 * @see Comparators#comparable()
	 */
	@SuppressWarnings("rawtypes")
	public static final ComparableComparator INSTANCE = new ComparableComparator();


	@Override
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}

}
