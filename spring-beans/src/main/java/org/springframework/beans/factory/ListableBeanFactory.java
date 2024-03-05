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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * {@link BeanFactory} 接口的扩展，由可以枚举其所有 bean 实例的 bean 工厂实现，而不是像客户端请求的那样逐个按名称查找 bean。
 * 预加载所有 bean 定义（例如基于 XML 的工厂）的 BeanFactory 实现可以实现此接口。
 *
 * <p>如果这是一个 {@link HierarchicalBeanFactory}，返回值将不考虑 BeanFactory 层次结构，而只与当前工厂中定义的 bean 相关。
 * 使用 {@link BeanFactoryUtils} 辅助类考虑祖先工厂中的 bean。
 *
 * <p>此接口中的方法将仅尊重此工厂的 bean 定义。它们将忽略通过 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * 的 {@code registerSingleton} 方法等其他手段注册的任何单例 bean，但 {@code getBeanNamesForType} 和 {@code getBeansOfType}
 * 除外，它们将检查此类手动注册的单例 bean。当然，BeanFactory 的 {@code getBean} 也允许对此类特殊 bean 透明访问。但是，在典型的情况下，
 * 所有 bean 都将由外部 bean 定义定义，因此大多数应用程序不需要担心此区分。
 *
 * <p><b>注意：</b>除了 {@code getBeanDefinitionCount} 和 {@code containsBeanDefinition} 外，此接口中的方法不适用于频繁调用。
 * 实现可能会很慢。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 * @since 16 April 2001
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * 检查此 BeanFactory 是否包含具有给定名称的 bean 定义。
	 * <p>不考虑此工厂可能参与的任何层次结构，并忽略通过 bean 定义以外的其他手段注册的任何单例 bean。
	 *
	 * @param beanName 要查找的 bean 的名称
	 * @return 如果此 BeanFactory 包含具有给定名称的 bean 定义
	 * @see #containsBean
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回工厂中定义的 bean 数量。
	 * <p> 不考虑该工厂可能参与的任何层次结构，并且忽略了通过 bean 定义以外的其他方式注册的任何单例 bean。
	 *
	 * @return 工厂中定义的 Bean 的数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有 bean 的名称。
	 * <p> 不考虑此工厂可能参与的任何层次结构，并且忽略了通过 bean 定义以外的其他方式注册的任何单例 bean。
	 *
	 * @return 此工厂中定义的所有 bean 的名称，如果没有定义则返回一个空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回指定 bean 的提供程序，允许延迟按需检索实例，包括可用性和唯一性选项。
	 *
	 * @param requiredType   bean 必须匹配的类型；可以是接口或超类
	 * @param allowEagerInit 流式访问是否可以初始化 <i>延迟初始化的单例</i> 和 <i>由 FactoryBeans 创建的对象</i>（或者通过工厂方法
	 *                       使用 "factory-bean" 引用创建）以进行类型检查
	 * @return 对应的提供程序句柄
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * 返回指定 bean 的提供程序，允许延迟按需检索实例，包括可用性和唯一性选项。
	 *
	 * @param requiredType   bean 必须匹配的类型；可以是泛型类型声明。
	 *                       请注意，这里不支持集合类型，与反射注入点形成对比。对于以编程方式检索与特定类型匹配的 bean 列表，
	 *                       请在此处指定实际的 bean 类型作为参数，然后随后使用 {@link ObjectProvider#orderedStream()} 或其延迟流/迭代选项。
	 * @param allowEagerInit 流式访问是否可以初始化 <i>延迟初始化的单例</i> 和 <i>由 FactoryBeans 创建的对象</i>（或者通过工厂方法
	 *                       使用 "factory-bean" 引用创建）以进行类型检查
	 * @return 对应的提供程序句柄
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 * @since 5.3
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

	/**
	 * 返回与给定类型匹配的 bean 的名称（包括子类），根据 bean 定义或 FactoryBean 的 {@code getObjectType} 的值判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将会被初始化。如果由 FactoryBean 创建的对象不匹配，
	 * 则原始 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 可以包含祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，
	 * 结果将与 {@code getBeanNamesForType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 bean 名称应始终尽可能按照后端配置中的定义顺序返回。
	 *
	 * @param type 要匹配的类型
	 * @return 匹配给定类型（包括子类）的 bean 的名称（或由 FactoryBeans 创建的对象），如果没有，则返回空数组
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 * @since 4.2
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * 返回与指定类型匹配的所有 bean 的名称（包括子类），从 bean 定义或 FactoryBean 的 {@code getObjectType} 的值中判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>如果 FactoryBean 创建的对象不匹配，则 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 也包括祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果与
	 * {@code getBeanNamesForType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 bean 名称应始终尽可能按照后端配置中的定义顺序返回 bean 名称。
	 *
	 * @param type                 要匹配的类或接口
	 * @param includeNonSingletons 是否包括原型或作用域 bean，或只包括单例（也适用于 FactoryBean）
	 * @param allowEagerInit       是否初始化 <i>延迟初始化的单例</i> 和 <i>由 FactoryBeans 创建的对象</i>
	 *                             （或者由带有 "factory-bean" 引用的工厂方法创建）进行类型检查
	 * @return 匹配给定对象类型（包括子类）的 bean 的名称，如果没有则返回一个空数组
	 */
	String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与指定类型匹配的所有 bean 的名称（包括子类），从 bean 定义或 FactoryBean 的 {@code getObjectType} 的值中判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>如果 FactoryBean 创建的对象不匹配，则 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 也包括祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果与
	 * {@code getBeanNamesForType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 bean 名称应始终尽可能按照后端配置中的定义顺序返回 bean 名称。
	 *
	 * @param type 要匹配的类或接口，或者为 {@code null} 表示所有 bean 名称
	 * @return 匹配给定对象类型（包括子类）的 bean 的名称，如果没有则返回一个空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type);

	/**
	 * 返回与指定类型匹配的所有 bean 的名称（包括子类），从 bean 定义或 FactoryBean 的 {@code getObjectType} 的值中判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>如果 FactoryBean 创建的对象不匹配，则 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 也包括祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果与
	 * {@code getBeanNamesForType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 bean 名称应始终尽可能按照后端配置中的定义顺序返回 bean 名称。
	 *
	 * @param type                 要匹配的类或接口，或者为 {@code null} 表示所有 bean 名称
	 * @param includeNonSingletons 是否包括原型或作用域 bean，或只包括单例（也适用于 FactoryBean）
	 * @param allowEagerInit       是否初始化 <i>延迟初始化的单例</i> 和
	 *                             <i>由 FactoryBeans 创建的对象</i> 进行类型检查。请注意，需要急切初始化 FactoryBeans
	 *                             和 "factory-bean" 引用以确定其类型：因此，请注意，将 "true" 传递给此标志将初始化 FactoryBeans
	 *                             和 "factory-bean" 引用。
	 * @return 匹配给定对象类型（包括子类）的 bean 的名称，如果没有则返回一个空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定对象类型匹配的 bean 实例（包括子类），从 bean 定义或 FactoryBean 的 {@code getObjectType} 的值中判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>如果 FactoryBean 创建的对象不匹配，则 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beansOfTypeIncludingAncestors} 也包括祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 getBeansOfType 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果与
	 * {@code getBeansOfType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 Map 应始终尽可能按照后端配置中的定义顺序返回 bean 名称和相应的 bean 实例。
	 *
	 * @param type 要匹配的类或接口，或者为 {@code null} 表示所有具体 bean
	 * @return 匹配的 bean 的 Map，包含 bean 名称作为键和相应的 bean 实例作为值
	 * @throws BeansException 如果无法创建 bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 * @since 1.1.2
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;

	/**
	 * 返回与给定对象类型匹配的 bean 实例（包括子类），从 bean 定义或 FactoryBean 的 {@code getObjectType} 的值中判断。
	 * <p><b>注意：此方法仅检查顶级 bean。</b> 它 <i>不会</i> 检查可能同样匹配指定类型的嵌套 bean。
	 * <p>如果 FactoryBean 创建的对象不匹配，则 FactoryBean 本身将与该类型匹配。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beansOfTypeIncludingAncestors} 也包括祖先工厂中的 bean。
	 * <p>注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	 * <p>此版本的 getBeansOfType 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果与
	 * {@code getBeansOfType(type, true, true)} 的结果相同。
	 * <p>此方法返回的 Map 应始终尽可能按照后端配置中的定义顺序返回 bean 名称和相应的 bean 实例。
	 *
	 * @param type                 要匹配的类或接口，或者为 {@code null} 表示所有具体 bean
	 * @param includeNonSingletons 是否包括原型或作用域 bean，或只包括单例（也适用于 FactoryBean）
	 * @param allowEagerInit       是否初始化 <i>延迟初始化的单例</i> 和
	 *                             <i>由 FactoryBeans 创建的对象</i> 进行类型检查。请注意，需要急切初始化 FactoryBeans
	 *                             和 "factory-bean" 引用以确定其类型：因此，请注意，将 "true" 传递给此标志将初始化 FactoryBeans
	 *                             和 "factory-bean" 引用。
	 * @return 匹配的 bean 的 Map，包含 bean 名称作为键和相应的 bean 实例作为值
	 * @throws BeansException 如果无法创建 bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 查找使用提供的 {@link Annotation} 类型注释的所有 bean 的名称，但尚未创建相应的 bean 实例。
	 * <p>请注意，此方法会考虑 FactoryBeans 创建的对象，这意味着为了确定其对象类型，将初始化 FactoryBeans。
	 *
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 所有匹配 bean 的名称
	 * @see #findAnnotationOnBean
	 * @since 4.0
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * 查找使用提供的 {@link Annotation} 类型注释的所有 bean，并返回包含 bean 名称和相应 bean 实例的 Map。
	 * <p>请注意，此方法会考虑 FactoryBeans 创建的对象，这意味着为了确定其对象类型，将初始化 FactoryBeans。
	 *
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 包含匹配的 bean 的 Map，其中包含 bean 名称作为键和相应的 bean 实例作为值
	 * @throws BeansException 如果无法创建 bean
	 * @see #findAnnotationOnBean
	 * @since 3.0
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 在指定的 bean 上查找 {@code annotationType} 的 {@link Annotation}，
	 * 遍历其接口和超类（如果给定类本身上找不到注解），以及检查 bean 的工厂方法（如果有的话）。
	 *
	 * @param beanName       要查找注解的 bean 的名称
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 如果找到指定类型的注解，则返回该类型的注解，否则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 bean
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 * @see #getType(String)
	 * @since 3.0
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

	/**
	 * 在指定的 bean 上查找 {@code annotationType} 的 {@link Annotation}，
	 * 遍历其接口和超类（如果给定类本身上找不到注解），以及检查 bean 的工厂方法（如果有的话）。
	 *
	 * @param beanName             要查找注解的 bean 的名称
	 * @param annotationType       要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @param allowFactoryBeanInit 是否可以为了确定其对象类型而初始化 {@code FactoryBean}
	 * @return 如果找到指定类型的注解，则返回该类型的注解，否则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 bean
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 * @see #getType(String, boolean)
	 * @since 5.3.14
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException;

}
