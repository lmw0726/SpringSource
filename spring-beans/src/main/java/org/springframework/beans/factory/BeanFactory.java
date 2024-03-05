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
 * 用于访问 Spring Bean 容器的根接口。
 *
 * <p>这是 Bean 容器的基本客户端视图；进一步的接口，如 {@link ListableBeanFactory} 和
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}，可用于特定目的。
 *
 * <p>此接口由持有多个 Bean 定义的对象实现，每个 Bean 定义都由一个 String 名称唯一标识。根据 Bean 定义的不同，
 * 工厂将返回一个包含对象的独立实例（Prototype 设计模式）或一个单个共享实例（Singleton 设计模式的一个优秀替代品，
 * 其中实例是工厂范围内的单例）。将返回哪种类型的实例取决于 Bean 工厂的配置：API 是相同的。从 Spring 2.0 开始，
 * 根据具体的应用程序上下文（例如，在 Web 环境中的 "request" 和 "session" 作用域）还提供了进一步的作用域。
 *
 * <p>此方法的重点在于 BeanFactory 是应用程序组件的中央注册表，并且集中了应用程序组件的配置（例如，不再需要单个对象读取属性文件）。
 * 有关此方法的好处的讨论，请参阅 "Expert One-on-One J2EE Design and Development" 的第 4 和 11 章。
 *
 * <p>请注意，通常最好依赖依赖注入（"push" 配置）通过 setter 或构造函数来配置应用程序对象，而不是使用任何形式的 "pull" 配置，
 * 如 BeanFactory 查找。Spring 的依赖注入功能是使用此 BeanFactory 接口及其子接口实现的。
 *
 * <p>通常，BeanFactory 将加载存储在配置源（例如 XML 文档）中的 Bean 定义，并使用 {@code org.springframework.beans} 包来配置这些 Bean。
 * 但是，实现可以简单地直接在 Java 代码中根据需要返回 Java 对象。关于定义可以存储在哪里的约束没有：LDAP、RDBMS、XML、属性文件等。
 * 鼓励实现支持 Bean 之间的引用（依赖注入）。
 *
 * <p>与 {@link ListableBeanFactory} 中的方法相反，此接口中的所有操作都将检查父工厂（如果这是一个 {@link HierarchicalBeanFactory}）。
 * 如果在此工厂实例中找不到 Bean，则将询问其直接父工厂。此工厂实例中的 Bean 应该覆盖任何父工厂中同名的 Bean。
 *
 * <p>Bean 工厂实现应尽可能支持标准的 Bean 生命周期接口。初始化方法及其标准顺序的完整集合如下：
 * <ol>
 * <li>BeanNameAware 的 {@code setBeanName}
 * <li>BeanClassLoaderAware 的 {@code setBeanClassLoader}
 * <li>BeanFactoryAware 的 {@code setBeanFactory}
 * <li>EnvironmentAware 的 {@code setEnvironment}
 * <li>EmbeddedValueResolverAware 的 {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware 的 {@code setResourceLoader}
 * （仅在应用程序上下文中运行时适用）
 * <li>ApplicationEventPublisherAware 的 {@code setApplicationEventPublisher}
 * （仅在应用程序上下文中运行时适用）
 * <li>MessageSourceAware 的 {@code setMessageSource}
 * （仅在应用程序上下文中运行时适用）
 * <li>ApplicationContextAware 的 {@code setApplicationContext}
 * （仅在应用程序上下文中运行时适用）
 * <li>ServletContextAware 的 {@code setServletContext}
 * （仅在 Web 应用程序上下文中运行时适用）
 * <li>BeanPostProcessors 的 {@code postProcessBeforeInitialization} 方法
 * <li>InitializingBean 的 {@code afterPropertiesSet}
 * <li>自定义 {@code init-method} 定义
 * <li>BeanPostProcessors 的 {@code postProcessAfterInitialization} 方法
 * </ol>
 *
 * <p>在关闭 Bean 工厂时，以下生命周期方法适用：
 * <ol>
 * <li>DestructionAwareBeanPostProcessors 的 {@code postProcessBeforeDestruction} 方法
 * <li>DisposableBean 的 {@code destroy}
 * <li>自定义 {@code destroy-method} 定义
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
