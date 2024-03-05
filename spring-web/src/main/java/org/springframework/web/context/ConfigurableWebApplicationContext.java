/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.context;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * 可配置的 Web 应用程序上下文需要实现的接口。
 * 由 {@link ContextLoader} 和 {@link org.springframework.web.servlet.FrameworkServlet} 支持。
 *
 * <p>注意：在从 {@link org.springframework.context.ConfigurableApplicationContext} 继承的 {@link #refresh} 方法调用之前，需要调用此接口的 setter 方法。
 * 它们本身不会导致上下文的初始化。
 *
 * @author Juergen Hoeller
 * @see #refresh
 * @see ContextLoader#createWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext
 * @since 05.12.2003
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

	/**
	 * ApplicationContext id 的前缀，用于引用上下文路径和/或 Servlet 名称。
	 */
	String APPLICATION_CONTEXT_ID_PREFIX = WebApplicationContext.class.getName() + ":";

	/**
	 * 工厂中 ServletConfig 环境 bean 的名称。
	 *
	 * @see javax.servlet.ServletConfig
	 */
	String SERVLET_CONFIG_BEAN_NAME = "servletConfig";


	/**
	 * 设置此 Web 应用程序上下文的 ServletContext。
	 * <p>不会导致上下文的初始化：在设置所有配置属性后需要调用 refresh。
	 *
	 * @see #refresh()
	 */
	void setServletContext(@Nullable ServletContext servletContext);

	/**
	 * 设置此 Web 应用程序上下文的 ServletConfig。
	 * 仅为属于特定 Servlet 的 WebApplicationContext 调用。
	 *
	 * @see #refresh()
	 */
	void setServletConfig(@Nullable ServletConfig servletConfig);

	/**
	 * 返回此 Web 应用程序上下文的 ServletConfig，如果有的话。
	 */
	@Nullable
	ServletConfig getServletConfig();

	/**
	 * 设置此 Web 应用程序上下文的命名空间，用于构建默认上下文配置位置。
	 * 根 Web 应用程序上下文没有命名空间。
	 */
	void setNamespace(@Nullable String namespace);

	/**
	 * 返回此 Web 应用程序上下文的命名空间，如果有的话。
	 */
	@Nullable
	String getNamespace();

	/**
	 * 以 init-param 样式设置此 Web 应用程序上下文的配置位置，即使用逗号、分号或空格分隔的不同位置。
	 * <p>如果未设置，实现应适当地为给定命名空间或根 Web 应用程序上下文使用默认值。
	 */
	void setConfigLocation(String configLocation);

	/**
	 * 设置此 Web 应用程序上下文的配置位置。
	 * <p>如果未设置，实现应适当地为给定命名空间或根 Web 应用程序上下文使用默认值。
	 */
	void setConfigLocations(String... configLocations);

	/**
	 * 返回此 Web 应用程序上下文的配置位置，如果未指定则返回 {@code null}。
	 */
	@Nullable
	String[] getConfigLocations();

}
