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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Holder containing one or more {@link PropertyValue} objects,
 * typically comprising one update for a specific target bean.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see PropertyValue
 * @since 13 May 2001
 */
public interface PropertyValues extends Iterable<PropertyValue> {

	/**
	 * 返回属性值上的 {@link Iterator}。
	 *
	 * @since 5.1
	 */
	@Override
	default Iterator<PropertyValue> iterator() {
		return Arrays.asList(getPropertyValues()).iterator();
	}

	/**
	 * 在属性值上返回 {@link Spliterator}。
	 *
	 * @since 5.1
	 */
	@Override
	default Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(getPropertyValues(), 0);
	}

	/**
	 * 返回包含属性值的顺序 {@link Stream}。
	 *
	 * @since 5.1
	 */
	default Stream<PropertyValue> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * 返回该对象中保存的PropertyValue对象的数组。
	 */
	PropertyValue[] getPropertyValues();

	/**
	 * 返回具有给定名称的属性值 (如果有)。
	 *
	 * @param propertyName 要搜索的名称
	 * @return 属性值，如果没有，则为 {@code null}
	 */
	@Nullable
	PropertyValue getPropertyValue(String propertyName);

	/**
	 * 返回从以前的PropertyValues以来的更改。子类也应该覆盖 {@code equals}。
	 *
	 * @param old 旧属性值
	 * @return 更新后的或新的属性。
	 * Return 如果没有更改，则为空属性值。
	 * @see Object#equals
	 */
	PropertyValues changesSince(PropertyValues old);

	/**
	 * 是否有此属性的属性值 (或其他处理条目)？
	 *
	 * @param propertyName 我们感兴趣的属性名称
	 * @return 此属性是否有属性名称
	 */
	boolean contains(String propertyName);

	/**
	 * 这个持有者根本不包含任何PropertyValue对象吗？
	 */
	boolean isEmpty();

}
