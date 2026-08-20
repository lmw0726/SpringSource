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

import javax.naming.NamingException;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * 从底层 Spring {@link JndiLocatorDelegate} 读取属性的 {@link PropertySource} 实现。
 *
 * <p>默认情况下，底层的 {@code JndiLocatorDelegate} 会将其
 * {@link JndiLocatorDelegate#setResourceRef(boolean) "resourceRef"} 属性设置为
 * {@code true}，这意味着查找的名称将自动添加 "java:comp/env/" 前缀，
 * 以符合已发布的
 * <a href="https://download.oracle.com/javase/jndi/tutorial/beyond/misc/policy.html">JNDI
 * 命名规范</a>。要覆盖此设置或更改前缀，请手动配置
 * {@code JndiLocatorDelegate} 并将其传递给此处接受它的某个构造函数。
 * 提供自定义 JNDI 属性时同样适用，应在构造 {@code JndiPropertySource} 之前，
 * 使用 {@link JndiLocatorDelegate#setJndiEnvironment(java.util.Properties)}
 * 进行指定。
 *
 * <p>请注意，{@link org.springframework.web.context.support.StandardServletEnvironment
 * StandardServletEnvironment} 默认包含一个 {@code JndiPropertySource}，
 * 对底层 {@link JndiLocatorDelegate} 的任何自定义都可以在
 * {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} 或
 * {@link org.springframework.web.WebApplicationInitializer
 * WebApplicationInitializer} 中进行。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see JndiLocatorDelegate
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.web.WebApplicationInitializer
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class JndiPropertySource extends PropertySource<JndiLocatorDelegate> {

	/**
	 * 使用给定的名称创建一个新的 {@code JndiPropertySource}，
	 * 并配置一个 {@link JndiLocatorDelegate}，该委托会为所有名称添加
	 * "java:comp/env/" 前缀。
	 */
	public JndiPropertySource(String name) {
		this(name, JndiLocatorDelegate.createDefaultResourceRefLocator());
	}

	/**
	 * 使用给定的名称和给定的 {@code JndiLocatorDelegate} 创建一个新的
	 * {@code JndiPropertySource}。
	 */
	public JndiPropertySource(String name, JndiLocatorDelegate jndiLocator) {
		super(name, jndiLocator);
	}


	/**
	 * 此实现从底层 {@link JndiLocatorDelegate} 查找并返回与给定名称关联的值。
	 * 如果在调用 {@link JndiLocatorDelegate#lookup(String)} 期间抛出
	 * {@link NamingException}，则返回 {@code null} 并输出一条 DEBUG 级别的日志，
	 * 包含异常消息。
	 */
	@Override
	@Nullable
	public Object getProperty(String name) {
		if (getSource().isResourceRef() && name.indexOf(':') != -1) {
			// 当前处于 resource-ref 模式（以 "java:comp/env" 作为前缀）。无需处理
			// 包含冒号的属性名，因为它们可能只是包含默认值子句，
			// 即使在文本属性源中也不太可能匹配到包含冒号的部分，
			// 而且在 JNDI 中冒号表示 JNDI 方案与实际名称之间的分隔符，
			// 因此这种方式永远不会匹配。
			return null;
		}

		try {
			Object value = this.source.lookup(name);
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] returned: [" + value + "]");
			}
			return value;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] threw NamingException " +
						"with message: " + ex.getMessage() + ". Returning null.");
			}
			return null;
		}
	}

}
