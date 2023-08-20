/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * Holder for constructor argument values, typically as part of a bean definition.
 *
 * <p>Supports values for a specific index in the constructor argument list
 * as well as for generic argument matches by type.
 *
 * @author Juergen Hoeller
 * @see BeanDefinition#getConstructorArgumentValues
 * @since 09.11.2003
 */
public class ConstructorArgumentValues {

	/**
	 * 存储索引下标的值对。
	 */
	private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();

	/**
	 * 通用的参数值列表，用于存储构造参数的参数名称和参数值、参数类型等信息。
	 */
	private final List<ValueHolder> genericArgumentValues = new ArrayList<>();


	/**
	 * 创建一个新的空ConstructorArgumentValues对象。
	 */
	public ConstructorArgumentValues() {
	}

	/**
	 * 深度复制构造函数。
	 *
	 * @param original 要复制的构造参数值
	 */
	public ConstructorArgumentValues(ConstructorArgumentValues original) {
		addArgumentValues(original);
	}


	/**
	 * 将所有给定的参数值复制到此对象中，使用单独的holder实例使值独立于原始对象。
	 * <p> 注意: 相同的ValueHolder实例将仅注册一次，以允许合并和重新合并参数值定义。
	 * 当然允许携带相同内容的不同ValueHolder实例。
	 */
	public void addArgumentValues(@Nullable ConstructorArgumentValues other) {
		if (other != null) {
			//遍历索引下标的值对，一次添加到 indexedArgumentValues 中。
			other.indexedArgumentValues.forEach(
					(index, argValue) -> addOrMergeIndexedArgumentValue(index, argValue.copy()));
			//如果其他构造参数值中的通用参数值不包含通用参数值中的值持有者，则将其合并到 genericArgumentValues 对象中。
			other.genericArgumentValues.stream()
					.filter(valueHolder -> !this.genericArgumentValues.contains(valueHolder))
					.forEach(valueHolder -> addOrMergeGenericArgumentValue(valueHolder.copy()));
		}
	}


