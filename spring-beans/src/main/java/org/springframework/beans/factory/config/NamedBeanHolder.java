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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.NamedBean;
import org.springframework.util.Assert;

/**
 * 给定bean名称和bean实例的简单持有者。
 *
 * @author Juergen Hoeller
 * @since 4.3.3
 * @param <T> bean类型
 * @see AutowireCapableBeanFactory#resolveNamedBean(Class)
 */
public class NamedBeanHolder<T> implements NamedBean {

	private final String beanName;

	private final T beanInstance;


	/**
	 * 为给定的bean名称和实例创建新的持有者。
	 * @param beanName bean的名称
	 * @param beanInstance 对应的bean实例
	 */
	public NamedBeanHolder(String beanName, T beanInstance) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanName = beanName;
		this.beanInstance = beanInstance;
	}


	/**
	 * 返回bean的名称。
	 */
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回对应的bean实例。
	 */
	public T getBeanInstance() {
		return this.beanInstance;
	}

}
