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
 * 用于继承父级设置的 bean 的定义。
 * 子 bean 定义对父 bean 定义具有固定的依赖关系。
 *
 * <p>子 bean 定义会从父级继承构造函数参数值、属性值和方法重写，并可选择添加新的值。
 * 如果指定了初始化方法、销毁方法和/或静态工厂方法，则会覆盖父级对应的设置。
 * 其余设置将 <i>始终</i> 从子定义中获取：
 * depends on、自动装配模式、依赖检查、单例、懒加载。
 *
 * <p><b>注意：</b> 自 Spring 2.5 起，以编程方式注册 bean 定义的首选方式是 {@link GenericBeanDefinition} 类，
 * 它允许通过 {@link GenericBeanDefinition#setParentName} 方法动态定义父级依赖。
 * 对于大多数使用场景，这实际上取代了 ChildBeanDefinition 类。
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
