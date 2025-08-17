/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.NonNull;

/**
 * {@link BeanDefinitionStoreException} 的子类，表示无效的覆盖尝试：
 * 通常是在 {@link DefaultListableBeanFactory#isAllowBeanDefinitionOverriding()} 为 {@code false} 时，
 * 尝试为相同 bean 名称注册新的定义。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
 * @see DefaultListableBeanFactory#registerBeanDefinition
 */
@SuppressWarnings("serial")
public class BeanDefinitionOverrideException extends BeanDefinitionStoreException {

	private final BeanDefinition beanDefinition;

	private final BeanDefinition existingDefinition;


	/**
	 * 为给定的新定义和已有定义创建一个新的 BeanDefinitionOverrideException。
	 * @param beanName 要注册的 bean 名称
	 * @param beanDefinition 新注册的 bean 定义
	 * @param existingDefinition 同名的已有 bean 定义
	 */
	public BeanDefinitionOverrideException(
			String beanName, BeanDefinition beanDefinition, BeanDefinition existingDefinition) {

		super(beanDefinition.getResourceDescription(), beanName,
				"Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
				"': There is already [" + existingDefinition + "] bound.");
		this.beanDefinition = beanDefinition;
		this.existingDefinition = existingDefinition;
	}


	/**
	 * 返回该 bean 定义来源的资源描述。
	 */
	@Override
	@NonNull
	public String getResourceDescription() {
		return String.valueOf(super.getResourceDescription());
	}

	/**
	 * 返回 bean 的名称。
	 */
	@Override
	@NonNull
	public String getBeanName() {
		return String.valueOf(super.getBeanName());
	}

	/**
	 * 返回新注册的 bean 定义。
	 * @see #getBeanName()
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 返回同名的已有 bean 定义。
	 * @see #getBeanName()
	 */
	public BeanDefinition getExistingDefinition() {
		return this.existingDefinition;
	}

}
