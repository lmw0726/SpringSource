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

/**
 * The root interface for accessing a Spring bean container.
 *
 * <p>This is the basic client view of a bean container;
 * further interfaces such as {@link ListableBeanFactory} and
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Spring
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup. Spring's Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code org.springframework.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * <p>In contrast to the methods in {@link ListableBeanFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalBeanFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Beans in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of BeanPostProcessors
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>a custom {@code init-method} definition
 * <li>{@code postProcessAfterInitialization} methods of BeanPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * <li>DisposableBean's {@code destroy}
 * <li>a custom {@code destroy-method} definition
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 * @since 13 April 2001
 */
public interface BeanFactory {

	/**
	 * 用于取消引用 {@link FactoryBean} 实例，并将其与FactoryBean创建的beans 区分开来。
	 * 例如，如果名为 {@code myJndiObject} 的bean是FactoryBean，
	 * 则获取 {@code &myJndiObject} 将返回工厂，而不是工厂返回的实例。
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p> 此方法允许将Spring BeanFactory用作单例或原型设计模式的替代品。
	 * 在单例bean的情况下，调用方可以保留对返回的对象的引用。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name 要检索的bean的名称
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有指定名称的bean
	 * @throws BeansException                如果不能获得bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。<p> 的行为与 {@link #getBean(String)} 相同，
	 * 但是如果bean不是必需的类型，则通过抛出 BeanNotOfRequiredTypeException 来提供类型安全性的度量。
	 * 这意味着ClassCastException不能在正确转换结果时抛出，就像 {@link #getBean(String)} 可能发生的那样。
	 * <p> 将别名翻译回相应的规范bean名称。<p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name         要检索的bean的名称
	 * @param requiredType 键入bean必须匹配; 可以是接口或超类
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException  如果没有这样的bean定义
	 * @throws BeanNotOfRequiredTypeException 如果bean不是必需的类型
	 * @throws BeansException                 如果无法创建bean
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p> 允许指定显式构造函数参数工厂方法参数，覆盖bean定义中指定的默认参数 (如果有)。
	 *
	 * @param name 要检索的bean的名称
	 * @param args 使用显式参数创建bean实例时要使用的参数 (仅在创建新实例而不是检索现有实例时应用)
	 * @return bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException  如果已经给出了参数，但受影响的bean不是原型
	 * @throws BeansException                如果无法创建bean
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 返回唯一匹配给定对象类型 (如果有) 的bean实例。
	 * <p> 此方法按类型查找区域进入 {@link ListableBeanFactory}，但也可以根据给定类型的名称转换为常规的按名称查找。
	 * 要跨bean集进行更广泛的检索操作，请使用 {@link ListableBeanFactory} 和 {@link BeanFactoryUtils}。
	 *
	 * @param requiredType 必须匹配bean类型; 可以是接口或超类
	 * @return 与所需类型匹配的单个bean实例
	 * @throws NoSuchBeanDefinitionException   如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException                  如果无法创建bean
	 * @see ListableBeanFactory
	 * @since 3.0
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p> 允许指定显式构造函数参数工厂方法参数，覆盖bean定义中指定的默认参数 (如果有)。
	 * <p> 此方法按类型查找区域进入 {@link ListableBeanFactory}，但也可以根据给定类型的名称转换为常规的按名称查找。
	 * 要跨bean集进行更广泛的检索操作，请使用 {@link ListableBeanFactory} 和 {@link BeanFactoryUtils}。
	 *
	 * @param requiredType 必须匹配bean类型; 可以是接口或超类
	 * @param args         使用显式参数创建bean实例时要使用的参数 (仅在创建新实例而不是检索现有实例时应用)
	 * @return 一个bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException  如果已经给出了参数，但受影响的bean不是原型
	 * @throws BeansException                如果无法创建bean
	 * @since 4.1
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * 返回指定bean的提供程序，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * <p> 要匹配泛型类型，请考虑 {@link #getBeanProvider(ResolvableType)}。
	 *
	 * @param requiredType 必须匹配bean类型; 可以是接口或超类
	 * @return 一个相关的处理提供者
	 * @see #getBeanProvider(ResolvableType)
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * 返回指定bean的提供程序，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * 此变体允许指定要匹配的泛型类型，类似于在method/constructor参数中带有泛型类型声明的反射注入点。
	 * <p> 请注意，与反射注入点相反，此处不支持bean的集合。
	 * 要以编程方式检索与特定类型匹配的bean列表，请在此处指定实际的bean类型作为参数，然后使用 {@link ObjectProvider#orderedStream()} 或其lazy streaming/iteration选项。
	 * <p> 此外，根据Java分配规则，泛型匹配在这里是严格的。对于与未检查的语义匹配的宽松回退 (类似于 “未检查” 的Java编译器警告)，
	 * 请考虑使用原始类型调用 {@link #getBeanProvider(Class)} 作为第二步，如果没有完全的通用匹配是此变体的 {@link ObjectProvider#getIfAvailable () 可用}。
	 *
	 * @param requiredType bean必须匹配的类型; 可以是泛型类型声明
	 * @return 一个相关的处理提供者
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * 这个bean工厂是否包含一个bean定义或外部注册的具有给定名称的单例实例？
	 * <p>如果给定的名称是别名，则将其翻译回相应的规范bean名称。
	 * <p>如果此工厂是分层的，则会询问任何父工厂是否在此工厂实例中找不到bean。
	 * <p> 如果找到与给定名称匹配的bean定义或单例实例，则无论命名的bean定义是具体的还是抽象的，懒惰的还是渴望的，
	 * 在范围内，此方法都将返回 {@code true}。
	 * 因此，请注意，此方法中的 {@code true} 返回值不一定指示 {@link #getBean} 将能够获得相同名称的实例。
	 *
	 * @param name 要查询的bean的名称
	 * @return 是否存在具有给定名称的bean
	 */
	boolean containsBean(String name);

