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
 * 当 BeanFactory 遇到无效的 Bean 定义时抛出的异常：
 * 例如不完整或相互矛盾的 Bean 元数据。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class BeanDefinitionStoreException extends FatalBeanException {

	@Nullable
	private final String resourceDescription;

	@Nullable
	private final String beanName;


	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param msg 详细信息（直接作为异常消息使用）
	 */
	public BeanDefinitionStoreException(String msg) {
		super(msg);
		this.resourceDescription = null;
		this.beanName = null;
	}

	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param msg 详细信息（直接作为异常消息使用）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public BeanDefinitionStoreException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.resourceDescription = null;
		this.beanName = null;
	}

	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param msg 详细信息（直接作为异常消息使用）
	 */
	public BeanDefinitionStoreException(@Nullable String resourceDescription, String msg) {
		super(msg);
		this.resourceDescription = resourceDescription;
		this.beanName = null;
	}

	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param msg 详细信息（直接作为异常消息使用）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public BeanDefinitionStoreException(@Nullable String resourceDescription, String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.resourceDescription = resourceDescription;
		this.beanName = null;
	}

	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param beanName Bean 的名称
	 * @param msg 详细信息（会附加在提示资源和 Bean 名称的介绍性消息之后）
	 */
	public BeanDefinitionStoreException(@Nullable String resourceDescription, String beanName, String msg) {
		this(resourceDescription, beanName, msg, null);
	}

	/**
	 * 创建一个新的 BeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param beanName Bean 的名称
	 * @param msg 详细信息（会附加在提示资源和 Bean 名称的介绍性消息之后）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public BeanDefinitionStoreException(
			@Nullable String resourceDescription, String beanName, String msg, @Nullable Throwable cause) {

		super("Invalid bean definition with name '" + beanName + "' defined in " + resourceDescription + ": " + msg,
				cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
	}


	/**
	 * 返回 Bean 定义来源资源的描述（如果可用）。
	 */
	@Nullable
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 返回 Bean 的名称（如果可用）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

}
