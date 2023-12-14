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

package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see PropertyValues
 * @see BeanWrapper
 * @since 13 May 2001
 */
@SuppressWarnings("serial")
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {

	/**
	 * 属性名称
	 */
	private final String name;

	/**
	 * 属性值
	 */
	@Nullable
	private final Object value;

	/**
	 * 是否可选，默认为不可选
	 */
	private boolean optional = false;

	/**
	 * 是否已经转换，默认为false，表示未转换
	 */
	private boolean converted = false;

	/**
	 * 转换后的值
	 */
	@Nullable
	private Object convertedValue;

	/**
	 * 包可见字段，指示是否需要转换。
	 */
	@Nullable
	volatile Boolean conversionNecessary;

	/**
	 * 用于缓存解析的属性路径令牌的包可见字段。
	 */
	@Nullable
	transient volatile Object resolvedTokens;


	/**
	 * 创建一个新的PropertyValue实例。
	 *
	 * @param name  属性的名称 (从不 {@code null})
	 * @param value 属性的值 (可能在类型转换之前)
	 */
	public PropertyValue(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}

	/**
	 * 复制构造函数。
	 *
	 * @param original 要复制的PropertyValue (从不 {@code null})
	 */
	public PropertyValue(PropertyValue original) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = original.getValue();
		this.optional = original.isOptional();
		this.converted = original.converted;
		this.convertedValue = original.convertedValue;
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original.getSource());
		//复制源对象的属性名和属性值
		copyAttributesFrom(original);
	}

	/**
	 * 为原始值持有者公开新值的构造函数。原始持有人将被暴露为新持有人的来源。
	 *
	 * @param original 要链接到的PropertyValue (从不 {@code null})
	 * @param newValue 要应用的新值
	 */
	public PropertyValue(PropertyValue original, @Nullable Object newValue) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = newValue;
		this.optional = original.isOptional();
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original);
		copyAttributesFrom(original);
	}


	/**
	 * 返回属性的名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回属性的值。
	 * <p> 请注意，此处将发生类型转换 <i> 不是 <i>。
	 * 执行类型转换是BeanWrapper实现的责任。
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 返回此值持有者的原始PropertyValue实例。
	 *
	 * @return 原始属性值 (此值持有者或此值持有者本身的来源)。
	 */
	public PropertyValue getOriginalPropertyValue() {
		PropertyValue original = this;
		Object source = getSource();
		while (source instanceof PropertyValue && source != original) {
			original = (PropertyValue) source;
			source = original.getSource();
		}
		return original;
	}

	/**
	 * 设置这是否是可选值，即在目标类上不存在相应属性时将被忽略。
	 *
	 * @since 3.0
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * 返回这是否是可选值，即当目标类上不存在相应属性时，将被忽略。
	 *
	 * @since 3.0
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * 返回此持有者是否已经包含转换后的值 ({@code true})，或者该值是否仍需要转换 ({@code false})。
	 */
	public synchronized boolean isConverted() {
		return this.converted;
	}

	/**
	 * 设置此属性值的转换后的值，经过处理类型转换。
	 */
	public synchronized void setConvertedValue(@Nullable Object value) {
		this.converted = true;
		this.convertedValue = value;
	}

	/**
	 * 返回此属性值的转换后的值，经过处理的类型转换。
	 */
	@Nullable
	public synchronized Object getConvertedValue() {
		return this.convertedValue;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PropertyValue)) {
			return false;
		}
		PropertyValue otherPv = (PropertyValue) other;
		return (this.name.equals(otherPv.name) &&
				ObjectUtils.nullSafeEquals(this.value, otherPv.value) &&
				ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource()));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "bean property '" + this.name + "'";
	}

}
