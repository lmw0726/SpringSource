/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个构造函数、字段、setter 方法或配置方法由 Spring 的依赖注入设施自动装配。
 * 这是 JSR-330 的 {@link javax.inject.Inject} 注解的替代方案，并增加了必需与可选的语义。
 *
 * <h3>自动装配构造函数</h3>
 * <p>任意给定 Bean 类中，只有一个构造函数可以声明此注解且 {@link #required} 属性为 {@code true}，
 * 表示在作为 Spring Bean 使用时需要自动装配的<i>该</i>构造函数。
 * 如果 {@code required} 属性为 {@code true}，则只能有一个构造函数被 {@code @Autowired} 注解。
 * 如果多个 <i>非必需</i> 构造函数声明了该注解，它们将被视为自动装配候选。
 * 容器中可以满足依赖最多的构造函数将被选择。如果没有候选构造函数的依赖能被满足，则使用主/默认构造函数（如果存在）。
 * 同样，如果类声明了多个构造函数但没有任何一个被注解为 {@code @Autowired}，则使用主/默认构造函数（如果存在）。
 * 如果类只声明了一个构造函数，则始终使用该构造函数，即使未被注解。被注解的构造函数不必是 public。
 *
 * <h3>自动装配字段</h3>
 * <p>字段在 Bean 构造完成后、配置方法调用前被注入。此类配置字段不必是 public。
 *
 * <h3>自动装配方法</h3>
 * <p>配置方法可以具有任意名称和任意数量的参数；每个参数都将从 Spring 容器中自动装配匹配的 Bean。
 * Bean 属性的 setter 方法实际上只是这种通用配置方法的特殊情况。此类配置方法不必是 public。
 *
 * <h3>自动装配参数</h3>
 * <p>虽然从 Spring Framework 5.0 开始，{@code @Autowired} 可以声明在单个方法或构造函数参数上，但框架大多数部分会忽略这种声明。
 * 核心 Spring 框架中唯一主动支持自动装配参数的是 {@code spring-test} 模块中的 JUnit Jupiter 支持
 * （详情参见
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">TestContext 框架</a>文档）。
 *
 * <h3>多参数与 'required' 语义</h3>
 * <p>对于多参数构造函数或方法，{@link #required} 属性适用于所有参数。
 * 单个参数可以声明为 Java 8 风格的 {@link java.util.Optional}，
 * 或从 Spring Framework 5.0 起使用 {@code @Nullable} 或 Kotlin 的非空类型参数，从而覆盖基础的 'required' 语义。
 *
 * <h3>自动装配数组、集合与 Map</h3>
 * <p>对于数组、{@link java.util.Collection} 或 {@link java.util.Map} 类型的依赖，容器会自动装配所有匹配声明值类型的 Bean。
 * 对于 Map 类型，键必须声明为 {@code String} 类型，容器会将其解析为对应的 Bean 名称。
 * 容器提供的集合会按顺序排列，考虑到目标组件的
 * {@link org.springframework.core.Ordered Ordered} 与 {@link org.springframework.core.annotation.Order @Order} 值，
 * 否则按照在容器中的注册顺序。
 * 或者，单个匹配的目标 Bean 也可以是通用类型的 {@code Collection} 或 {@code Map} 本身，被注入为整体。
 *
 * <h3>在 {@code BeanPostProcessor} 或 {@code BeanFactoryPostProcessor} 中不支持</h3>
 * <p>实际注入是通过 {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 执行的，
 * 因此你<em>不能</em>在 {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor} 类型中使用 {@code @Autowired} 注入引用。
 * 详情请参阅 {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor} 的 javadoc（默认情况下会检查此注解的存在）。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * 声明被注解的依赖是否为必需。
	 * <p>默认值为 {@code true}。
	 */
	boolean required() default true;

}
