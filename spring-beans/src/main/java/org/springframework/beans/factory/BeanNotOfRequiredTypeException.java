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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;

/**
 * 当bean不匹配期望类型时抛出的异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

	/** 类型错误的实例名称。 */
	private final String beanName;

	/** 所需类型。 */
	private final Class<?> requiredType;

	/** 有问题的类型。 */
	private final Class<?> actualType;


	/**
	 * 创建一个新的BeanNotOfRequiredTypeException。
	 * @param beanName 请求的bean名称
	 * @param requiredType 所需类型
	 * @param actualType 返回的实际类型，与期望类型不匹配
	 */
	public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
		super("Bean named '" + beanName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
				"' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
		this.beanName = beanName;
		this.requiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * 返回类型错误的实例名称。
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回bean的期望类型。
	 */
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * 返回找到的实例的实际类型。
	 */
	public Class<?> getActualType() {
		return this.actualType;
	}

}
