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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * 当引用无效的 bean 属性时抛出的异常。
 * 携带出错的 bean 类和属性名信息。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
@SuppressWarnings("serial")
public class InvalidPropertyException extends FatalBeanException {

	private final Class<?> beanClass;

	private final String propertyName;


	/**
	 * 创建一个新的 InvalidPropertyException。
	 * @param beanClass 出错的 bean 类
	 * @param propertyName 出错的属性名
	 * @param msg 详细消息
	 */
	public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
		this(beanClass, propertyName, msg, null);
	}

	/**
	 * 创建一个新的 InvalidPropertyException。
	 * @param beanClass 出错的 bean 类
	 * @param propertyName 出错的属性名
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, @Nullable Throwable cause) {
		super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
		this.propertyName = propertyName;
	}

	/**
	 * 返回出错的 bean 类。
	 */
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	/**
	 * 返回出错的属性名。
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

}
