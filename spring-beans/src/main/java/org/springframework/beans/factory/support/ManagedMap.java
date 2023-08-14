/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tag collection class used to hold managed Map values, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 27.05.2003
 */
@SuppressWarnings("serial")
public class ManagedMap<K, V> extends LinkedHashMap<K, V> implements Mergeable, BeanMetadataElement {

	/**
	 * 元数据配置的源
	 */
	@Nullable
	private Object source;

	/**
	 * key类型名称
	 */
	@Nullable
	private String keyTypeName;

	/**
	 * value类型名称
	 */
	@Nullable
	private String valueTypeName;

	/**
	 * 是否可以进行属性合并。
	 */
	private boolean mergeEnabled;


	public ManagedMap() {
	}

	public ManagedMap(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 返回一个包含从给定条目中提取的键和值的新实例。条目本身不存储在Map中。
	 *
	 * @param entries {@code Map.Entry}包含填充Map的键和值
	 * @param <K>     {@code Map}的 key 类型
	 * @param <V>     {@code Map} 的 value 类型
	 * @return 包含指定映射的 {@code Map}
	 * @since 5.3.16
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <K, V> ManagedMap<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
		ManagedMap<K, V> map = new ManagedMap<>();
		for (Entry<? extends K, ? extends V> entry : entries) {
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

	/**
	 * 设置此元数据元素的配置源 {@code Object}。
	 * <p> 对象的确切类型将取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置要用于此映射的默认键类型名称 (类名)。
	 */
	public void setKeyTypeName(@Nullable String keyTypeName) {
		this.keyTypeName = keyTypeName;
	}

	/**
	 * 返回要用于此映射的默认键类型名称 (类名)。
	 */
	@Nullable
	public String getKeyTypeName() {
		return this.keyTypeName;
	}

	/**
	 * 设置要用于此映射的默认值类型名称 (类名)。
	 */
	public void setValueTypeName(@Nullable String valueTypeName) {
		this.valueTypeName = valueTypeName;
	}

	/**
	 * 返回要用于此映射的默认值类型名称 (类名)。
	 */
	@Nullable
	public String getValueTypeName() {
		return this.valueTypeName;
	}

	/**
	 * 设置是否应为此集合启用合并 (如果存在 “父” 集合值)。
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			//如果不允许进行属性合并，抛出异常
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			//如果父元素为空，返回当前的ManagedMap实例。
			return this;
		}
		if (!(parent instanceof Map)) {
			//如果父元素不是Map类型，抛出异常。
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		//构建一个新的ManagedMap实例。先将父元素的key-value添加进来，再将当前实例的key-value添加进来。
		Map<K, V> merged = new ManagedMap<>();
		merged.putAll((Map<K, V>) parent);
		merged.putAll(this);
		return merged;
	}

}
