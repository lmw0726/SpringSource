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

package org.springframework.context.annotation;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.lang.annotation.*;

/**
 * 表示当一个或多个指定的激活配置文件处于活动状态时，组件是可注册的。
 *
 * <p>一个<em>配置文件</em>是一个命名的逻辑分组，可以通过编程方式使用 {@link ConfigurableEnvironment#setActiveProfiles} 激活，
 * 或通过在 {@code web.xml} 中设置 {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME spring.profiles.active}
 * 作为 JVM 系统属性、环境变量或 Servlet 上下文参数来声明。
 * 配置文件也可以通过集成测试中的 {@code @ActiveProfiles} 注解来声明。
 *
 * <p>{@code @Profile} 注解可以以以下任何方式使用：
 * <ul>
 * <li>作为任何直接或间接注解了 {@code @Component} 的类的类型级别注解，包括 {@link Configuration @Configuration} 类</li>
 * <li>作为元注解，用于组合自定义构造型注解</li>
 * <li>作为任何 {@link Bean @Bean} 方法的方法级别注解</li>
 * </ul>
 *
 * <p>如果一个 {@code @Configuration} 类标记了 {@code @Profile}，那么与该类相关联的所有 {@code @Bean} 方法和 {@link Import @Import}
 * 注解将被绕过，除非指定的一个或多个配置文件处于活动状态。配置文件字符串可以包含一个简单的配置文件名（例如 {@code "p1"}）或一个配置文件表达式。
 * 配置文件表达式允许表达更复杂的配置文件逻辑，例如 {@code "p1 & p2"}。有关支持的格式的详细信息，请参阅 {@link Profiles#of(String...)}。
 *
 * <p>这类似于在 Spring XML 中的行为：如果 {@code beans} 元素的 {@code profile} 属性已经被提供，例如，{@code <beans profile="p1,p2">}，
 * 那么除非至少激活了配置文件 'p1' 或 'p2'，否则 {@code beans} 元素将不会被解析。同样地，如果一个 {@code @Component} 或 {@code @Configuration}
 * 类被标记为 {@code @Profile({"p1", "p2"})}，那么除非至少激活了配置文件 'p1' 或 'p2'，否则该类将不会被注册或处理。
 *
 * <p>如果给定的配置文件以 NOT 运算符（{@code !}）开头，那么如果该配置文件 <em>不</em> 处于活动状态，将会注册被注解的组件 —— 例如，
 * 给定 {@code @Profile({"p1", "!p2"})}，如果配置文件 'p1' 处于活动状态或配置文件 'p2' <em>不</em> 处于活动状态，则会发生注册。
 *
 * <p>如果省略了 {@code @Profile} 注解，那么无论哪个（如果有的话）配置文件处于活动状态，都会发生注册。
 *
 * <p><b>注意：</b> 在 {@code @Bean} 方法上使用 {@code @Profile} 时，可能会存在一个特殊的情况：对于同一个 Java 方法名称的重载 {@code @Bean} 方法
 * （类似于构造函数重载），需要在所有重载方法上一致地声明一个 {@code @Profile} 条件。如果条件不一致，那么只有在重载方法中的第一个声明上的条件才会生效。
 * 因此，{@code @Profile} 不能用于选择具有特定参数签名的重载方法。在创建时，相同 bean 的所有工厂方法之间的解析都遵循 Spring 的构造函数解析算法。
 * <b>如果要定义具有不同配置文件条件的替代 bean，请使用指向相同 {@link Bean#name bean 名称} 的不同 Java 方法名称；请参见
 * {@link Configuration @Configuration} 的 javadoc 中的 {@code ProfileDatabaseConfig}。</b>
 *
 * <p>在 XML 中通过 {@code <beans>} 元素的 {@code "profile"} 属性可以定义 Spring bean。有关详细信息，请参阅
 * {@code spring-beans} XSD（版本 3.1 或更高版本）中的文档。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see ConfigurableEnvironment#setActiveProfiles
 * @see ConfigurableEnvironment#setDefaultProfiles
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
 * @see Conditional
 * @see org.springframework.test.context.ActiveProfiles
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

	/**
	 * 注册组件所需的配置文件集。
	 */
	String[] value();

}
