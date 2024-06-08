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

package org.springframework.web.context.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

/**
 * {@link FactoryBean}，用于获取特定的现有 ServletContext 属性。
 * 在用作 bean 引用时暴露该 ServletContext 属性，从而有效地将其作为命名的 Spring bean 实例可用。
 *
 * <p>旨在链接在 Spring 应用程序上下文启动之前存在的 ServletContext 属性。
 * 通常，这些属性将由第三方 Web 框架放置在那里。
 * 在纯粹基于 Spring 的 Web 应用程序中，将不需要链接 ServletContext 属性。
 *
 * <p><b>注意:</b> 自 Spring 3.0 以来，您还可以使用类型为 Map 的 "contextAttributes" 默认 bean，
 * 并使用 "#{contextAttributes.myKey}" 表达式对其进行解引用以按名称访问特定属性。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.WebApplicationContext#CONTEXT_ATTRIBUTES_BEAN_NAME
 * @see ServletContextParameterFactoryBean
 * @since 1.1.4
 */
public class ServletContextAttributeFactoryBean implements FactoryBean<Object>, ServletContextAware {
	/**
	 * 属性名
	 */
	@Nullable
	private String attributeName;
	/**
	 * 属性值
	 */
	@Nullable
	private Object attribute;


	/**
	 * 设置要暴露的 ServletContext 属性的名称。
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		// 如果属性名称为 null，则抛出 IllegalArgumentException 异常
		if (this.attributeName == null) {
			throw new IllegalArgumentException("Property 'attributeName' is required");
		}

		// 从 Servlet上下文 获取指定名称的属性
		this.attribute = servletContext.getAttribute(this.attributeName);

		// 如果获取到的属性为 null，则抛出 IllegalStateException 异常
		if (this.attribute == null) {
			throw new IllegalStateException("No ServletContext attribute '" + this.attributeName + "' found");
		}
	}


	@Override
	@Nullable
	public Object getObject() throws Exception {
		return this.attribute;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.attribute != null ? this.attribute.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
