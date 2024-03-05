/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * {@code Servlet}-based web应用程序使用的{@link Environment}实现。所有与web相关的（基于servlet的）{@code ApplicationContext}类默认初始化一个实例。
 *
 * <p>贡献{@code ServletConfig}、{@code ServletContext}和基于JNDI的{@link PropertySource}实例。有关详细信息，请参阅{@link #customizePropertySources}方法文档。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see StandardEnvironment
 * @since 3.1
 */
public class StandardServletEnvironment extends StandardEnvironment implements ConfigurableWebEnvironment {

	/**
	 * Servlet上下文初始化参数属性源名称：{@value}.
	 */
	public static final String SERVLET_CONTEXT_PROPERTY_SOURCE_NAME = "servletContextInitParams";

	/**
	 * Servlet配置初始化参数属性源名称：{@value}.
	 */
	public static final String SERVLET_CONFIG_PROPERTY_SOURCE_NAME = "servletConfigInitParams";

	/**
	 * JNDI属性源名称：{@value}.
	 */
	public static final String JNDI_PROPERTY_SOURCE_NAME = "jndiProperties";


	/**
	 * JDK 9+的JNDI API的防御性引用（可选的java.naming模块）
	 */
	private static final boolean jndiPresent = ClassUtils.isPresent(
			"javax.naming.InitialContext", StandardServletEnvironment.class.getClassLoader());


	/**
	 * 创建一个新的{@code StandardServletEnvironment}实例。
	 */
	public StandardServletEnvironment() {
	}

	/**
	 * 使用特定的{@link MutablePropertySources}实例创建一个新的{@code StandardServletEnvironment}实例。
	 *
	 * @param propertySources 要使用的属性源
	 * @since 5.3.4
	 */
	protected StandardServletEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * 自定义属性源集合，包括由超类贡献的属性源以及适用于标准基于servlet的环境的那些：
	 * <ul>
	 * <li>{@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME}
	 * <li>{@value #JNDI_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>{@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME}中存在的属性将优先于{@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME}中的属性，
	 * 并且在上述任何一个中找到的属性将优先于{@value #JNDI_PROPERTY_SOURCE_NAME}中的属性。
	 * <p>上述任何一个中的属性将优先于{@link StandardEnvironment}超类贡献的系统属性和环境变量。
	 * <p>{@code Servlet}-相关的属性源在此阶段被添加为{@link StubPropertySource stubs}，并且一旦实际的{@link ServletContext}对象可用，
	 * 将通过{@linkplain #initPropertySources(ServletContext, ServletConfig)完全初始化}。
	 *
	 * @see StandardEnvironment#customizePropertySources
	 * @see org.springframework.core.env.AbstractEnvironment#customizePropertySources
	 * @see ServletConfigPropertySource
	 * @see ServletContextPropertySource
	 * @see org.springframework.jndi.JndiPropertySource
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources
	 * @see #initPropertySources(ServletContext, ServletConfig)
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		// 添加模拟的 Servlet 配置属性源到属性源列表的末尾
		propertySources.addLast(new StubPropertySource(SERVLET_CONFIG_PROPERTY_SOURCE_NAME));
		// 添加模拟的 Servlet 上下文属性源到属性源列表的末尾
		propertySources.addLast(new StubPropertySource(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));
		// 如果 JNDI 存在并且默认的 JNDI 环境可用，则添加 JNDI 属性源到属性源列表的末尾
		if (jndiPresent && JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
			propertySources.addLast(new JndiPropertySource(JNDI_PROPERTY_SOURCE_NAME));
		}
		// 调用父类的 customizePropertySources 方法，执行其他自定义操作
		super.customizePropertySources(propertySources);
	}

	@Override
	public void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
		WebApplicationContextUtils.initServletPropertySources(getPropertySources(), servletContext, servletConfig);
	}

}
