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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 用于 Map 的属性编辑器，将任何源 Map 转换为指定的目标 Map 类型。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see java.util.Map
 * @see java.util.SortedMap
 */
public class CustomMapEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Map> mapType;

	private final boolean nullAsEmptyMap;


	/**
	 * 为指定的目标类型创建一个 CustomMapEditor，
	 * 保持传入的 {@code null} 不变。
	 * @param mapType 目标类型，需要是 Map 的子接口或具体 Map 类
	 * @see java.util.Map
	 * @see java.util.HashMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType) {
		this(mapType, false);
	}

	/**
	 * 为指定的目标类型创建一个 CustomMapEditor。
	 * <p>如果传入的值已经是指定类型，则直接使用。
	 * 如果是不同类型的 Map 或数组，则会转换为目标 Map 类型的默认实现。
	 * 如果是其他类型的值，将创建一个仅包含该值的目标 Map。
	 * <p>默认 Map 实现为：SortedMap 使用 TreeMap，普通 Map 使用 LinkedHashMap。
	 * @param mapType 目标类型，需要是 Map 的子接口或具体 Map 类
	 * @param nullAsEmptyMap 是否将传入的 {@code null} 值转换为空 Map（对应类型）
	 * @see java.util.Map
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap) {
		Assert.notNull(mapType, "Map type is required");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException(
					"Map type [" + mapType.getName() + "] does not implement [java.util.Map]");
		}
		this.mapType = mapType;
		this.nullAsEmptyMap = nullAsEmptyMap;
	}


	/**
	 * 将给定的文本值转换为只包含单个元素的 Map。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定的值转换为目标类型的 Map。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyMap) {
			super.setValue(createMap(this.mapType, 0));
		}
		else if (value == null || (this.mapType.isInstance(value) && !alwaysCreateNewMap())) {
			// 直接使用源值，因为它已经匹配目标类型。
			super.setValue(value);
		}
		else if (value instanceof Map) {
			// 转换 Map 元素。
			Map<?, ?> source = (Map<?, ?>) value;
			Map<Object, Object> target = createMap(this.mapType, source.size());
			source.forEach((key, val) -> target.put(convertKey(key), convertValue(val)));
			super.setValue(target);
		}
		else {
			throw new IllegalArgumentException("Value cannot be converted to Map: " + value);
		}
	}

	/**
	 * 创建指定类型的 Map，并设定初始容量（如果该 Map 类型支持）。
	 * @param mapType Map 的子接口类型
	 * @param initialCapacity 初始容量
	 * @return 新的 Map 实例
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
		if (!mapType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate map class: " + mapType.getName(), ex);
			}
		}
		else if (SortedMap.class == mapType) {
			return new TreeMap<>();
		}
		else {
			return new LinkedHashMap<>(initialCapacity);
		}
	}

	/**
	 * 返回是否总是创建一个新的 Map，
	 * 即使传入的 Map 类型已经匹配。
	 * <p>默认返回 "false"；可以通过重写此方法来强制创建新的 Map，
	 * 例如始终对元素进行转换。
	 * @see #convertKey
	 * @see #convertValue
	 */
	protected boolean alwaysCreateNewMap() {
		return false;
	}

	/**
	 * 钩子方法，用于转换每个遇到的 Map 键。
	 * 默认实现直接返回传入的键。
	 * <p>可以重写此方法对特定键进行转换，例如将 String 转为 Integer。
	 * <p>仅在实际创建新 Map 时调用！
	 * 如果传入的 Map 类型已经匹配，默认情况下不会调用。
	 * 可通过重写 {@link #alwaysCreateNewMap()} 强制每次都创建新 Map。
	 * @param key 源键
	 * @return 目标 Map 中使用的键
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertKey(Object key) {
		return key;
	}

	/**
	 * 钩子方法，用于转换每个遇到的 Map 值。
	 * 默认实现直接返回传入的值。
	 * <p>可以重写此方法对特定值进行转换，例如将 String 转为 Integer。
	 * <p>仅在实际创建新 Map 时调用！
	 * 如果传入的 Map 类型已经匹配，默认情况下不会调用。
	 * 可通过重写 {@link #alwaysCreateNewMap()} 强制每次都创建新 Map。
	 * @param value 源值
	 * @return 目标 Map 中使用的值
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertValue(Object value) {
		return value;
	}


	/**
	 * 此实现返回 {@code null}，表示没有合适的文本表示形式。
	 */
	@Override
	@Nullable
	public String getAsText() {
		return null;
	}

}
