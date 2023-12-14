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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Holder for a typed String value. Can be added to bean definitions
 * in order to explicitly specify a target type for a String value,
 * for example for collection elements.
 *
 * <p>This holder will just store the String value and the target type.
 * The actual conversion will be performed by the bean factory.
 *
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues
 * @see org.springframework.beans.MutablePropertyValues#addPropertyValue
 * @since 1.2
 */
public class TypedStringValue implements BeanMetadataElement {

	/**
	 * 字符串值
	 */
	@Nullable
	private String value;

	/**
	 * 目标类型，有两种类型，一个是String类型，一个是Class类型
	 */
	@Nullable
	private volatile Object targetType;

	/**
	 * 源对象
	 */
	@Nullable
	private Object source;

	/**
	 * 指定类型名称
	 */
	@Nullable
	private String specifiedTypeName;

	/**
	 * 该值是否为动态值
	 */
	private volatile boolean dynamic;


	/**
	 * 为给定的字符串值创建一个新的 {@link TypedStringValue}。
	 *
	 * @param value 字符串值
	 */
	public TypedStringValue(@Nullable String value) {
		setValue(value);
	}

	/**
	 * 为给定的字符串值和目标类型创建一个新的 {@link TypedStringValue}。
	 *
	 * @param value      字符串值
	 * @param targetType 要转换为的类型
	 */
	public TypedStringValue(@Nullable String value, Class<?> targetType) {
		setValue(value);
		setTargetType(targetType);
	}

	/**
	 * 为给定的字符串值和目标类型创建一个新的 {@link TypedStringValue}。
	 *
	 * @param value          字符串值
	 * @param targetTypeName 要转换为的类型
	 */
	public TypedStringValue(@Nullable String value, String targetTypeName) {
		setValue(value);
		setTargetTypeName(targetTypeName);
	}


	/**
	 * 设置字符串值。
	 * <p> 仅用于操作注册值，例如在BeanFactoryPostProcessors中。
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * 返回字符串值
	 */
	@Nullable
	public String getValue() {
		return this.value;
	}

	/**
	 * 设置要转换为的类型。
	 * <p> 仅用于操作注册值，例如在BeanFactoryPostProcessors中。
	 */
	public void setTargetType(Class<?> targetType) {
		Assert.notNull(targetType, "'targetType' must not be null");
		this.targetType = targetType;
	}

	/**
	 * 返回要转换为的类型。
	 */
	public Class<?> getTargetType() {
		Object targetTypeValue = this.targetType;
		if (!(targetTypeValue instanceof Class)) {
			throw new IllegalStateException("Typed String value does not carry a resolved target type");
		}
		return (Class<?>) targetTypeValue;
	}

	/**
	 * 指定要转换为的类型。
	 */
	public void setTargetTypeName(@Nullable String targetTypeName) {
		this.targetType = targetTypeName;
	}

	/**
	 * 返回要转换为的类型。
	 */
	@Nullable
	public String getTargetTypeName() {
		Object targetTypeValue = this.targetType;
		if (targetTypeValue instanceof Class) {
			return ((Class<?>) targetTypeValue).getName();
		} else {
			return (String) targetTypeValue;
		}
	}

	/**
	 * 返回此类型化字符串值是否携带目标类型。
	 */
	public boolean hasTargetType() {
		return (this.targetType instanceof Class);
	}

	/**
	 * 确定要转换为的类型，必要时从指定的类名解析它。
	 * 当使用已解析的目标类型调用时，还将从其名称重新加载指定的类。
	 *
	 * @param classLoader 用于解析 (潜在) 类名的类加载器
	 * @return 要转换为的解析类型
	 * @throws ClassNotFoundException 如果无法解析类型
	 */
	@Nullable
	public Class<?> resolveTargetType(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		//获取目标类型名称
		String typeName = getTargetTypeName();
		if (typeName == null) {
			//如果类型名称为空，返回null
			return null;
		}
		//委托给ClassUtils.forName获取类型名称对应的类型
		Class<?> resolvedClass = ClassUtils.forName(typeName, classLoader);
		//将目标类型，有字符串设置为Class类型
		this.targetType = resolvedClass;
		return resolvedClass;
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
	 * 将类型名称设置为实际为此特定值指定 (如果有)。
	 */
	public void setSpecifiedTypeName(@Nullable String specifiedTypeName) {
		this.specifiedTypeName = specifiedTypeName;
	}

	/**
	 * 返回实际为此特定值指定的类型名称 (如果有)。
	 */
	@Nullable
	public String getSpecifiedTypeName() {
		return this.specifiedTypeName;
	}

	/**
	 * 将此值标记为动态值，即包含表达式，因此不受缓存的影响。
	 */
	public void setDynamic() {
		this.dynamic = true;
	}

	/**
	 * 返回此值是否已被标记为动态。
	 */
	public boolean isDynamic() {
		return this.dynamic;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedStringValue)) {
			return false;
		}
		TypedStringValue otherValue = (TypedStringValue) other;
		return (ObjectUtils.nullSafeEquals(this.value, otherValue.value) &&
				ObjectUtils.nullSafeEquals(this.targetType, otherValue.targetType));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.targetType);
	}

	@Override
	public String toString() {
		return "TypedStringValue: value [" + this.value + "], target type [" + this.targetType + "]";
	}

}
