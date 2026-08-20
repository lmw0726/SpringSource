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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 可以定位任意数量 JNDI 对象的便捷超类。
 * 继承自 JndiAccessor，以继承 "jndiTemplate" 和 "jndiEnvironment" bean 属性。
 *
 * <p>JNDI 名称可以包含也可以不包含 Java EE 应用程序在访问本地映射的（ENC - 环境命名上下文）资源时
 * 所期望的 "java:comp/env/" 前缀。如果未包含，并且 "resourceRef" 属性为 true（默认值为
 * <strong>false</strong>）且未指定其他 scheme（如 "java:"），则会自动添加 "java:comp/env/" 前缀。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 */
public abstract class JndiLocatorSupport extends JndiAccessor {

	/** 在 Java EE 容器中使用的 JNDI 前缀。 */
	public static final String CONTAINER_PREFIX = "java:comp/env/";


	private boolean resourceRef = false;


	/**
	 * 设置查找是否在 Java EE 容器中进行，即如果 JNDI 名称不包含 "java:comp/env/" 前缀，
	 * 是否需要添加该前缀。默认值为 "false"。
	 * <p>注意：仅在未指定其他 scheme（如 "java:"）时才会生效。
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * 返回查找是否在 Java EE 容器中进行。
	 */
	public boolean isResourceRef() {
		return this.resourceRef;
	}


	/**
	 * 通过 JndiTemplate 对给定名称执行实际的 JNDI 查找。
	 * <p>如果名称不以 "java:comp/env/" 开头，且 "resourceRef" 设置为 "true"，
	 * 则会添加此前缀。
	 * @param jndiName 要查找的 JNDI 名称
	 * @return 获取到的对象
	 * @throws NamingException 如果 JNDI 查找失败
	 * @see #setResourceRef
	 */
	protected Object lookup(String jndiName) throws NamingException {
		return lookup(jndiName, null);
	}

	/**
	 * 通过 JndiTemplate 对给定名称执行实际的 JNDI 查找。
	 * <p>如果名称不以 "java:comp/env/" 开头，且 "resourceRef" 设置为 "true"，
	 * 则会添加此前缀。
	 * @param jndiName 要查找的 JNDI 名称
	 * @param requiredType 所需的对象类型
	 * @return 获取到的对象
	 * @throws NamingException 如果 JNDI 查找失败
	 * @see #setResourceRef
	 */
	protected <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		Assert.notNull(jndiName, "'jndiName' must not be null");
		String convertedName = convertJndiName(jndiName);
		T jndiObject;
		try {
			jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
		}
		catch (NamingException ex) {
			if (!convertedName.equals(jndiName)) {
				// 尝试回退到最初指定的名称...
				if (logger.isDebugEnabled()) {
					logger.debug("Converted JNDI name [" + convertedName +
							"] not found - trying original name [" + jndiName + "]. " + ex);
				}
				jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
			}
			else {
				throw ex;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Located object with JNDI name [" + convertedName + "]");
		}
		return jndiObject;
	}

	/**
	 * 将给定的 JNDI 名称转换为实际要使用的 JNDI 名称。
	 * <p>默认实现会在 "resourceRef" 为 "true" 且未指定其他 scheme（如 "java:"）时，
	 * 添加 "java:comp/env/" 前缀。
	 * @param jndiName 原始的 JNDI 名称
	 * @return 实际使用的 JNDI 名称
	 * @see #CONTAINER_PREFIX
	 * @see #setResourceRef
	 */
	protected String convertJndiName(String jndiName) {
		// 如果未指定容器前缀且未指定其他 scheme，则添加容器前缀。
		if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
			jndiName = CONTAINER_PREFIX + jndiName;
		}
		return jndiName;
	}

}
