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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Immutable placeholder class used for a property value object when it's
 * a reference to another bean in the factory, to be resolved at runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 */
public class RuntimeBeanReference implements BeanReference {

	/**
	 * bean名称
	 */
	private final String beanName;

	/**
	 * bean类型
	 */
	@Nullable
	private final Class<?> beanType;

	/**
	 * 是否是对父工厂中bean的显式引用。
	 */
	private final boolean toParent;

	/**
	 * 数据源
	 */
	@Nullable
	private Object source;


	/**
	 * 为给定的bean名称创建一个新的RuntimeBeanReference。
	 *
	 * @param beanName 目标bean的名称
	 */
	public RuntimeBeanReference(String beanName) {
		this(beanName, false);
	}

	/**
	 * 为给定的bean名称创建一个新的RuntimeBeanReference，并可以选择将其标记为父工厂中bean的引用。
	 *
	 * @param beanName 目标bean的名称
	 * @param toParent 是否是对父工厂中bean的显式引用
	 */
	public RuntimeBeanReference(String beanName, boolean toParent) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.beanType = null;
		this.toParent = toParent;
	}

	/**
	 *为给定类型的bean创建新的RuntimeBeanReference。
	 *
	 * @param beanType 目标bean的类型
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType) {
		this(beanType, false);
	}

	/**
	 * 为给定类型的bean创建一个新的RuntimeBeanReference，并选择将其标记为父工厂中bean的引用。
	 *
	 * @param beanType 目标bean的类型
	 * @param toParent 是否是对父工厂中bean的显式引用
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType, boolean toParent) {
		Assert.notNull(beanType, "'beanType' must not be empty");
		this.beanName = beanType.getName();
		this.beanType = beanType;
		this.toParent = toParent;
	}


	/**
	 * 返回请求的bean名称，或者在按类型解析的情况下返回完全限定的类型名称。
	 *
	 * @see #getBeanType()
	 */
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 如果需要按类型解析，则返回请求的bean类型。
	 *
	 * @since 5.2
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 返回这是否是对父工厂中bean的显式引用。
	 */
	public boolean isToParent() {
		return this.toParent;
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


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RuntimeBeanReference)) {
			return false;
		}
		RuntimeBeanReference that = (RuntimeBeanReference) other;
		return (this.beanName.equals(that.beanName) && this.beanType == that.beanType &&
				this.toParent == that.toParent);
	}

	@Override
	public int hashCode() {
		int result = this.beanName.hashCode();
		result = 29 * result + (this.toParent ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return '<' + getBeanName() + '>';
	}

}
