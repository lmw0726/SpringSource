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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.filter.TypeFilter;

/**
 * 配置组件扫描指令，用于与 @{@link Configuration} 类配合使用。
 * 提供与 Spring XML 的 {@code <context:component-scan>} 元素并行的支持。
 *
 * <p>可以指定 {@link #basePackageClasses} 或 {@link #basePackages}（或其别名
 * {@link #value}）来定义要扫描的特定包。如果未定义特定包，
 * 则将从此注解声明所在的包开始扫描。
 *
 * <p>请注意，{@code <context:component-scan>} 元素具有
 * {@code annotation-config} 属性；但是，此注解没有。这是因为
 * 在使用 {@code @ComponentScan} 的几乎所有情况下，都假定使用默认注解配置
 * 处理（例如处理 {@code @Autowired} 等）。此外，
 * 在使用 {@link AnnotationConfigApplicationContext} 时，注解配置处理器
 * 始终会注册，这意味着在 {@code @ComponentScan} 级别禁用它们的任何尝试
 * 都将被忽略。
 *
 * <p>使用示例请参阅 {@link Configuration @Configuration} 的 Javadoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see Configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {

	/**
	 * {@link #basePackages} 的别名。
	 * <p>如果不需要其他属性，可以更简洁地声明注解，
	 * 例如 {@code @ComponentScan("org.my.pkg")}
	 * 而不是 {@code @ComponentScan(basePackages = "org.my.pkg")}。
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 要扫描注解组件的基础包。
	 * <p>{@link #value} 是此属性的别名（与之互斥）。
	 * <p>如果要使用类型安全的方式代替基于字符串的包名，
	 * 请使用 {@link #basePackageClasses}。
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * {@link #basePackages} 的类型安全替代方案，用于指定要扫描注解组件的包。
	 * 指定的每个类所在的包都将被扫描。
	 * <p>考虑在每个包中创建一个特殊的无操作标记类或接口，
	 * 该类或接口除被此属性引用外无其他用途。
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * 用于在 Spring 容器中命名检测到的组件的 {@link BeanNameGenerator} 类。
	 * <p>{@link BeanNameGenerator} 接口本身的默认值表示
	 * 用于处理此 {@code @ComponentScan} 注解的扫描器应
	 * 使用其继承的 bean 名称生成器，例如默认的
	 * {@link AnnotationBeanNameGenerator} 或在引导时提供的任何自定义实例。
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * 用于解析检测到的组件的作用域的 {@link ScopeMetadataResolver}。
	 */
	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	/**
	 * 指示是否应为检测到的组件生成代理，这在以代理方式使用作用域时可能是必需的。
	 * <p>默认值是延迟到执行实际扫描的组件扫描器的默认行为。
	 * <p>请注意，设置此属性会覆盖为 {@link #scopeResolver} 设置的任何值。
	 * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
	 */
	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	/**
	 * 控制符合组件检测条件的类文件。
	 * <p>考虑使用 {@link #includeFilters} 和 {@link #excludeFilters}
	 * 以获得更灵活的方式。
	 */
	String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;

	/**
	 * 指示是否应启用自动检测带有 {@code @Component}、{@code @Repository}、
	 * {@code @Service} 或 {@code @Controller} 注解的类。
	 */
	boolean useDefaultFilters() default true;

	/**
	 * 指定哪些类型适合进行组件扫描。
	 * <p>进一步将候选组件的范围从 {@link #basePackages} 中的所有内容
	 * 缩小到与给定过滤器或过滤器匹配的基础包中的所有内容。
	 * <p>请注意，如果指定了默认过滤器，这些过滤器将在默认过滤器之外应用。
	 * 任何在指定的基础包下与给定过滤器匹配的类型都将被包含，
	 * 即使其不匹配默认过滤器（即未注解 {@code @Component}）。
	 * @see #resourcePattern()
	 * @see #useDefaultFilters()
	 */
	Filter[] includeFilters() default {};

	/**
	 * 指定哪些类型不适合进行组件扫描。
	 * @see #resourcePattern
	 */
	Filter[] excludeFilters() default {};

	/**
	 * 指定扫描到的 bean 是否应注册为延迟初始化。
	 * <p>默认值为 {@code false}；需要时切换为 {@code true}。
	 * @since 4.1
	 */
	boolean lazyInit() default false;


	/**
	 * 声明用作 {@linkplain ComponentScan#includeFilters 包含过滤器}
	 * 或 {@linkplain ComponentScan#excludeFilters 排除过滤器} 的类型过滤器。
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		/**
		 * 要使用的过滤器类型。
		 * <p>默认值为 {@link FilterType#ANNOTATION}。
		 * @see #classes
		 * @see #pattern
		 */
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * {@link #classes} 的别名。
		 * @see #classes
		 */
		@AliasFor("classes")
		Class<?>[] value() default {};

		/**
		 * 用作过滤器的类或多个类。
		 * <p>下表说明了根据 {@link #type} 属性的配置值如何解释这些类。
		 * <table border="1">
		 * <tr><th>{@code FilterType}</th><th>类的解释方式</th></tr>
		 * <tr><td>{@link FilterType#ANNOTATION ANNOTATION}</td>
		 * <td>注解本身</td></tr>
		 * <tr><td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
		 * <td>检测到的组件应可分配到的类型</td></tr>
		 * <tr><td>{@link FilterType#CUSTOM CUSTOM}</td>
		 * <td>{@link TypeFilter} 的实现</td></tr>
		 * </table>
		 * <p>当指定了多个类时，使用 <em>OR</em> 逻辑
		 * &mdash;例如，"包含带有 {@code @Foo} 或 {@code @Bar} 注解的类型"。
		 * <p>自定义 {@link TypeFilter TypeFilters} 可以选择实现以下任意
		 * {@link org.springframework.beans.factory.Aware Aware} 接口，
		 * 其各自的方法将在 {@link TypeFilter#match match} 之前被调用：
		 * <ul>
		 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
		 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
		 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
		 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
		 * </ul>
		 * <p>指定零个类是允许的，但对组件扫描没有影响。
		 * @since 4.2
		 * @see #value
		 * @see #type
		 */
		@AliasFor("value")
		Class<?>[] classes() default {};

		/**
		 * 用于过滤器的模式（或多个模式），作为指定类 {@link #value} 的替代方案。
		 * <p>如果 {@link #type} 设置为 {@link FilterType#ASPECTJ ASPECTJ}，
		 * 这是一个 AspectJ 类型模式表达式。如果 {@link #type} 设置为
		 * {@link FilterType#REGEX REGEX}，这是一个用于匹配全限定类名的正则表达式。
		 * @see #type
		 * @see #classes
		 */
		String[] pattern() default {};

	}

}
