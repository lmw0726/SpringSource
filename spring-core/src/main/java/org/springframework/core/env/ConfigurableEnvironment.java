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

package org.springframework.core.env;

import java.util.Map;

/**
 * 大多数（如果不是所有）{@link Environment}类型都要实现的配置接口。
 * 提供了设置活动profile和默认profile、操作底层属性源等功能。通过{@link ConfigurablePropertyResolver}超接口，
 * 客户端可以设置和验证必需的属性、自定义转换服务等。
 *
 * <h2>操作属性源</h2>
 * <p>可以通过从{@link #getPropertySources()}返回的{@link MutablePropertySources}实例来移除、重新排序或替换属性源；
 * 并且可以添加额外的属性源。以下示例针对{@code ConfigurableEnvironment}的{@link StandardEnvironment}实现，
 * 但通常适用于任何实现，尽管特定的默认属性源可能会有所不同。
 *
 * <h4>示例：添加具有最高搜索优先级的新属性源</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>示例：移除默认系统属性属性源</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>示例：为测试目的模拟系统环境</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * 当{@link Environment}被{@code ApplicationContext}使用时，任何此类{@code PropertySource}操作都必须在调用上下文的
 * {@link org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}方法之前执行。
 * 这样可以确保在容器引导过程中所有属性源都可用，包括由{@linkplain org.springframework.context.support.PropertySourcesPlaceholderConfigurer property
 * placeholder configurers}使用。
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * 指定此{@code Environment}的激活profile集合。在容器引导期间，将评估profile以确定是否应将bean定义注册到容器中。
	 * <p>任何现有的激活profile都将替换为给定的参数；使用零个参数调用以清除当前的激活profile集。使用{@link #addActiveProfile}来添加一个profile，
	 * 同时保留现有的集合。
	 *
	 * @throws IllegalArgumentException 如果任何profile为null、空或仅包含空格
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * 将profile添加到当前的激活profile集合中。
	 *
	 * @throws IllegalArgumentException 如果profile为null、空或仅包含空格
	 * @see #setActiveProfiles
	 */
	void addActiveProfile(String profile);

	/**
	 * 指定在没有通过{@link #setActiveProfiles}显式激活其他profile的情况下，要默认激活的profile集合。
	 *
	 * @throws IllegalArgumentException 如果任何profile为null、空或仅包含空格
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * 以可变形式返回此{@code Environment}的{@link PropertySources}，
	 * 允许对应该{@code Environment}对象解析属性时搜索的{@link PropertySource}对象集进行操作。
	 * {@link MutablePropertySources}的各种方法（如{@link MutablePropertySources#addFirst addFirst}、
	 * {@link MutablePropertySources#addLast addLast}、{@link MutablePropertySources#addBefore addBefore}和
	 * {@link MutablePropertySources#addAfter addAfter}）允许对属性源排序进行细粒度控制。
	 * 例如，确保某些用户定义的属性源具有比默认属性源（例如一组系统属性或一组系统环境变量）更高的搜索优先级。
	 *
	 * @see AbstractEnvironment#customizePropertySources
	 */
	MutablePropertySources getPropertySources();

	/**
	 * 如果当前{@link SecurityManager}允许，返回{@link System#getProperties()}的值，否则返回一个Map实现，
	 * 该实现将尝试使用调用{@link System#getProperty(String)}来访问各个键。
	 *
	 * <p>请注意，大多数{@code Environment}实现都将此系统属性映射作为默认的{@link PropertySource}，
	 * 以便进行搜索。因此，除非明确打算绕过其他属性源，否则建议不直接使用此方法。
	 *
	 * <p>对返回的Map调用{@link Map#get(Object)}将永远不会抛出{@link IllegalAccessException}；
	 * 在安全管理器禁止访问属性的情况下，将返回{@code null}并发出INFO级别的日志消息，注明异常。
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * 如果当前{@link SecurityManager}允许，则返回{@link System#getenv()}的值，否则返回一个Map实现，
	 * 该实现将尝试使用调用{@link System#getenv(String)}来访问各个键。
	 *
	 * <p>请注意，大多数{@link Environment}实现都将此系统环境映射作为默认的{@link PropertySource}，
	 * 以便进行搜索。因此，除非明确打算绕过其他属性源，否则建议不直接使用此方法。
	 *
	 * <p>对返回的Map调用{@link Map#get(Object)}将永远不会抛出{@link IllegalAccessException}；
	 * 在安全管理器禁止访问属性的情况下，将返回{@code null}并发出INFO级别的日志消息，注明异常。
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * 将给定父环境的活动profile、默认profile和属性源追加到此（子）环境的相应集合中。
	 *
	 * <p>对于在父环境和子环境中都存在的同名{@code PropertySource}实例，将保留子实例并丢弃父实例。
	 * 这样可以允许子级通过覆盖属性源来覆盖父级，同时避免通过常见属性源类型（例如系统环境和系统属性）进行冗余搜索。
	 *
	 * <p>活动profile和默认profile名称也会过滤重复项，以避免混淆和冗余存储。
	 *
	 * <p>父环境在任何情况下都保持不变。请注意，在调用{@code merge}之后发生对父环境的任何更改都不会反映在子级中。
	 * 因此，在调用{@code merge}之前应谨慎配置父属性源和profile信息。
	 *
	 * @param parent 要合并的环境
	 * @since 3.1.2
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 */
	void merge(ConfigurableEnvironment parent);

}
