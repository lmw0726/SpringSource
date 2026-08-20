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

package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;

/**
 * 基于 JNDI 的 {@link CustomizableThreadFactory} 变体，在 Java EE 7 环境中
 * 对 JSR-236 的 "java:comp/DefaultManagedThreadFactory" 执行默认查找，
 * 如果未找到则回退到本地 {@link CustomizableThreadFactory} 设置。
 *
 * <p>这是在 Java EE 7 环境中使用托管线程的便捷方式，在其他环境下则简单地使用
 * 常规本地线程——无需条件配置（即无需配置文件）。
 *
 * <p>注意：此类并非严格基于 JSR-236；它可以与 JNDI 中找到的任何常规
 * {@link java.util.concurrent.ThreadFactory} 配合使用。因此，默认的
 * JNDI 名称 "java:comp/DefaultManagedThreadFactory" 可以通过
 * {@link #setJndiName "jndiName"} bean 属性进行自定义。
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultManagedAwareThreadFactory extends CustomizableThreadFactory implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	@Nullable
	private String jndiName = "java:comp/DefaultManagedThreadFactory";

	@Nullable
	private ThreadFactory threadFactory;


	/**
	 * 设置用于 JNDI 查找的 JNDI 模板。
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiLocator.setJndiTemplate(jndiTemplate);
	}

	/**
	 * 设置用于 JNDI 查找的 JNDI 环境。
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiLocator.setJndiEnvironment(jndiEnvironment);
	}

	/**
	 * 设置查找是否在 Java EE 容器中进行，即如果 JNDI 名称尚未包含
	 * "java:comp/env/" 前缀，则需要添加该前缀。
	 * PersistenceAnnotationBeanPostProcessor 的默认值为 "true"。
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * 指定要委托的 {@link java.util.concurrent.ThreadFactory} 的 JNDI 名称，
	 * 替代默认的 JNDI 名称 "java:comp/DefaultManagedThreadFactory"。
	 * <p>这可以是完全限定的 JNDI 名称，也可以是在 "resourceRef" 设置为 "true"
	 * 时相对于当前环境命名上下文的 JNDI 名称。
	 * @see #setResourceRef
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			try {
				this.threadFactory = this.jndiLocator.lookup(this.jndiName, ThreadFactory.class);
			}
			catch (NamingException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to retrieve [" + this.jndiName + "] from JNDI", ex);
				}
				logger.info("Could not find default managed thread factory in JNDI - " +
						"proceeding with default local thread factory");
			}
		}
	}


	@Override
	public Thread newThread(Runnable runnable) {
		if (this.threadFactory != null) {
			return this.threadFactory.newThread(runnable);
		}
		else {
			return super.newThread(runnable);
		}
	}

}
