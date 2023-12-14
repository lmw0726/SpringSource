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

package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * The default implementation of the {@link PropertyValues} interface.
 * Allows simple manipulation of properties, and provides constructors
 * to support deep copy and construction from a Map.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 13 May 2001
 */
@SuppressWarnings("serial")
public class MutablePropertyValues implements PropertyValues, Serializable {

	/**
	 * 属性值列表
	 */
	private final List<PropertyValue> propertyValueList;

	/**
	 * 已经处理过的属性名称
	 */
	@Nullable
	private Set<String> processedProperties;

	/**
	 * 是否转换过
	 */
	private volatile boolean converted;


	/**
	 * 创建一个新的空的MutablePropertyValues对象。
	 * <p> 可以使用 {@code add} 方法添加属性值。
	 *
	 * @see #add(String, Object)
	 */
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList<>(0);
	}

	/**
	 * 深度复制构造函数。保证PropertyValue引用是独立的，尽管它不能深度复制当前由各个PropertyValue对象引用的对象。
	 *
	 * @param original 要复制的属性值
	 * @see #addPropertyValues(PropertyValues)
	 */
	public MutablePropertyValues(@Nullable PropertyValues original) {
		// 我们可以优化这一点，因为它是全新的: 没有现有属性值的替换。
		if (original != null) {
			//如果属性值不为空，获取它的属性值组
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList<>(pvs.length);
			for (PropertyValue pv : pvs) {
				//遍历后，添加到propertyValueList中
				this.propertyValueList.add(new PropertyValue(pv));
			}
		} else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * 从Map构造一个新的MutablePropertyValues对象。
	 *
	 * @param original 一个Map，其属性值以属性名称字符串为键
	 * @see #addPropertyValues(Map)
	 */
	public MutablePropertyValues(@Nullable Map<?, ?> original) {
		if (original == null) {
			//如果Map为空，propertyValueList为空的ArrayList
			this.propertyValueList = new ArrayList<>(0);
		} else {
			//否则从Map中获取key-value构建成PropertyValue添加进propertyValueList中
			this.propertyValueList = new ArrayList<>(original.size());
			original.forEach((attrName, attrValue) -> this.propertyValueList.add(
					new PropertyValue(attrName.toString(), attrValue)));
		}
	}

	/**
	 * 使用给定的PropertyValue对象列表构造一个新的MutablePropertyValues对象。
	 * <p> 这是高级使用方案的构造函数。它不打算用于典型的编程用途。
	 *
	 * @param propertyValueList PropertyValue对象列表
	 */
	public MutablePropertyValues(@Nullable List<PropertyValue> propertyValueList) {
		this.propertyValueList =
				(propertyValueList != null ? propertyValueList : new ArrayList<>());
	}


	/**
	 * 以其原始形式返回PropertyValue对象的基础列表。
	 * 返回的列表可以直接修改，尽管不建议这样做。
	 * <p> 这是一个访问器，用于优化对所有PropertyValue对象的访问。它不打算用于典型的编程用途。
	 */
	public List<PropertyValue> getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * 返回列表中PropertyValue条目的数量。
	 */
	public int size() {
		return this.propertyValueList.size();
	}

	/**
	 * 将所有给定的属性值复制到此对象中。
	 * 保证PropertyValue引用是独立的，尽管它不能深度复制当前由各个PropertyValue对象引用的对象。
	 *
	 * @param other 要复制的属性值
	 * @return 这是为了允许在一个链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(@Nullable PropertyValues other) {
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			for (PropertyValue pv : pvs) {
				addPropertyValue(new PropertyValue(pv));
			}
		}
		return this;
	}

	/**
	 * 添加给定Map中的所有属性值。
	 *
	 * @param other 一个Map，其属性值由属性名作为键值，该属性名必须是字符串
	 * @return 这是为了允许在一个链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(@Nullable Map<?, ?> other) {
		if (other != null) {
			other.forEach((attrName, attrValue) -> addPropertyValue(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		return this;
	}

	/**
	 * 添加PropertyValue对象，为相应的属性替换任何现有对象或与之合并 (如果适用)。
	 *
	 * @param pv 要添加的PropertyValue对象
	 * @return 这是为了允许在一个链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				//如果属性名相同，则合并属性值
				pv = mergeIfRequired(pv, currentPv);
				//重新设置合并后的属性值
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		//否则直接添加该PropertyValue
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * {@code addPropertyValue} 的重载版本，它采用属性名称和属性值。
	 * <p> 注意: 从春季3.0开始，我们建议使用更简洁且具有链接功能的变体 {@link #add}。
	 *
	 * @param propertyName  属性名称
	 * @param propertyValue 属性值
	 * @see #addPropertyValue(PropertyValue)
	 */
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * 添加PropertyValue对象，为相应的属性替换任何现有对象或与之合并 (如果适用)。
	 *
	 * @param propertyName  属性名
	 * @param propertyValue 属性值
	 * @return 这是为了允许在一个链中添加多个属性值
	 */
	public MutablePropertyValues add(String propertyName, @Nullable Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
		return this;
	}

	/**
	 * 修改此对象中保存的PropertyValue对象。从0索引。
	 */
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * 如果支持并启用合并，则将提供的 “新” {@link PropertyValue} 的值与当前 {@link PropertyValue} 的值合并。
	 *
	 * @see Mergeable
	 */
	private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
		Object value = newPv.getValue();
		if (value instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) value;
			if (mergeable.isMergeEnabled()) {
				//如果支持属性合并，对属性值进行合并
				Object merged = mergeable.merge(currentPv.getValue());
				return new PropertyValue(newPv.getName(), merged);
			}
		}
		return newPv;
	}

	/**
	 * 删除给定的PropertyValue (如果包含)。
	 *
	 * @param pv 要删除的属性值
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	/**
	 * 采用属性名称的 {@code removePropertyValue} 重载版本。
	 *
	 * @param propertyName 属性名称
	 * @see #removePropertyValue(PropertyValue)
	 */
	public void removePropertyValue(String propertyName) {
		this.propertyValueList.remove(getPropertyValue(propertyName));
	}


	@Override
	public Iterator<PropertyValue> iterator() {
		return Collections.unmodifiableList(this.propertyValueList).iterator();
	}

	@Override
	public Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(this.propertyValueList, 0);
	}

	@Override
	public Stream<PropertyValue> stream() {
		return this.propertyValueList.stream();
	}

	@Override
	public PropertyValue[] getPropertyValues() {
		return this.propertyValueList.toArray(new PropertyValue[0]);
	}

	@Override
	@Nullable
	public PropertyValue getPropertyValue(String propertyName) {
		for (PropertyValue pv : this.propertyValueList) {
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	/**
	 * 获取原始属性值 (如果有)。
	 *
	 * @param propertyName 要搜索的名称
	 * @return 原始属性值，如果找不到，则为 {@code null}
	 * @see #getPropertyValue(String)
	 * @see PropertyValue#getValue()
	 * @since 4.0
	 */
	@Nullable
	public Object get(String propertyName) {
		PropertyValue pv = getPropertyValue(propertyName);
		return (pv != null ? pv.getValue() : null);
	}

	@Override
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}

		// 对于新集中的每个属性值
		for (PropertyValue newPv : this.propertyValueList) {
			// 如果不是旧属性值，添加它
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null || !pvOld.equals(newPv)) {
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}

	@Override
	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null ||
				(this.processedProperties != null && this.processedProperties.contains(propertyName)));
	}

	@Override
	public boolean isEmpty() {
		return this.propertyValueList.isEmpty();
	}


	/**
	 * 在某个处理器在PropertyValue(s) 机制之外调用相应的setter方法的意义上，将指定的属性注册为 “已处理”。
	 * <p> 这将导致从针对指定属性的 {@link #contains} 调用中返回 {@code true}。
	 *
	 * @param propertyName 属性的名称。
	 */
	public void registerProcessedProperty(String propertyName) {
		if (this.processedProperties == null) {
			this.processedProperties = new HashSet<>(4);
		}
		this.processedProperties.add(propertyName);
	}

	/**
	 * 清除给定属性名称的“已处理过”注册，如果有
	 *
	 * @since 3.2.13
	 */
	public void clearProcessedProperty(String propertyName) {
		if (this.processedProperties != null) {
			this.processedProperties.remove(propertyName);
		}
	}

	/**
	 * 将此持有者标记为仅包含转换后的值 (即不再需要运行时解析)。
	 */
	public void setConverted() {
		this.converted = true;
	}

	/**
	 * 返回此持有者是否仅包含转换后的值 ({@code true})，或者是否仍需要转换值 ({@code false})。
	 */
	public boolean isConverted() {
		return this.converted;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MutablePropertyValues &&
				this.propertyValueList.equals(((MutablePropertyValues) other).propertyValueList)));
	}

	@Override
	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	@Override
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		if (pvs.length > 0) {
			return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
		}
		return "PropertyValues: length=0";
	}

}
