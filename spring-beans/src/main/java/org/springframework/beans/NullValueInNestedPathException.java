/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * 当导航有效的嵌套属性路径时遇到NullPointerException时抛出的异常。
 *
 * <p>例如，导航"spouse.age"可能会失败，因为目标对象的spouse属性值为null。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class NullValueInNestedPathException extends InvalidPropertyException {

	/**
	 * 创建新的NullValueInNestedPathException。
	 * @param beanClass 有问题的Bean类
	 * @param propertyName 有问题的属性
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName, "Value of nested property '" + propertyName + "' is null");
	}

	/**
	 * 创建新的NullValueInNestedPathException。
	 * @param beanClass 有问题的Bean类
	 * @param propertyName 有问题的属性
	 * @param msg 详细消息
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * 创建新的NullValueInNestedPathException。
	 * @param beanClass 有问题的Bean类
	 * @param propertyName 有问题的属性
	 * @param msg 详细消息
	 * @param cause 根本原因
	 * @since 4.3.2
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

}
