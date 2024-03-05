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
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * 扩展 {@link org.springframework.beans.factory.BeanFactory} 接口的扩展，
 * 由那些能够自动装配的 Bean 工厂来实现，前提是它们希望为现有的 Bean 实例暴露此功能。
 *
 * <p>BeanFactory 的这个子接口不是用于普通应用程序代码的：通常情况下使用 {@link org.springframework.beans.factory.BeanFactory}
 * 或 {@link org.springframework.beans.factory.ListableBeanFactory}。
 *
 * <p>其他框架的集成代码可以利用此接口来连接和填充 Spring 无法控制生命周期的现有 Bean 实例。
 * 例如，对于 WebWork Actions 和 Tapestry Page 对象非常有用。
 *
 * <p>请注意，此接口未由 {@link org.springframework.context.ApplicationContext} 门面实现，
 * 因为它几乎从不被应用程序代码使用。 也就是说，它也可以从应用程序上下文中获得，
 * 通过 ApplicationContext 的 {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()} 方法访问。
 *
 * <p>您还可以实现 {@link org.springframework.beans.factory.BeanFactoryAware} 接口，
 * 它即使在 ApplicationContext 中运行时也会暴露内部 BeanFactory，以便访问 AutowireCapableBeanFactory：
 * 只需将传入的 BeanFactory 强制转换为 AutowireCapableBeanFactory。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 * @since 04.12.2003
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * 表示没有外部定义的自动装配的常量。请注意，仍将应用 BeanFactoryAware 等和注释驱动的注入。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * 表示按名称自动装配 Bean 属性（适用于所有 Bean 属性设置器）的常量。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * 表示按类型自动装配 Bean 属性（适用于所有 Bean 属性设置器）的常量。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * 表示自动装配可以满足的最贪婪的构造函数（涉及解析适当的构造函数）的常量。
	 *
	 * @see #createBean
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * 表示通过对 Bean 类的内省来确定适当的自动装配策略的常量。
	 *
	 * @see #createBean
	 * @see #autowire
	 * @deprecated 自Spring 3.0起：如果您使用混合自动装配策略，请更喜欢基于注解的自动装配，以清晰地划分自动装配需求。
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;

	/**
	 * 在初始化现有 Bean 实例时，用于“原始实例”约定的后缀：要附加到完全限定的 Bean 类名，
	 * 例如“com.mypackage.MyClass.ORIGINAL”，以便强制返回给定实例，即没有代理等。
	 *
	 * @see #initializeBean(Object, String)
	 * @see #applyBeanPostProcessorsBeforeInitialization(Object, String)
	 * @see #applyBeanPostProcessorsAfterInitialization(Object, String)
	 * @since 5.1
	 */
	String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


	//-------------------------------------------------------------------------
	// 创建和填充外部bean实例的典型方法
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新 bean 实例。
	 * <p>执行 bean 的完全初始化，包括所有适用的{@link BeanPostProcessor BeanPostProcessors}。
	 * <p>注意：这是用于创建一个新实例，填充带注释的字段和方法以及应用所有标准的 bean 初始化回调。
	 * 它不意味着传统的按名称或按类型自动装配属性；用于这些目的的是 {@link #createBean(Class, int, boolean)}。
	 *
	 * @param beanClass 要创建的 bean 的类
	 * @return 新的 bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 通过应用实例化后的回调和 bean 属性后处理（例如，用于注解驱动的注入）来填充给定的 bean 实例。
	 * <p>注意：这主要用于（重新）填充带注释的字段和方法，无论是对新实例还是对反序列化实例。它不意味着传统的按名称或按类型自动装配属性；用于这些目的的是 {@link #autowireBeanProperties}。
	 *
	 * @param existingBean 现有的 bean 实例
	 * @throws BeansException 如果装配失败
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置给定的原始 bean：自动装配 bean 属性，应用 bean 属性值，
	 * 应用工厂回调（例如 {@code setBeanName} 和 {@code setBeanFactory}）以及应用所有 bean 后处理器
	 * （包括可能包装给定原始 bean 的后处理器）。
	 * <p>这实际上是 {@link #initializeBean} 提供的超集，完全应用相应 bean 定义指定的配置。
	 * <b>注意：此方法需要给定名称的 bean 定义！</b>
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param beanName     bean 的名称，如有必要，将其传递给它
	 *                     （必须有该名称的 bean 定义）
	 * @return 要使用的 bean 实例，原始实例或包装实例
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的 bean 定义
	 * @throws BeansException                                                  如果初始化失败
	 * @see #initializeBean
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// 用于对bean生命周期进行细粒度控制的专用方法
	//-------------------------------------------------------------------------

	/**
	 * 使用指定的自动装配策略完全创建给定类的新 Bean 实例。
	 * 此接口中定义的所有常量均在此处受支持。
	 * <p>执行对 Bean 的完全初始化，包括所有适用的 {@link BeanPostProcessor BeanPostProcessors}。
	 * 这实际上是 {@link #autowire} 提供的超集，添加了 {@link #initializeBean} 行为。
	 *
	 * @param beanClass       要创建的 Bean 的类
	 * @param autowireMode    按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖项检查
	 *                        (不适用于自动装配构造函数，因此在那里被忽略)
	 * @return 新的 Bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 */
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的自动装配策略实例化给定类的新 Bean 实例。
	 * 此接口中定义的所有常量均在此处受支持。
	 * 也可以使用 {@code AUTOWIRE_NO} 来只应用实例化前的回调（例如用于基于注解的注入）。
	 * <p>不应用标准 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 Bean 的任何进一步初始化。
	 * 该接口为这些目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。
	 * 但是，如果适用于实例的构造，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 *
	 * @param beanClass       要实例化的 Bean 的类
	 * @param autowireMode    按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 Bean 实例中的对象引用执行依赖检查
	 *                        (不适用于自动装配构造函数，因此在那里被忽略)
	 * @return 新的 Bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #initializeBean
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 按名称或类型自动装配给定 Bean 实例的 Bean 属性。
	 * 也可以使用 {@code AUTOWIRE_NO} 来只应用实例化后的回调（例如用于基于注解的注入）。
	 * <p>不应用标准 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 Bean 的任何进一步初始化。
	 * 该接口为这些目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。
	 * 但是，如果适用于实例的配置，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 *
	 * @param existingBean    现有的 Bean 实例
	 * @param autowireMode    按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 Bean 实例中的对象引用执行依赖检查
	 * @throws BeansException 如果装配失败
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_NO
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException;

	/**
	 * 将给定名称的 Bean 定义的属性值应用于给定的 Bean 实例。
	 * Bean 定义可以定义一个完全自包含的 Bean，重用其属性值，或者只是属性值，用于现有 Bean 实例。
	 * <p>此方法不会自动装配 Bean 属性；它只应用显式定义的属性值。 使用 {@link #autowireBeanProperties} 方法自动装配现有 Bean 实例。
	 * <b>注意：此方法需要给定名称的 Bean 定义！</b>
	 * <p>不应用标准 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 Bean 的任何进一步初始化。
	 * 该接口为这些目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。 但是，如果适用于实例的配置，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 *
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName     Bean 工厂中 Bean 定义的名称
	 *                     (必须有一个该名称的 Bean 定义)
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的 Bean 定义
	 * @throws BeansException                                                  如果应用属性值失败
	 * @see #autowireBeanProperties
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化给定的原始 Bean，应用工厂回调，如 {@code setBeanName} 和 {@code setBeanFactory}，
	 * 也应用所有 Bean 后处理器（包括可能包装给定原始 Bean 的 Bean）。
	 * <p>请注意，给定名称的 Bean 定义不必存在于 Bean 工厂中。
	 * 传入的 Bean 名称将仅用于回调，但不会与注册的 Bean 定义进行检查。
	 *
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName     Bean 的名称，如果必要，则传递给它
	 *                     (只传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                     可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以确保返回给定实例，即没有代理等)
	 * @return 要使用的 Bean 实例，原始的或包装的实例
	 * @throws BeansException 如果初始化失败
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 Bean 实例，
	 * 调用它们的 {@code postProcessBeforeInitialization} 方法。 返回的 Bean 实例可能是原始的包装器。
	 *
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName     Bean 的名称，如果必要，则传递给它
	 *                     (只传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                     可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以确保返回给定实例，即没有代理等)
	 * @return 要使用的 Bean 实例，原始的或包装的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessBeforeInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 Bean 实例，
	 * 调用它们的 {@code postProcessAfterInitialization} 方法。 返回的 Bean 实例可能是原始的包装器。
	 *
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName     Bean 的名称，如果必要，则传递给它
	 *                     (只传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                     可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以确保返回给定实例，即没有代理等)
	 * @return 要使用的 Bean 实例，原始的或包装的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessAfterInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 销毁给定的 Bean 实例（通常来自 {@link #createBean}），应用 {@link org.springframework.beans.factory.DisposableBean} 合同，
	 * 以及已注册的 {@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}。
	 * <p>销毁过程中引发的任何异常应该被捕获并记录，而不是传播到此方法的调用者。
	 *
	 * @param existingBean 要销毁的 Bean 实例
	 */
	void destroyBean(Object existingBean);


	//-------------------------------------------------------------------------
	// 用于解析注入点的委托方法
	//-------------------------------------------------------------------------

	/**
	 * 解析唯一匹配给定对象类型的 Bean 实例（如果存在），包括其 Bean 名称。
	 * <p>这实际上是 {@link #getBean(Class)} 的一种变体，它保留了匹配实例的 Bean 名称。
	 *
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @return Bean 名称加上 Bean 实例
	 * @throws NoSuchBeanDefinitionException   如果找不到匹配的 Bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 Bean
	 * @throws BeansException                  如果无法创建 Bean
	 * @see #getBean(Class)
	 * @since 4.3.3
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 解析给定 Bean 名称的 Bean 实例，提供一个依赖描述符以便公开给目标工厂方法。
	 * <p>这实际上是 {@link #getBean(String, Class)} 的一种变体，支持具有 {@link org.springframework.beans.factory.InjectionPoint} 参数的工厂方法。
	 *
	 * @param name       要查找的 Bean 的名称
	 * @param descriptor 用于请求的注入点的依赖描述符
	 * @return 相应的 Bean 实例
	 * @throws NoSuchBeanDefinitionException 如果没有具有指定名称的 Bean
	 * @throws BeansException                如果无法创建 Bean
	 * @see #getBean(String, Class)
	 * @since 5.1.5
	 */
	Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

	/**
	 * 解析此工厂中定义的 Bean 相对于指定的依赖项。
	 *
	 * @param descriptor         依赖项的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖项的 Bean 的名称
	 * @return 已解析的对象；如果未找到则为 {@code null}
	 * @throws NoSuchBeanDefinitionException   如果找不到匹配的 Bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 Bean
	 * @throws BeansException                  如果由于任何其他原因导致依赖项解析失败
	 * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
	 * @since 2.5
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;

	/**
	 * 解析在该工厂中定义的bean相对于指定的依赖关系。
	 *
	 * @param descriptor         依赖关系的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖关系的bean的名称
	 * @param autowiredBeanNames 应该将所有自动装配的bean的名称（用于解析给定依赖关系）添加到的Set
	 * @param typeConverter      用于填充数组和集合的TypeConverter
	 * @return 已解析的对象，如果找不到则为{@code null}
	 * @throws NoSuchBeanDefinitionException   如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException                  如果由于任何其他原因导致依赖关系解析失败
	 * @see DependencyDescriptor
	 * @since 2.5
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
							 @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
