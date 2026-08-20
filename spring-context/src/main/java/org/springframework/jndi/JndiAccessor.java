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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * JNDI 访问器的便捷超类，提供 "jndiTemplate"
 * 和 "jndiEnvironment" bean 属性。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 */
public class JndiAccessor {

	/**
	 * 日志记录器，供子类使用。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private JndiTemplate jndiTemplate = new JndiTemplate();


	/**
	 * 设置用于 JNDI 查找的 JNDI 模板。
	 * <p>也可以通过 "jndiEnvironment" 指定 JNDI 环境设置。
	 * @see #setJndiEnvironment
	 */
	public void setJndiTemplate(@Nullable JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * 返回用于 JNDI 查找的 JNDI 模板。
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * 设置用于 JNDI 查找的 JNDI 环境。
	 * <p>使用给定的环境设置创建一个 JndiTemplate。
	 * @see #setJndiTemplate
	 */
	public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 返回用于 JNDI 查找的 JNDI 环境。
	 */
	@Nullable
	public Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}

}
