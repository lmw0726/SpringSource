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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 工厂钩子，允许自定义修改应用程序上下文的bean定义，调整上下文基础bean工厂的bean属性值。
 *
 * <p>用于针对系统管理员的自定义配置文件，覆盖在应用程序上下文中配置的bean属性。参见
 * {@link PropertyResourceConfigurer} 及其具体实现，以解决此类配置需求。
 *
 * <p>{@code BeanFactoryPostProcessor} 可以与并修改bean定义，但永远不会与bean实例交互。
 * 这样做可能导致过早的bean实例化，违反容器并引发意外的副作用。如果需要与bean实例交互，
 * 考虑实现 {@link BeanPostProcessor}。
 *
 * <h3>注册</h3>
 * <p>{@code ApplicationContext} 在其bean定义中自动检测 {@code BeanFactoryPostProcessor}
 * bean，并在任何其他bean创建之前应用它们。{@code BeanFactoryPostProcessor} 也可以以编程方式
 * 向 {@code ConfigurableApplicationContext} 注册。
 *
 * <h3>排序</h3>
 * <p>在 {@code ApplicationContext} 中自动检测到的 {@code BeanFactoryPostProcessor}
 * bean 将根据 {@link org.springframework.core.PriorityOrdered} 和
 * {@link org.springframework.core.Ordered} 语义进行排序。相反，以编程方式注册到
 * {@code ConfigurableApplicationContext} 的 {@code BeanFactoryPostProcessor}
 * bean 将按照注册顺序应用; 通过实现 {@code PriorityOrdered} 或 {@code Ordered} 接口
 * 表达的任何排序语义都将被忽略。此外，{@link org.springframework.core.annotation.Order @Order}
 * 注解对 {@code BeanFactoryPostProcessor} bean 不起作用。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 * @since 06.07.2003
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * 在标准初始化后修改应用程序上下文的内部bean工厂。所有bean定义都将被加载，但尚未实例化任何bean。
	 * 这允许甚至对急切初始化的bean进行覆盖或添加属性。
	 *
	 * @param beanFactory 应用程序上下文使用的bean工厂
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
