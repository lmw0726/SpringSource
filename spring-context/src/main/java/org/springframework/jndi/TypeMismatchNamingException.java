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

package org.springframework.jndi;

import javax.naming.NamingException;

/**
 * 当JNDI环境中定位到的对象出现类型不匹配时抛出此异常。
 * 由JndiTemplate抛出。
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 * @see JndiTemplate#lookup(String, Class)
 */
@SuppressWarnings("serial")
public class TypeMismatchNamingException extends NamingException {

	private final Class<?> requiredType;

	private final Class<?> actualType;


	/**
	 * 构造一个新的TypeMismatchNamingException，
	 * 根据给定参数构建说明文本。
	 * @param jndiName JNDI名称
	 * @param requiredType 查找所需的类型
	 * @param actualType 查找返回的实际类型
	 */
	public TypeMismatchNamingException(String jndiName, Class<?> requiredType, Class<?> actualType) {
		super("Object of type [" + actualType + "] available at JNDI location [" +
				jndiName + "] is not assignable to [" + requiredType.getName() + "]");
		this.requiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * 返回查找所需的类型（如果可用）。
	 */
	public final Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * 返回查找返回的实际类型（如果可用）。
	 */
	public final Class<?> getActualType() {
		return this.actualType;
	}

}