	/**
	 * 这个bean是共享的单例吗？也就是说，{@link #getBean} 是否总是返回相同的实例？
	 * <p>注意: 返回 {@code false} 的方法没有明确表示独立实例。它指示非单例实例，也可能对应于作用域bean。
	 * 使用 {@link #isPrototype} 操作显式检查独立实例。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name 要查询的bean的名称
	 * @return 这个bean是否对应一个单例实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #isPrototype
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 这个bean是一个原型对象吗？
	 * 也就是说，{@link #getBean} 总是会返回独立实例吗？
	 * <p>注意: 此返回 {@code false} 的方法没有明确指示单例对象。
	 * 它指示非独立实例，也可能对应于作用域bean。
	 * 使用 {@link #isSingleton} 操作显式检查共享单例实例。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name 要查询的bean的名称
	 * @return 这个bean是否总是提供独立的实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #isSingleton
	 * @since 2.0.3
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定的类型匹配。
	 * 更具体地说，检查给定名称的 {@link #getBean} 调用是否会返回可分配给指定目标类型的对象。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name        要查询的bean的名称
	 * @param typeToMatch 要匹配的类型 (作为 {@code ResolvableType})
	 * @return {@code true} 如果bean类型匹配，{@code false} 如果不匹配或还不能确定
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #getType
	 * @since 4.2
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定的类型匹配。
	 * 更具体地说，检查给定名称的 {@link #getBean} 调用是否会返回可分配给指定目标类型的对象。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name        要查询的bean的名称
	 * @param typeToMatch 要匹配的类型 (作为 {@code Class})
	 * @return {@code true} 如果bean类型匹配，{@code false} 如果不匹配或还不能确定
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #getType
	 * @since 2.0.1
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean的类型。
	 * 更具体地说，确定 {@link #getBean} 将为给定名称返回的对象类型。
	 * <p> 对于 {@link FactoryBean}，返回FactoryBean创建的对象的类型，如 {@link FactoryBean#getObjectType()} 所公开。
	 * 这可能导致初始化先前未初始化的 {@code FactoryBean} (请参阅 {@link #getType(String，boolean)})。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name 要查询的bean的名称
	 * @return bean的类型，如果无法确定，则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 1.1.2
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean的类型。
	 * 更具体地说，确定 {@link #getBean} 将为给定名称返回的对象类型。
	 * <p> 对于 {@link FactoryBean}，返回FactoryBean创建的对象的类型，如 {@link FactoryBean#getObjectType()} 所公开。
	 * 根据 {@code allowFactoryBeanInit} 标志，如果没有可用的早期类型信息，这可能会导致先前未初始化的 {@code FactoryBean} 的初始化。
	 * <p> 将别名翻译回相应的规范bean名称。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name                 要查询的bean的名称
	 * @param allowFactoryBeanInit {@code FactoryBean} 是否可以仅出于确定其对象类型的目的而初始化
	 * @return bean的类型，如果无法确定，则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 5.2
	 */
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的别名 (如果有)。
	 * <p> 在 {@link #getBean} 调用中使用时，所有这些别名都指向同一bean。
	 * <p> 如果给定的名称是别名，则将返回相应的原始bean名和其他别名 (如果有)，其中原始bean名是数组中的第一个元素。
	 * <p> 将询问父工厂是否在此工厂实例中找不到bean。
	 *
	 * @param name 要检查别名的bean名称
	 * @return 别名，如果没有，则为空数组
	 * @see #getBean
	 */
	String[] getAliases(String name);

}