	/**
	 * Add an argument value for the given index in the constructor argument list.
	 *
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value) {
		addIndexedArgumentValue(index, new ValueHolder(value));
	}

	/**
	 * Add an argument value for the given index in the constructor argument list.
	 *
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 * @param type  the type of the constructor argument
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
		addIndexedArgumentValue(index, new ValueHolder(value, type));
	}

	/**
	 * Add an argument value for the given index in the constructor argument list.
	 *
	 * @param index    the index in the constructor argument list
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	public void addIndexedArgumentValue(int index, ValueHolder newValue) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		Assert.notNull(newValue, "ValueHolder must not be null");
		addOrMergeIndexedArgumentValue(index, newValue);
	}

	/**
	 * 在构造函数参数列表中为给定索引添加一个参数值，如果需要，将新值 (通常是集合) 与当前值合并:
	 * 请参阅 {@link org.springframework.beans.Mergeable}。
	 *
	 * @param key      构造函数参数列表中的索引
	 * @param newValue 值持有人形式的参数值
	 */
	private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
		//获取该索引处的值持有者
		ValueHolder currentValue = this.indexedArgumentValues.get(key);
		if (currentValue != null && newValue.getValue() instanceof Mergeable) {
			//如果该值持有者不为空，且新值持有者的值是Mergeable类型
			Mergeable mergeable = (Mergeable) newValue.getValue();
			if (mergeable.isMergeEnabled()) {
				//如果是可合并的，合并两者的属性，并设值到新值持有者中。
				newValue.setValue(mergeable.merge(currentValue.getValue()));
			}
		}
		//添加在该索引处添加上新的值持有者
		this.indexedArgumentValues.put(key, newValue);
	}

	/**
	 * Check whether an argument value has been registered for the given index.
	 *
	 * @param index the index in the constructor argument list
	 */
	public boolean hasIndexedArgumentValue(int index) {
		return this.indexedArgumentValues.containsKey(index);
	}

	/**
	 * Get argument value for the given index in the constructor argument list.
	 *
	 * @param index        the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 *                     untyped values only)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType) {
		return getIndexedArgumentValue(index, requiredType, null);
	}

	/**
	 * Get argument value for the given index in the constructor argument list.
	 *
	 * @param index        the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 *                     untyped values only)
	 * @param requiredName the type to match (can be {@code null} to match
	 *                     unnamed values only, or empty String to match any name)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = this.indexedArgumentValues.get(index);
		if (valueHolder != null &&
				(valueHolder.getType() == null || (requiredType != null &&
						ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
				(valueHolder.getName() == null || (requiredName != null &&
						(requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
			return valueHolder;
		}
		return null;
	}

	/**
	 * Return the map of indexed argument values.
	 *
	 * @return unmodifiable Map with Integer index as key and ValueHolder as value
	 * @see ValueHolder
	 */
	public Map<Integer, ValueHolder> getIndexedArgumentValues() {
		return Collections.unmodifiableMap(this.indexedArgumentValues);
	}


	/**
	 * Add a generic argument value to be matched by type.
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 *
	 * @param value the argument value
	 */
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}

	/**
	 * Add a generic argument value to be matched by type.
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 *
	 * @param value the argument value
	 * @param type  the type of the constructor argument
	 */
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}

	/**
	 * Add a generic argument value to be matched by type or name (if available).
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 *
	 * @param newValue the argument value in the form of a ValueHolder
	 *                 <p>Note: Identical ValueHolder instances will only be registered once,
	 *                 to allow for merging and re-merging of argument value definitions. Distinct
	 *                 ValueHolder instances carrying the same content are of course allowed.
	 */
	public void addGenericArgumentValue(ValueHolder newValue) {
		Assert.notNull(newValue, "ValueHolder must not be null");
		if (!this.genericArgumentValues.contains(newValue)) {
			addOrMergeGenericArgumentValue(newValue);
		}
	}

	/**
	 * 添加一个通用参数值，如果需要，将新值 (通常是集合) 与当前值合并:
	 * 请参阅 {@link org.springframework.beans.Mergeable}。
	 *
	 * @param newValue 值持有人形式的参数值
	 */
	private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
		if (newValue.getName() != null) {
			//如果值持有者有名称
			for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext(); ) {
				//遍历当前的通用参数值中的值持有者
				ValueHolder currentValue = it.next();
				if (newValue.getName().equals(currentValue.getName())) {
					if (newValue.getValue() instanceof Mergeable) {
						//如果二者名称相同，且新值持有者是 Mergeable类型，获取值
						Mergeable mergeable = (Mergeable) newValue.getValue();
						if (mergeable.isMergeEnabled()) {
							//如果是可合并的，则合并属性
							newValue.setValue(mergeable.merge(currentValue.getValue()));
						}
					}
					it.remove();
				}
			}
		}
		this.genericArgumentValues.add(newValue);
	}

	/**
	 * Look for a generic argument value that matches the given type.
	 *
	 * @param requiredType the type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
		return getGenericArgumentValue(requiredType, null, null);
	}

	/**
	 * Look for a generic argument value that matches the given type.
	 *
	 * @param requiredType the type to match
	 * @param requiredName the name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
		return getGenericArgumentValue(requiredType, requiredName, null);
	}

	/**
	 * Look for the next generic argument value that matches the given type,
	 * ignoring argument values that have already been used in the current
	 * resolution process.
	 *
	 * @param requiredType     the type to match (can be {@code null} to find
	 *                         an arbitrary next generic argument value)
	 * @param requiredName     the name to match (can be {@code null} to not
	 *                         match argument values by name, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already been used
	 *                         in the current resolution process and should therefore not be returned again
	 * @return the ValueHolder for the argument, or {@code null} if none found
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
											   @Nullable Set<ValueHolder> usedValueHolders) {

		for (ValueHolder valueHolder : this.genericArgumentValues) {
			if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
				continue;
			}
			if (valueHolder.getName() != null && (requiredName == null ||
					(!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
				continue;
			}
			if (valueHolder.getType() != null && (requiredType == null ||
					!ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
				continue;
			}
			if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
					!ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
				continue;
			}
			return valueHolder;
		}
		return null;
	}

	/**
	 * Return the list of generic argument values.
	 *
	 * @return unmodifiable List of ValueHolders
	 * @see ValueHolder
	 */
	public List<ValueHolder> getGenericArgumentValues() {
		return Collections.unmodifiableList(this.genericArgumentValues);
	}


	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 *
	 * @param index        the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
		return getArgumentValue(index, requiredType, null, null);
	}

	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 *
	 * @param index        the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @param requiredName the parameter name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
		return getArgumentValue(index, requiredType, requiredName, null);
	}

	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 *
	 * @param index            the index in the constructor argument list
	 * @param requiredType     the parameter type to match (can be {@code null}
	 *                         to find an untyped argument value)
	 * @param requiredName     the parameter name to match (can be {@code null}
	 *                         to find an unnamed argument value, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already
	 *                         been used in the current resolution process and should therefore not
	 *                         be returned again (allowing to return the next generic argument match
	 *                         in case of multiple generic argument values of the same type)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
										@Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
		}
		return valueHolder;
	}

	/**
	 * Return the number of argument values held in this instance,
	 * counting both indexed and generic argument values.
	 */
	public int getArgumentCount() {
		return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
	}

	/**
	 * Return if this holder does not contain any argument values,
	 * neither indexed ones nor generic ones.
	 */
	public boolean isEmpty() {
		return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
	}

	/**
	 * Clear this holder, removing all argument values.
	 */
	public void clear() {
		this.indexedArgumentValues.clear();
		this.genericArgumentValues.clear();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConstructorArgumentValues)) {
			return false;
		}
		ConstructorArgumentValues that = (ConstructorArgumentValues) other;
		if (this.genericArgumentValues.size() != that.genericArgumentValues.size() ||
				this.indexedArgumentValues.size() != that.indexedArgumentValues.size()) {
			return false;
		}
		Iterator<ValueHolder> it1 = this.genericArgumentValues.iterator();
		Iterator<ValueHolder> it2 = that.genericArgumentValues.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			ValueHolder vh1 = it1.next();
			ValueHolder vh2 = it2.next();
			if (!vh1.contentEquals(vh2)) {
				return false;
			}
		}
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			ValueHolder vh1 = entry.getValue();
			ValueHolder vh2 = that.indexedArgumentValues.get(entry.getKey());
			if (vh2 == null || !vh1.contentEquals(vh2)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		for (ValueHolder valueHolder : this.genericArgumentValues) {
			hashCode = 31 * hashCode + valueHolder.contentHashCode();
		}
		hashCode = 29 * hashCode;
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			hashCode = 31 * hashCode + (entry.getValue().contentHashCode() ^ entry.getKey().hashCode());
		}
		return hashCode;
	}


	/**
	 * 构造函数参数值的持有者，带有一个可选的type属性，指示实际构造函数参数的目标类型。
	 */
	public static class ValueHolder implements BeanMetadataElement {

		/**
		 * 值
		 */
		@Nullable
		private Object value;

		/**
		 * 类名
		 */
		@Nullable
		private String type;

		/**
		 * 值的名称
		 */
		@Nullable
		private String name;

		/**
		 * 源对象
		 */
		@Nullable
		private Object source;

		/**
		 * 是否已经转换，默认为未转换。
		 */
		private boolean converted = false;

		/**
		 * 转换后的值
		 */
		@Nullable
		private Object convertedValue;

		/**
		 * 为给定值创建一个新的ValueHolder。
		 *
		 * @param value 参数值
		 */
		public ValueHolder(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * Create a new ValueHolder for the given value and type.
		 *
		 * @param value the argument value
		 * @param type  the type of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * Create a new ValueHolder for the given value, type and name.
		 *
		 * @param value the argument value
		 * @param type  the type of the constructor argument
		 * @param name  the name of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type, @Nullable String name) {
			this.value = value;
			this.type = type;
			this.name = name;
		}

		/**
		 * Set the value for the constructor argument.
		 */
		public void setValue(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * 返回构造函数参数的值。
		 */
		@Nullable
		public Object getValue() {
			return this.value;
		}

		/**
		 * Set the type of the constructor argument.
		 */
		public void setType(@Nullable String type) {
			this.type = type;
		}

		/**
		 * Return the type of the constructor argument.
		 */
		@Nullable
		public String getType() {
			return this.type;
		}

		/**
		 * Set the name of the constructor argument.
		 */
		public void setName(@Nullable String name) {
			this.name = name;
		}

		/**
		 * Return the name of the constructor argument.
		 */
		@Nullable
		public String getName() {
			return this.name;
		}

		/**
		 * Set the configuration source {@code Object} for this metadata element.
		 * <p>The exact type of the object will depend on the configuration mechanism used.
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
		 * Return whether this holder contains a converted value already ({@code true}),
		 * or whether the value still needs to be converted ({@code false}).
		 */
		public synchronized boolean isConverted() {
			return this.converted;
		}

		/**
		 * Set the converted value of the constructor argument,
		 * after processed type conversion.
		 */
		public synchronized void setConvertedValue(@Nullable Object value) {
			this.converted = (value != null);
			this.convertedValue = value;
		}

		/**
		 * Return the converted value of the constructor argument,
		 * after processed type conversion.
		 */
		@Nullable
		public synchronized Object getConvertedValue() {
			return this.convertedValue;
		}

		/**
		 * Determine whether the content of this ValueHolder is equal
		 * to the content of the given other ValueHolder.
		 * <p>Note that ValueHolder does not implement {@code equals}
		 * directly, to allow for multiple ValueHolder instances with the
		 * same content to reside in the same Set.
		 */
		private boolean contentEquals(ValueHolder other) {
			return (this == other ||
					(ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
		}

		/**
		 * Determine whether the hash code of the content of this ValueHolder.
		 * <p>Note that ValueHolder does not implement {@code hashCode}
		 * directly, to allow for multiple ValueHolder instances with the
		 * same content to reside in the same Set.
		 */
		private int contentHashCode() {
			return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
		}

		/**
		 * Create a copy of this ValueHolder: that is, an independent
		 * ValueHolder instance with the same contents.
		 */
		public ValueHolder copy() {
			ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
			copy.setSource(this.source);
			return copy;
		}
	}

}
