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

/**
 * 表示当前应用程序运行环境的接口。
 * 模型化应用程序环境的两个关键方面：<em>profiles</em> 和 <em>properties</em>。
 * 与属性访问相关的方法通过 {@link PropertyResolver} 超接口公开。
 *
 * <p><em>profile</em> 是一组逻辑上命名的 bean 定义，只有在给定 profile 是 <em>active</em> 时才会向容器注册。
 * 无论是通过 XML 定义还是通过注解，都可以将 bean 分配给 profile；有关语法详细信息，请参阅 spring-beans 3.1 架构或
 * {@link org.springframework.context.annotation.Profile @Profile} 注解。{@code Environment} 对象与 profile
 * 的关系在于确定哪些（如果有）profile 是当前 {@linkplain #getActiveProfiles 激活的}，哪些（如果有）profile
 * 应该 {@linkplain #getDefaultProfiles 默认激活}。
 *
 * <p><em>properties</em> 在几乎所有应用程序中起着重要作用，并且可能来自各种来源：属性文件、JVM 系统属性、系统环境变量、
 * JNDI、Servlet 上下文参数、临时 Properties 对象、Maps 等等。{@code Environment} 对象与属性的关系是为用户提供
 * 一种方便的服务接口，用于配置属性源并从中解析属性。
 *
 * <p>在 {@code ApplicationContext} 中管理的 bean 可以注册为 {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware} 或 {@code @Inject} {@code Environment}，
 * 以便直接查询 profile 状态或解析属性。
 *
 * <p>然而，在大多数情况下，应用程序级别的 bean 不应直接与 {@code Environment} 交互，而是可能必须具有 {@code ${...}}
 * 属性值的 bean 替换为属性占位符配置器，例如 {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}，它本身是 {@code EnvironmentAware}，并且从 Spring 3.1 开始
 * 在使用 {@code <context:property-placeholder/>} 时默认注册。
 *
 * <p>必须通过 {@code ConfigurableEnvironment} 接口对 {@code Environment} 对象的配置进行。此接口是从所有
 * {@code AbstractApplicationContext} 子类的 {@code getEnvironment()} 方法返回的。有关在应用程序上下文
 * {@code refresh()} 之前操作属性源的使用示例，请参见 {@link ConfigurableEnvironment} Javadoc。
 *
 * @author Chris Beams
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 * @since 3.1
 */
public interface Environment extends PropertyResolver {

	/**
	 * 返回为此环境明确激活的 profile 集合。Profile 用于根据部署环境条件性地创建逻辑分组的 bean 定义。
	 * 可以通过设置 {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 * "spring.profiles.active"} 作为系统属性或通过调用 {@link ConfigurableEnvironment#setActiveProfiles(String...)}
	 * 来激活 profile。如果没有明确指定激活的 profile，则将自动激活任何 {@linkplain #getDefaultProfiles() 默认 profile}。
	 *
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * 返回在没有明确设置激活 profile 的情况下默认激活的 profile 集合。
	 *
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	String[] getDefaultProfiles();

	/**
	 * 返回一个或多个给定配置文件是否处于活动状态，或者在没有显式活动配置文件的情况下，返回一个或多个给定配置文件是否包含在默认配置文件集中。
	 * 如果配置文件以 “!” 开头，则逻辑是反向的，即如果给定的配置文件 <em>非<em> 激活的，则该方法将返回 {@code true}。
	 * 例如，如果配置文件 “p1” 处于活动状态或 “p2” 不处于活动状态，则 {@code env.acceptsProfiles (“p1”，“!p2”)} 将返回 {@code true}。
	 *
	 * @throws IllegalArgumentException 如果用零参数调用，或者，如果任何配置文件为 {@code null} 、空或仅空格
     * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 * @see #acceptsProfiles(Profiles)
	 * @deprecated as of 5.1 in favor of {@link #acceptsProfiles(Profiles)}
	 */
	@Deprecated
	boolean acceptsProfiles(String... profiles);

	/**
	 * 返回 {@linkplain #getActiveProfiles() 激活的 profiles} 是否与给定的 {@link Profiles} predicate 匹配。
	 */
	boolean acceptsProfiles(Profiles profiles);

}
