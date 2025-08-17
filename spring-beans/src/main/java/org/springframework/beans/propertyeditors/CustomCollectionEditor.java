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
import java.lang.reflect.Array;
import java.util.*;

/**
 * Collection 的属性编辑器，将任意源 Collection 转换为指定目标 Collection 类型。
 *
 * <p>默认注册用于 Set、SortedSet 和 List，
 * 以便在类型不匹配目标属性时自动将任意 Collection 转换为这些目标类型之一。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see java.util.Collection
 * @see java.util.Set
 * @see java.util.SortedSet
 * @see java.util.List
 */
public class CustomCollectionEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Collection> collectionType;

	private final boolean nullAsEmptyCollection;


	/**
	 * 为指定目标类型创建一个新的 CustomCollectionEditor，
	 * 保留传入的 {@code null} 不变。
	 * @param collectionType 目标类型，必须是 Collection 的子接口或具体 Collection 类
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType) {
		this(collectionType, false);
	}

	/**
	 * 为指定目标类型创建一个新的 CustomCollectionEditor。
	 * <p>如果传入的值是目标类型，则直接使用。
	 * 如果是不同的 Collection 类型或数组，则转换为目标 Collection 类型的默认实现。
	 * 如果是其他类型，则创建一个包含该单个值的目标 Collection。
	 * <p>默认 Collection 实现为：List 使用 ArrayList，SortedSet 使用 TreeSet，Set 使用 LinkedHashSet。
	 * @param collectionType 目标类型，必须是 Collection 的子接口或具体 Collection 类
	 * @param nullAsEmptyCollection 是否将传入的 {@code null} 值转换为空 Collection（相应类型）
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
		Assert.notNull(collectionType, "Collection type is required");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException(
					"Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
		}
		this.collectionType = collectionType;
		this.nullAsEmptyCollection = nullAsEmptyCollection;
	}


	/**
	 * 将给定的文本值转换为包含单个元素的 Collection。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定值转换为目标类型的 Collection。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyCollection) {
			super.setValue(createCollection(this.collectionType, 0));
		}
		else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
			// 源值类型已匹配，直接使用。
			super.setValue(value);
		}
		else if (value instanceof Collection) {
			// 转换 Collection 元素。
			Collection<?> source = (Collection<?>) value;
			Collection<Object> target = createCollection(this.collectionType, source.size());
			for (Object elem : source) {
				target.add(convertElement(elem));
			}
			super.setValue(target);
		}
		else if (value.getClass().isArray()) {
			// 将数组元素转换为 Collection 元素。
			int length = Array.getLength(value);
			Collection<Object> target = createCollection(this.collectionType, length);
			for (int i = 0; i < length; i++) {
				target.add(convertElement(Array.get(value, i)));
			}
			super.setValue(target);
		}
		else {
			// 单个值：转换为包含单个元素的 Collection。
			Collection<Object> target = createCollection(this.collectionType, 1);
			target.add(convertElement(value));
			super.setValue(target);
		}
	}

	/**
	 * 创建指定类型的 Collection，并设置初始容量（如果该 Collection 类型支持）。
	 * @param collectionType Collection 的子接口类型
	 * @param initialCapacity 初始容量
	 * @return 新创建的 Collection 实例
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
		if (!collectionType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionType.getName(), ex);
			}
		}
		else if (List.class == collectionType) {
			return new ArrayList<>(initialCapacity);
		}
		else if (SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else {
			return new LinkedHashSet<>(initialCapacity);
		}
	}

	/**
	 * 是否总是创建一个新的 Collection，
	 * 即使传入的 Collection 类型已经匹配。
	 * <p>默认值为 false；可被重写以强制创建新的 Collection，
	 * 例如无论如何都对元素进行转换。
	 * @see #convertElement
	 */
	protected boolean alwaysCreateNewCollection() {
		return false;
	}

	/**
	 * 钩子方法，用于转换每个遇到的 Collection/数组元素。
	 * 默认实现直接返回传入的元素。
	 * <p>可被重写以转换特定元素，
	 * 例如将 String 转换为 Integer，当输入为 String 数组且需转换为 Integer Set 时。
	 * <p>仅在实际创建新 Collection 时调用！
	 * 默认情况下，如果传入的 Collection 类型已经匹配，则不会调用。
	 * 重写 {@link #alwaysCreateNewCollection()} 可在任何情况下强制创建新 Collection。
	 * @param element 源元素
	 * @return 在目标 Collection 中使用的元素
	 * @see #alwaysCreateNewCollection()
	 */
	protected Object convertElement(Object element) {
		return element;
	}


	/**
	 * 此实现返回 {@code null}，表示没有适当的文本表示形式。
	 */
	@Override
	@Nullable
	public String getAsText() {
		return null;
	}

}
