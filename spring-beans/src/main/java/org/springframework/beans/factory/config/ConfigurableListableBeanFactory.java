/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.lang.Nullable;

import java.util.Iterator;

/**
 * 用于实现大多数可列出的 Bean 工厂的配置接口。
 * 除了 {@link ConfigurableBeanFactory}，它还提供了分析和修改 Bean 定义以及预实例化单例的功能。
 *
 * <p>这个 {@link org.springframework.beans.factory.BeanFactory} 的子接口不应该在普通应用程序代码中使用：对于典型的用例，
 * 请坚持使用 {@link org.springframework.beans.factory.BeanFactory} 或 {@link org.springframework.beans.factory.ListableBeanFactory}。
 * 此接口仅用于允许在需要访问 Bean 工厂配置方法时进行框架内部插件化。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 * @since 03.11.2003
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * 忽略自动装配中的给定依赖类型：例如，String。默认为 none。
	 *
	 * @param type 要忽略的依赖类型
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略自动装配中的给定依赖接口。
	 * <p>这通常由应用程序上下文使用，以注册通过其他方式解析的依赖项，例如通过 BeanFactoryAware 或 ApplicationContextAware。
	 * <p>默认情况下，仅忽略 BeanFactoryAware 接口。要忽略更多类型，请为每种类型调用此方法。
	 *
	 * @param ifc 要忽略的依赖接口
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 使用相应的自动装配值注册特殊的依赖类型。
	 * <p>这用于应该自动装配但未在工厂中定义为 Bean 的工厂/上下文引用：
	 * 例如，类型为 ApplicationContext 的依赖项解析为 Bean 所在的 ApplicationContext 实例。
	 * <p>注意：在普通 BeanFactory 中没有注册这样的默认类型，甚至对于 BeanFactory 接口本身也没有。
	 *
	 * @param dependencyType 要注册的依赖类型。这通常是一个基本接口，例如 BeanFactory，
	 *                       它的扩展也将被解析为自动装配依赖（例如，ListableBeanFactory），
	 *                       只要给定的值实际上实现了扩展接口。
	 * @param autowiredValue 相应的自动装配值。这也可以是 {@link org.springframework.beans.factory.ObjectFactory}
	 *                       接口的实现，它允许对实际目标值进行延迟解析。
	 */
	void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

	/**
	 * 确定指定的 Bean 是否符合自动装配的候选对象的条件，以注入到声明了匹配类型的依赖项的其他 Bean 中。
	 * <p>此方法还会检查祖先工厂。
	 *
	 * @param beanName   要检查的 Bean 的名称
	 * @param descriptor 要解析的依赖项的描述符
	 * @return 是否应将 Bean 视为自动装配候选对象
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 Bean
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * 返回指定 Bean 的注册 BeanDefinition，允许访问其属性值和构造函数参数值（可以在 Bean 工厂后处理期间修改）。
	 * <p>返回的 BeanDefinition 对象不应该是副本，而是作为在工厂中注册的原始定义对象。
	 * 这意味着如果需要的话，它应该可以转换为更具体的实现类型。
	 * <p><b>注意：</b>此方法不考虑祖先工厂。它仅用于访问此工厂的本地 Bean 定义。
	 *
	 * @param beanName Bean 的名称
	 * @return 注册的 BeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果在此工厂中没有定义给定名称的 Bean
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回由此工厂管理的所有 Bean 名称的统一视图。
	 * <p>包括 Bean 定义名称以及手动注册的单例实例的名称，其中 Bean 定义名称始终先出现，
	 * 类似于如何工作的类型/注释特定检索 Bean 名称。
	 *
	 * @return Bean 名称视图的复合迭代器
	 * @see #containsBeanDefinition
	 * @see #registerSingleton
	 * @see #getBeanNamesForType
	 * @see #getBeanNamesForAnnotation
	 * @since 4.1.2
	 */
	Iterator<String> getBeanNamesIterator();

	/**
	 * 清除合并的 Bean 定义缓存，删除不符合完整元数据缓存资格的 Bean 条目。
	 * <p>通常在对原始 Bean 定义进行更改后触发，例如在应用 {@link BeanFactoryPostProcessor} 后。
	 * 请注意，在此时已经创建的 Bean 的元数据将被保留。
	 *
	 * @see #getBeanDefinition
	 * @see #getMergedBeanDefinition
	 * @since 4.2
	 */
	void clearMetadataCache();

	/**
	 * 冻结所有 Bean 定义，表示注册的 Bean 定义将不会再被修改或进一步后处理。
	 * <p>这允许工厂积极缓存 Bean 定义元数据。
	 */
	void freezeConfiguration();

	/**
	 * 返回此工厂的 Bean 定义是否已冻结，即不应该再被修改或进一步后处理。
	 *
	 * @return 如果工厂的配置被视为已冻结，则为 {@code true}
	 */
	boolean isConfigurationFrozen();

	/**
	 * 确保实例化所有非延迟初始化的单例，也考虑 {@link org.springframework.beans.factory.FactoryBean FactoryBeans}。
	 * 通常在工厂设置结束时（如果需要）调用。
	 *
	 * @throws BeansException 如果无法创建其中一个单例 Bean。
	 *                        注意：这可能已经留下了一些已经初始化的 Bean 工厂！在这种情况下，请调用 {@link #destroySingletons()} 进行完全清理。
	 * @see #destroySingletons()
	 */
	void preInstantiateSingletons() throws BeansException;

}
