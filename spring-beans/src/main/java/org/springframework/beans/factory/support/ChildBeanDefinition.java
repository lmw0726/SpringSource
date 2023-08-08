/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Bean definition for beans which inherit settings from their parent.
 * Child bean definitions have a fixed dependency on a parent bean definition.
 *
 * <p>A child bean definition will inherit constructor argument values,
 * property values and method overrides from the parent, with the option
 * to add new values. If init method, destroy method and/or static factory
 * method are specified, they will override the corresponding parent settings.
 * The remaining settings will <i>always</i> be taken from the child definition:
 * depends on, autowire mode, dependency check, singleton, lazy init.
 *
 * <p><b>NOTE:</b> Since Spring 2.5, the preferred way to register bean
 * definitions programmatically is the {@link GenericBeanDefinition} class,
 * which allows to dynamically define parent dependencies through the
 * {@link GenericBeanDefinition#setParentName} method. This effectively
 * supersedes the ChildBeanDefinition class for most use cases.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 */
@SuppressWarnings("serial")
public class ChildBeanDefinition extends AbstractBeanDefinition {
	/**
	 * 父bean名称
	 */
	@Nullable
	private String parentName;


	/**
	 * 为给定的父级创建一个新的ChildBeanDefinition，通过其bean属性和配置方法进行配置。
	 *
	 * @param parentName 父bean的名称
	 * @see #setBeanClass
	 * @see #setScope
	 * @see #setConstructorArgumentValues
	 * @see #setPropertyValues
	 */
	public ChildBeanDefinition(String parentName) {
		super();
		this.parentName = parentName;
	}

	/**
	 * 为给定的父级创建新的ChildBeanDefinition。
	 *
	 * @param parentName 父bean的名称
	 * @param pvs        子级的附加属性值
	 */
	public ChildBeanDefinition(String parentName, MutablePropertyValues pvs) {
		super(null, pvs);
		this.parentName = parentName;
	}

	/**
	 * 为给定的父级创建新的ChildBeanDefinition。
	 *
	 * @param parentName 父bean名称
	 * @param cargs      要应用的构造函数参数值
	 * @param pvs        子级的附加属性值
	 */
	public ChildBeanDefinition(
			String parentName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
	}

	/**
	 * 为给定的父级创建一个新的ChildBeanDefinition，提供构造函数参数和属性值。
	 *
	 * @param parentName 父bean的名称
	 * @param beanClass  要实例化的bean的类
	 * @param cargs      要应用的构造函数参数值
	 * @param pvs        要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, Class<?> beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClass(beanClass);
	}

	/**
	 * 为给定的父级创建一个新的ChildBeanDefinition，提供构造函数参数和属性值。
	 * 采用bean类名称，以避免早期加载bean类。
	 *
	 * @param parentName    父bean的名称
	 * @param beanClassName 要实例化的类的名称
	 * @param cargs         要应用的构造函数参数值
	 * @param pvs           要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClassName(beanClassName);
	}

	/**
	 * 创建一个新的ChildBeanDefinition作为给定bean定义的深度副本。
	 *
	 * @param original 要复制的原始bean定义
	 */
	public ChildBeanDefinition(ChildBeanDefinition original) {
		super(original);
	}


	@Override
	public void setParentName(@Nullable String parentName) {
		this.parentName = parentName;
	}

	@Override
	@Nullable
	public String getParentName() {
		return this.parentName;
	}

	@Override
	public void validate() throws BeanDefinitionValidationException {
		super.validate();
		if (this.parentName == null) {
			throw new BeanDefinitionValidationException("'parentName' must be set in ChildBeanDefinition");
		}
	}


	@Override
	public AbstractBeanDefinition cloneBeanDefinition() {
		return new ChildBeanDefinition(this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ChildBeanDefinition)) {
			return false;
		}
		ChildBeanDefinition that = (ChildBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(this.parentName, that.parentName) && super.equals(other));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.parentName) * 29 + super.hashCode();
	}

	@Override
	public String toString() {
		return "Child bean with parent '" + this.parentName + "': " + super.toString();
	}

}
