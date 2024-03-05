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

package org.springframework.core.env;

/**
 * 适用于“标准”（即非Web）应用程序的{@link Environment}实现。
 *
 * <p>除了{@link ConfigurableEnvironment}的常规功能（例如属性解析和与配置文件相关的操作）之外，
 * 此实现配置了两个默认属性源，按以下顺序搜索：
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() 系统属性}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
 * </ul>
 *
 * 也就是说，如果键“xyz”同时存在于JVM系统属性和当前进程的环境变量集中，
 * 则调用{@code environment.getProperty("xyz")}时将从系统属性中返回键“xyz”的值。
 * 默认选择此顺序是因为系统属性是每个JVM的，而环境变量可能是在给定系统上的许多JVM中相同的。
 * 给予系统属性的优先级允许在每个JVM基础上覆盖环境变量。
 *
 * <p>可以删除、重新排序或替换这些默认属性源，并且可以使用{@link MutablePropertySources}实例
 * （从{@link #getPropertySources()}中获得）添加其他属性源。有关用法示例，请参阅{@link ConfigurableEnvironment} Javadoc。
 *
 * <p>有关在shell环境（例如Bash）中处理属性名称的详细信息，请参阅{@link SystemEnvironmentPropertySource} Javadoc。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class StandardEnvironment extends AbstractEnvironment {

	/** 系统环境属性源名称：{@value}. */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM系统属性属性源名称：{@value}. */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * 使用默认的{@link MutablePropertySources}实例创建一个新的{@code StandardEnvironment}实例。
	 */
	public StandardEnvironment() {
	}

	/**
	 * 使用特定的{@link MutablePropertySources}实例创建一个新的{@code StandardEnvironment}实例。
	 * @param propertySources 要使用的属性源
	 * @since 5.3.4
	 */
	protected StandardEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * 自定义属性源集合，包括适用于任何标准Java环境的那些：
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}中存在的属性将优先于{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}中的属性。
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		// 将系统属性添加到属性源列表的末尾作为属性源
		propertySources.addLast(new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		// 将系统环境变量添加到属性源列表的末尾作为属性源
		propertySources.addLast(new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
