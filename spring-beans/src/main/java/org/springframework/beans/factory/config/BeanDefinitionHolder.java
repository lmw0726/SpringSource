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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Holder for a BeanDefinition with name and aliases.
 * Can be registered as a placeholder for an inner bean.
 *
 * <p>Can also be used for programmatic registration of inner bean
 * definitions. If you don't care about BeanNameAware and the like,
 * registering RootBeanDefinition or ChildBeanDefinition is good enough.
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 * @since 1.0.2
 */
public class BeanDefinitionHolder implements BeanMetadataElement {
	/**
	 * BeanDefinition对象
	 */
	private final BeanDefinition beanDefinition;
	/**
	 * Bean名称
	 */
	private final String beanName;
	/**
	 * 别名集合
	 */
	@Nullable
	private final String[] aliases;


	/**
	 * 创建一个新的BeanDefinitionHolder。
	 *
	 * @param beanDefinition 要包装的bean定义
	 * @param beanName       bean的名称 (为bean定义指定)
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
		this(beanDefinition, beanName, null);
	}

	/**
	 * 创建一个新的BeanDefinitionHolder。
	 *
	 * @param beanDefinition 要包装的bean定义
	 * @param beanName       bean的名称 (为bean定义指定)
	 * @param aliases        bean的别名，如果没有，则为 {@code null}
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanDefinition = beanDefinition;
		this.beanName = beanName;
		this.aliases = aliases;
	}

	/**
	 * 复制构造函数: 创建一个新的BeanDefinitionHolder，其内容与给定的BeanDefinitionHolder实例相同。
	 * <p> 注意: 包装的BeanDefinition引用按原样使用; 它是 {@code not} 的深度复制。
	 *
	 * @param beanDefinitionHolder 要复制的 BeanDefinitionHolder
	 */
	public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
		Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
		this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
		this.beanName = beanDefinitionHolder.getBeanName();
		this.aliases = beanDefinitionHolder.getAliases();
	}


	/**
	 * 返回包装的BeanDefinition。
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 返回为bean定义指定的bean的主名称。
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回bean的别名，直接为bean定义指定。
	 *
	 * @return 别名数组，如果没有，则为 {@code null}
	 */
	@Nullable
	public String[] getAliases() {
		return this.aliases;
	}

	/**
	 * 公开bean定义的源对象。
	 *
	 * @see BeanDefinition#getSource()
	 */
	@Override
	@Nullable
	public Object getSource() {
		return this.beanDefinition.getSource();
	}

	/**
	 * 确定给定的候选名称是否与bean名称或此bean定义中存储的别名匹配。
	 */
	public boolean matchesName(@Nullable String candidateName) {
		//候选名称与bean名称相等，或者候选名称是者与转换后的bean名称相等，或者别名中含有候选名称。
		return (candidateName != null && (candidateName.equals(this.beanName) ||
				candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
				ObjectUtils.containsElement(this.aliases, candidateName)));
	}


	/**
	 * 返回一个友好的简短描述，说明名称和别名。
	 *
	 * @see #getBeanName()
	 * @see #getAliases()
	 */
	public String getShortDescription() {
		if (this.aliases == null) {
			return "Bean definition with name '" + this.beanName + "'";
		}
		return "Bean definition with name '" + this.beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
	}

	/**
	 * 返回bean的长描述，包括名称和别名以及包含的 {@link BeanDefinition} 的描述。
	 *
	 * @see #getShortDescription()
	 * @see #getBeanDefinition()
	 */
	public String getLongDescription() {
		return getShortDescription() + ": " + this.beanDefinition;
	}

	/**
	 * 此实现返回长描述。可以重写以返回简短描述或任何类型的自定义描述。
	 *
	 * @see #getLongDescription()
	 * @see #getShortDescription()
	 */
	@Override
	public String toString() {
		return getLongDescription();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeanDefinitionHolder)) {
			return false;
		}
		BeanDefinitionHolder otherHolder = (BeanDefinitionHolder) other;
		return this.beanDefinition.equals(otherHolder.beanDefinition) &&
				this.beanName.equals(otherHolder.beanName) &&
				ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
	}

	@Override
	public int hashCode() {
		int hashCode = this.beanDefinition.hashCode();
		hashCode = 29 * hashCode + this.beanName.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
		return hashCode;
	}

}
