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

package org.springframework.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;

/**
 * {@link JndiLocatorSupport} 的子类，提供了公共的查找方法，
 * 便于作为委托使用。
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 */
public class JndiLocatorDelegate extends JndiLocatorSupport {

	/**
	 * 系统属性，指示 Spring 忽略默认的 JNDI 环境，即
	 * {@link #isDefaultJndiEnvironmentAvailable()} 始终返回 {@code false}。
	 * <p>默认值为 "false"，允许在例如 {@link JndiPropertySource} 中进行常规的默认 JNDI 访问。
	 * 将此标志切换为 {@code true} 是一种优化，适用于此类 JNDI 回退搜索本来就不会找到任何内容的场景，
	 * 从而避免重复的 JNDI 查找开销。
	 * <p>请注意，此标志仅影响 JNDI 回退搜索，不影响显式配置的 JNDI 查找，
	 * 例如 {@code DataSource} 或其他环境资源的查找。
	 * 该标志实际上只影响基于 {@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()}
	 * 检查而尝试进行 JNDI 搜索的代码：特别是
	 * {@code StandardServletEnvironment} 和 {@code StandardPortletEnvironment}。
	 * @since 4.3
	 * @see #isDefaultJndiEnvironmentAvailable()
	 * @see JndiPropertySource
	 */
	public static final String IGNORE_JNDI_PROPERTY_NAME = "spring.jndi.ignore";


	private static final boolean shouldIgnoreDefaultJndiEnvironment =
			SpringProperties.getFlag(IGNORE_JNDI_PROPERTY_NAME);


	@Override
	public Object lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName);
	}

	@Override
	public <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		return super.lookup(jndiName, requiredType);
	}


	/**
	 * 创建一个 {@code JndiLocatorDelegate} 实例，并将其 "resourceRef" 属性设置为
	 * {@code true}，这意味着所有名称都将添加 "java:comp/env/" 前缀。
	 * @see #setResourceRef
	 */
	public static JndiLocatorDelegate createDefaultResourceRefLocator() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		return jndiLocator;
	}

	/**
	 * 检查此 JVM 上是否可用默认的 JNDI 环境，即 Java EE 环境中的默认 JNDI 环境。
	 * @return 如果可以使用默认的 InitialContext 则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	public static boolean isDefaultJndiEnvironmentAvailable() {
		if (shouldIgnoreDefaultJndiEnvironment) {
			return false;
		}
		try {
			new InitialContext().getEnvironment();
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
