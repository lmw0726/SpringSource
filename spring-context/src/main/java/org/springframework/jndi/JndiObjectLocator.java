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

package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于 JNDI 的服务定位器的便捷超类，
 * 提供对特定 JNDI 资源的可配置查找。
 *
 * <p>公开了一个 {@link #setJndiName "jndiName"} 属性。此属性可能包含也可能不包含
 * Java EE 应用程序在访问本地映射（环境命名上下文）资源时所需的 "java:comp/env/" 前缀。
 * 如果不包含，并且 "resourceRef" 属性为 true（默认值为 <strong>false</strong>），
 * 且未指定其他方案（例如 "java:"），则会自动添加 "java:comp/env/" 前缀。
 *
 * <p>子类可以在适当的任何时候调用 {@link #lookup()} 方法。
 * 某些类可能在初始化时执行此操作，而其他类可能按需执行。
 * 后一种策略更灵活，因为它允许在 JNDI 对象可用之前初始化定位器。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiName
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 * @see #lookup()
 */
public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

	@Nullable
	private String jndiName;

	@Nullable
	private Class<?> expectedType;


	/**
	 * 指定要查找的 JNDI 名称。如果名称不以 "java:comp/env/" 开头，
	 * 并且 "resourceRef" 设置为 "true"，则会自动添加此前缀。
	 * @param jndiName 要查找的 JNDI 名称
	 * @see #setResourceRef
	 */
	public void setJndiName(@Nullable String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * 返回要查找的 JNDI 名称。
	 */
	@Nullable
	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * 指定定位到的 JNDI 对象应该可以赋值给的类型（如果有）。
	 */
	public void setExpectedType(@Nullable Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * 返回定位到的 JNDI 对象应该可以赋值给的类型（如果有）。
	 */
	@Nullable
	public Class<?> getExpectedType() {
		return this.expectedType;
	}

	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		if (!StringUtils.hasLength(getJndiName())) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}
	}


	/**
	 * 执行实际的 JNDI 查找，以获取此定位器的目标资源。
	 * @return 定位到的目标对象
	 * @throws NamingException 如果 JNDI 查找失败或定位到的 JNDI 对象
	 * 无法赋值给期望的类型
	 * @see #setJndiName
	 * @see #setExpectedType
	 * @see #lookup(String, Class)
	 */
	protected Object lookup() throws NamingException {
		String jndiName = getJndiName();
		Assert.state(jndiName != null, "No JNDI name specified");
		return lookup(jndiName, getExpectedType());
	}

}
