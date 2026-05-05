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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 由在处理 @{@link Configuration} 类时注册额外 bean 定义的类型实现的接口。
 * 当需要在 bean 定义级别（与 {@code @Bean} 方法/实例级别相对）进行操作时非常有用。
 *
 * <p>与 {@code @Configuration} 和 {@link ImportSelector} 一样，此类型的类可以提供给 @{@link Import}
 * 注解（也可以从 {@code ImportSelector} 中返回）。
 *
 * <p>{@link ImportBeanDefinitionRegistrar} 可以实现以下任意
 * {@link org.springframework.beans.factory.Aware Aware} 接口，其相应的方法将在
 * {@link #registerBeanDefinitions} 之前被调用：
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>或者，该类可以提供一个构造函数，参数类型为以下一个或多个受支持的类型：
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>有关使用示例，请参阅实现类及相关的单元测试。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * 根据导入的 {@code @Configuration} 类的给定注解元数据，按需注册 bean 定义。
	 * <p>注意，由于 {@code @Configuration} 类处理的生命周期约束，{@link BeanDefinitionRegistryPostProcessor}
	 * 类型<em>不能</em>在此处注册。
	 * <p>默认实现委托给
	 * {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}。
	 * @param importingClassMetadata 导入类的注解元数据
	 * @param registry 当前的 bean 定义注册表
	 * @param importBeanNameGenerator 导入 bean 的 bean 名称生成策略：
	 * 默认为 {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR}，
	 * 如果设置了 {@link ConfigurationClassPostProcessor#setBeanNameGenerator}，
	 * 则使用用户提供的策略。在后一种情况下，传入的策略将与包含它的应用程序上下文中用于组件扫描的策略相同
	 * （否则，默认的组件扫描命名策略为 {@link AnnotationBeanNameGenerator#INSTANCE}）。
	 * @since 5.2
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 * @see ConfigurationClassPostProcessor#setBeanNameGenerator
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {

		registerBeanDefinitions(importingClassMetadata, registry);
	}

	/**
	 * 根据导入的 {@code @Configuration} 类的给定注解元数据，按需注册 bean 定义。
	 * <p>注意，由于 {@code @Configuration} 类处理的生命周期约束，{@link BeanDefinitionRegistryPostProcessor}
	 * 类型<em>不能</em>在此处注册。
	 * <p>默认实现为空。
	 * @param importingClassMetadata 导入类的注解元数据
	 * @param registry 当前的 bean 定义注册表
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	}

}
