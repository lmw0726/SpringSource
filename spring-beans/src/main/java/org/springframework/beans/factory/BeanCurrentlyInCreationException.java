/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * 当引用一个正在创建中的 Bean 时抛出的异常。
 * 这种情况通常发生在构造函数自动装配时，装配到了当前正在构造的 Bean。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class BeanCurrentlyInCreationException extends BeanCreationException {

	/**
	 * 创建一个新的 BeanCurrentlyInCreationException，
	 * 并带有默认的错误消息，提示存在循环引用。
	 * @param beanName 被请求的 Bean 名称
	 */
	public BeanCurrentlyInCreationException(String beanName) {
		super(beanName,
				"Requested bean is currently in creation: Is there an unresolvable circular reference?");
	}

	/**
	 * 创建一个新的 BeanCurrentlyInCreationException。
	 * @param beanName 被请求的 Bean 名称
	 * @param msg 详细信息
	 */
	public BeanCurrentlyInCreationException(String beanName, String msg) {
		super(beanName, msg);
	}

}
