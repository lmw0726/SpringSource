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
	 * 在构造函数参数列表中为给定索引添加一个参数值。
	 *
	 * @param index 构造函数参数列表中的索引
	 * @param value 参数值
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value) {
		addIndexedArgumentValue(index, new ValueHolder(value));
	}

	/**
	 * 在构造函数参数列表中为给定索引添加一个参数值。
	 *
	 * @param index 构造函数参数列表中的索引
	 * @param value 参数值
	 * @param type  构造函数参数的类型
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
		addIndexedArgumentValue(index, new ValueHolder(value, type));
	}

	/**
	 * 在构造函数参数列表中为给定索引添加一个参数值。
	 *
	 * @param index    构造函数参数列表中的索引
	 * @param newValue 值持有人形式的参数值
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
	 * 检查是否已为给定索引注册了参数值。
	 *
	 * @param index 构造函数参数列表中的索引
	 */
	public boolean hasIndexedArgumentValue(int index) {
		return this.indexedArgumentValues.containsKey(index);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值。
	 *
	 * @param index        构造函数参数列表中的索引
	 * @param requiredType 要匹配的类型 (可以是 {@code null} ,表示匹配非类型化的值)
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType) {
		return getIndexedArgumentValue(index, requiredType, null);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值。
	 *
	 * @param index        构造函数参数列表中的索引
	 * @param requiredType 要匹配的类型 (可以是 {@code null} ,表示匹配非类型化的值)
	 * @param requiredName 要匹配的类型 (可以是 {@code null} 只匹配未命名的值，或空字符串匹配任何名称)
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		//根据索引获取ValueHolder
		ValueHolder valueHolder = this.indexedArgumentValues.get(index);
		if (valueHolder == null) {
			//如果值持有者为null，返回null。
			return null;
		}
		if ((valueHolder.getType() == null || (requiredType != null &&
				ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
				(valueHolder.getName() == null || (requiredName != null &&
						(requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
			//类名和构造参数名称与指定的类型和名称相同。
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回索引Map参数值。
	 *
	 * @return 以整数索引为键，以ValueHolder为值的不可修改映射
	 * @see ValueHolder
	 */
	public Map<Integer, ValueHolder> getIndexedArgumentValues() {
		return Collections.unmodifiableMap(this.indexedArgumentValues);
	}


	/**
	 * 添加要按类型匹配的通用参数值。
	 * <p> 注意: 单个通用参数值将仅使用一次，而不是多次匹配。
	 *
	 * @param value the argument value
	 */
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}

	/**
	 * 添加要按类型匹配的通用参数值。
	 * <p> 注意: 单个通用参数值将仅使用一次，而不是多次匹配。
	 *
	 * @param value 参数值
	 * @param type  构造函数参数的类型
	 */
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}

	/**
	 * 添加要按类型或名称 (如果有) 匹配的通用参数值。
	 * <p> 注意: 单个通用参数值将仅使用一次，而不是多次匹配。
	 *
	 * @param newValue ValueHolder 形式的参数值
	 *                 <p>注意: 相同的ValueHolder实例将仅注册一次，以允许合并和重新合并参数值定义。
	 *                 当然允许携带相同内容的不同ValueHolder实例。
	 */
	public void addGenericArgumentValue(ValueHolder newValue) {
		Assert.notNull(newValue, "ValueHolder must not be null");
		if (!this.genericArgumentValues.contains(newValue)) {
			//如果通用的参数值列表不包含该值，则添加该值
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
	 * 查找与给定类型匹配的通用参数值。
	 *
	 * @param requiredType 要匹配的类型
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
		return getGenericArgumentValue(requiredType, null, null);
	}

	/**
	 * 查找与给定类型匹配的通用参数值。
	 *
	 * @param requiredType 要匹配的类型
	 * @param requiredName 要匹配的名称
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
		return getGenericArgumentValue(requiredType, requiredName, null);
	}

	/**
	 * 查找与给定类型匹配的下一个通用参数值，忽略当前解决过程中已经使用的参数值。
	 *
	 * @param requiredType     要匹配的类型 (可以是 {@code null} ，表示查找下一个任意泛型参数值)
	 * @param requiredName     要匹配的名称 (可以是 {@code null} ，表示不按名称匹配参数值，或空字符串以匹配任何名称)
	 * @param usedValueHolders 一组ValueHolder对象，它们已经在当前的解析过程中使用过，因此不应该再次返回
	 * @return 参数的ValueHolder，如果没有找到，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
											   @Nullable Set<ValueHolder> usedValueHolders) {

		for (ValueHolder valueHolder : this.genericArgumentValues) {
			if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
				//如果已经使用过了，跳过处理。
				continue;
			}
			if (valueHolder.getName() != null && (requiredName == null ||
					(!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
				//如果构造参数名称不为空，要匹配的名称不为空，并且要匹配的名称与构造参数名称不匹配，跳过处理。
				continue;
			}
			if (valueHolder.getType() != null && (requiredType == null ||
					!ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
				//如果构造参数类型不为空，并且要匹配的类型为空，或者要匹配的类型与构造参数类型不匹配，跳过处理。
				continue;
			}
			if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
					!ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
				//要匹配的类型不为空，且构造参数类型为空，并且构造参数名称为空，
				// 且值valueHolder的值不是要匹配的类型（或者他的子类），跳过处理。
				continue;
			}
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回通用参数值列表。
	 *
	 * @return 不可修改的价值持有者列表
	 * @see ValueHolder
	 */
	public List<ValueHolder> getGenericArgumentValues() {
		return Collections.unmodifiableList(this.genericArgumentValues);
	}


	/**
	 * 在构造函数参数列表中查找与给定索引相对应的参数值，或者按类型进行一般匹配。
	 *
	 * @param index        构造函数参数列表中的索引
	 * @param requiredType 要匹配的参数类型
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
		return getArgumentValue(index, requiredType, null, null);
	}

	/**
	 * 在构造函数参数列表中查找与给定索引相对应的参数值，或者按类型进行一般匹配。
	 *
	 * @param index        构造函数参数列表中的索引
	 * @param requiredType 要匹配的参数类型
	 * @param requiredName 要匹配的参数名称
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
		return getArgumentValue(index, requiredType, requiredName, null);
	}

	/**
	 * 在构造函数参数列表中查找与给定索引相对应的参数值，或者按类型进行一般匹配。
	 *
	 * @param index            构造函数参数列表中的索引
	 * @param requiredType     要匹配的参数类型 (可以是 {@code null} ，表示查找无类型的参数值)
	 * @param requiredName     要匹配的参数名称 (可以是 {@code null} ，表示可以查找未命名的参数值，也可以是空字符串以匹配任何名称)
	 * @param usedValueHolders 一组ValueHolder对象，它们已经在当前的解析过程中使用过，
	 *                         因此不应该再次返回 (允许在同一类型的多个泛型参数值的情况下返回下一个泛型参数匹配)
	 * @return 参数的ValueHolder，如果未设置，则为 {@code null}
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
										@Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
		//先通过索引下标获取ValueHolder实例
		if (valueHolder == null) {
			//如果获取不到，再通过通用参数值获取ValueHolder
			valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
		}
		return valueHolder;
	}

	/**
	 * 返回此实例中保留的参数值的数量，同时计算索引参数值和通用参数值。
	 */
	public int getArgumentCount() {
		return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
	}

	/**
	 * 如果此持有者不包含任何参数值，既不包含索引值也不包含通用值，则返回true。
	 */
	public boolean isEmpty() {
		return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
	}

	/**
	 * 清除此持有者，删除所有参数值。
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
		 * 构造函数参数的类型名称
		 */
		@Nullable
		private String type;

		/**
		 * 构造函数参数的名称
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
		 * 为给定值和类型创建新的ValueHolder。
		 *
		 * @param value 参数值
		 * @param type  构造函数参数的类型
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * 为给定值、类型和名称创建新的ValueHolder。
		 *
		 * @param value 参数值
		 * @param type  构造函数参数的类型
		 * @param name  构造函数参数的名称
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type, @Nullable String name) {
			this.value = value;
			this.type = type;
			this.name = name;
		}

		/**
		 * 设置构造函数参数的值。
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
		 * 设置构造函数参数的类型。
		 */
		public void setType(@Nullable String type) {
			this.type = type;
		}

		/**
		 * 返回构造函数参数的类型。
		 */
		@Nullable
		public String getType() {
			return this.type;
		}

		/**
		 * 设置构造函数参数的名称。
		 */
		public void setName(@Nullable String name) {
			this.name = name;
		}

		/**
		 * 返回构造函数参数的名称。
		 */
		@Nullable
		public String getName() {
			return this.name;
		}

		/**
		 * 设置此元数据元素的配置源 {@code Object}。<p> 对象的确切类型将取决于所使用的配置机制。
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
		 * 返回此持有者是否已经包含转换后的值 ({@code true})，或者该值是否仍需要转换 ({@code false})。
		 */
		public synchronized boolean isConverted() {
			return this.converted;
		}

		/**
		 * 设置经过处理的类型转换后的构造函数参数的转换值。
		 */
		public synchronized void setConvertedValue(@Nullable Object value) {
			this.converted = (value != null);
			this.convertedValue = value;
		}

		/**
		 * 返回经过处理的类型转换后的构造函数参数的转换值。
		 */
		@Nullable
		public synchronized Object getConvertedValue() {
			return this.convertedValue;
		}

		/**
		 * 确定此值持有人的内容是否等于给定其他值持有人的内容。
		 * <p> 请注意，ValueHolder不会直接实现 {@code equals}，以允许具有相同内容的多个ValueHolder实例驻留在同一集中。
		 */
		private boolean contentEquals(ValueHolder other) {
			return (this == other ||
					(ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
		}

		/**
		 * 确定此值持有者的内容的哈希码。
		 * <p> 请注意，ValueHolder不会直接实现 {@code hashCode}，以允许具有相同内容的多个ValueHolder实例驻留在同一集中。
		 */
		private int contentHashCode() {
			return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
		}

		/**
		 * 创建此ValueHolder的副本: 即具有相同内容的独立ValueHolder实例。
		 */
		public ValueHolder copy() {
			ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
			copy.setSource(this.source);
			return copy;
		}
	}

}
