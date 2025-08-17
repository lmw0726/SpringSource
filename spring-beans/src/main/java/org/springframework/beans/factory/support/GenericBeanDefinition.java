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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * GenericBeanDefinition 是用于标准 Bean 定义的“一站式”解决方案。
 * 与其他 Bean 定义一样，它允许指定一个类，以及可选的构造函数参数值和属性值。
 * 此外，通过 "parentName" 属性可以灵活地配置继承自父级 Bean 定义的关系。
 *
 * <p>通常，建议使用 {@code GenericBeanDefinition} 来注册面向用户的 Bean 定义
 *（例如供后处理器操作的定义，甚至可能重新配置其父级名称）。
 * 如果父/子关系在设计时已明确确定，则可使用 {@code RootBeanDefinition} 或 {@code ChildBeanDefinition}。
 *
 * @author Juergen Hoeller
 * @see #setParentName
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @since 2.5
 */
@SuppressWarnings("serial")
public class GenericBeanDefinition extends AbstractBeanDefinition {

	/**
	 * 父bean名称
	 */
	@Nullable
	private String parentName;


	/**
	 * 创建一个新的GenericBeanDefinition，通过其bean属性和配置方法进行配置。
	 *
	 * @see #setBeanClass
	 * @see #setScope
	 * @see #setConstructorArgumentValues
	 * @see #setPropertyValues
	 */
	public GenericBeanDefinition() {
		super();
	}

	/**
	 * 创建一个新的GenericBeanDefinition作为给定bean定义的深度副本。
	 *
	 * @param original 要复制的原始bean定义
	 */
	public GenericBeanDefinition(BeanDefinition original) {
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
	public AbstractBeanDefinition cloneBeanDefinition() {
		return new GenericBeanDefinition(this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GenericBeanDefinition)) {
			return false;
		}
		GenericBeanDefinition that = (GenericBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(this.parentName, that.parentName) && super.equals(other));
	}

	@Override
	public String toString() {
		if (this.parentName != null) {
			return "Generic bean with parent '" + this.parentName + "': " + super.toString();
		}
		return "Generic bean: " + super.toString();
	}

}
