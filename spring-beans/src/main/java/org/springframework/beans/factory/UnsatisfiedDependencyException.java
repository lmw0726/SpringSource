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
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 当bean依赖于其他bean或简单属性，但这些依赖项未在bean工厂定义中指定时抛出的异常，
 * 尽管依赖检查已启用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 03.09.2003
 */
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends BeanCreationException {

	@Nullable
	private final InjectionPoint injectionPoint;


	/**
	 * 创建一个新的UnsatisfiedDependencyException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param propertyName 无法满足的bean属性名称
	 * @param msg 详细消息
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = null;
	}

	/**
	 * 创建一个新的UnsatisfiedDependencyException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param propertyName 无法满足的bean属性名称
	 * @param ex 表示不满足依赖关系的bean创建异常
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, BeansException ex) {

		this(resourceDescription, beanName, propertyName, "");
		initCause(ex);
	}

	/**
	 * 创建一个新的UnsatisfiedDependencyException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param injectionPoint 注入点（字段或方法/构造函数参数）
	 * @param msg 详细消息
	 * @since 4.3
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through " + injectionPoint +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = injectionPoint;
	}

	/**
	 * 创建一个新的UnsatisfiedDependencyException。
	 * @param resourceDescription bean定义来源资源的描述
	 * @param beanName 请求的bean名称
	 * @param injectionPoint 注入点（字段或方法/构造函数参数）
	 * @param ex 表示不满足依赖关系的bean创建异常
	 * @since 4.3
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, BeansException ex) {

		this(resourceDescription, beanName, injectionPoint, "");
		initCause(ex);
	}


	/**
	 * 返回注入点（字段或方法/构造函数参数），如果已知的话。
	 * @since 4.3
	 */
	@Nullable
	public InjectionPoint getInjectionPoint() {
		return this.injectionPoint;
	}

}
