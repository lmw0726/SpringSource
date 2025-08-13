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

package org.springframework.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link MultiValueMap}的简单实现，包装了一个{@link LinkedHashMap}，
 * 使用{@link ArrayList}存储多个值。
 *
 * <p>此Map实现通常不是线程安全的。它主要设计用于从请求对象暴露的数据结构，
 * 仅在单线程中使用。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <K> 键类型
 * @param <V> 值元素类型
 */
public class LinkedMultiValueMap<K, V> extends MultiValueMapAdapter<K, V>  // 5.3新增的公共基类
		implements Serializable, Cloneable {

	private static final long serialVersionUID = 3801124242820219131L;


	/**
	 * 创建一个包装{@link LinkedHashMap}的新LinkedMultiValueMap。
	 */
	public LinkedMultiValueMap() {
		super(new LinkedHashMap<>());
	}

	/**
	 * 创建一个包装{@link LinkedHashMap}的新LinkedMultiValueMap，
	 * 初始容量可容纳指定数量的元素，无需立即调整大小/重新哈希。
	 * @param expectedSize 预期元素数量（对应容量将据此计算，
	 * 以确保不需要调整大小/重新哈希操作）
	 * @see CollectionUtils#newLinkedHashMap(int)
	 */
	public LinkedMultiValueMap(int expectedSize) {
		super(CollectionUtils.newLinkedHashMap(expectedSize));
	}

	/**
	 * 拷贝构造函数：创建一个与指定Map具有相同映射的新LinkedMultiValueMap。
	 * 注意这是浅拷贝；其值持有的List条目将被重用，因此无法独立修改。
	 * @param otherMap 要将其映射放入此Map的Map
	 * @see #clone()
	 * @see #deepCopy()
	 */
	public LinkedMultiValueMap(Map<K, List<V>> otherMap) {
		super(new LinkedHashMap<>(otherMap));
	}


	/**
	 * 创建此Map的深拷贝。
	 * @return 此Map的拷贝，包括每个值持有的List条目的拷贝
	 * （每个条目都使用独立的可修改{@link ArrayList}），
	 * 遵循{@code MultiValueMap.addAll}语义
	 * @since 4.2
	 * @see #addAll(MultiValueMap)
	 * @see #clone()
	 */
	public LinkedMultiValueMap<K, V> deepCopy() {
		LinkedMultiValueMap<K, V> copy = new LinkedMultiValueMap<>(size());
		forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
		return copy;
	}

	/**
	 * 创建此Map的常规拷贝。
	 * @return 此Map的浅拷贝，重用此Map的值持有List条目
	 * （即使某些条目是共享或不可修改的），
	 * 遵循标准{@code Map.put}语义
	 * @since 4.2
	 * @see #put(Object, List)
	 * @see #putAll(Map)
	 * @see LinkedMultiValueMap#LinkedMultiValueMap(Map)
	 * @see #deepCopy()
	 */
	@Override
	public LinkedMultiValueMap<K, V> clone() {
		return new LinkedMultiValueMap<>(this);
	}

}
