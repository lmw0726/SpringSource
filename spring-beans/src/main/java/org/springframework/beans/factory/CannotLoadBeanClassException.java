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

import org.springframework.beans.FatalBeanException;
import org.springframework.lang.Nullable;

/**
 * 当BeanFactory无法加载给定bean的指定类时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class CannotLoadBeanClassException extends FatalBeanException {

	@Nullable
	private final String resourceDescription;

	private final String beanName;

	@Nullable
	private final String beanClassName;


	/**
	 * 创建一个新的CannotLoadBeanClassException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param beanClassName bean类的名称
	 * @param cause 根本原因
	 */
	public CannotLoadBeanClassException(@Nullable String resourceDescription, String beanName,
			@Nullable String beanClassName, ClassNotFoundException cause) {

		super("Cannot find class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}

	/**
	 * 创建一个新的CannotLoadBeanClassException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param beanClassName bean类的名称
	 * @param cause 根本原因
	 */
	public CannotLoadBeanClassException(@Nullable String resourceDescription, String beanName,
			@Nullable String beanClassName, LinkageError cause) {

		super("Error loading class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") +
				": problem with class file or dependent class", cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}


	/**
	 * 返回bean定义来源资源的描述。
	 */
	@Nullable
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 返回请求的bean名称。
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回我们试图加载的类的名称。
	 */
	@Nullable
	public String getBeanClassName() {
		return this.beanClassName;
	}

}
