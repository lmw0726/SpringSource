/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link org.springframework.context.support.LiveBeansView} 的子类，用于查找 Web 应用程序中的所有 ApplicationContext，这些 ApplicationContext 在 ServletContext 属性中公开。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @deprecated 自 5.3 起，建议使用 Spring Boot 激活器来实现这样的需求
 */
@Deprecated
public class ServletContextLiveBeansView extends org.springframework.context.support.LiveBeansView {
	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;

	/**
	 * 为给定的 Servlet上下文 创建一个新的 Live Beans视图。
	 * @param servletContext 当前 ServletContext
	 */
	public ServletContextLiveBeansView(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}

	@Override
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		// 创建用于存储 可配置的应用程序上下文 的集合
		Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();
		// 获取 Servlet上下文 中的所有属性名的枚举
		Enumeration<String> attrNames = this.servletContext.getAttributeNames();
		// 遍历所有属性名
		while (attrNames.hasMoreElements()) {
			// 获取属性名和属性值
			String attrName = attrNames.nextElement();
			Object attrValue = this.servletContext.getAttribute(attrName);
			// 如果属性值是 可配置的应用程序上下文 类型，则添加到集合中
			if (attrValue instanceof ConfigurableApplicationContext) {
				contexts.add((ConfigurableApplicationContext) attrValue);
			}
		}
		// 返回存储 可配置的应用程序上下文 的集合
		return contexts;
	}

}
