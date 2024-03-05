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

package org.springframework.beans.factory.config;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

/**
 * 大多数 Bean 工厂都应该实现的配置接口。提供了配置 Bean 工厂的功能，除了 {@link org.springframework.beans.factory.BeanFactory} 接口中的客户端方法之外。
 *
 * <p>这个 Bean 工厂接口不应该在普通应用程序代码中使用：对于典型的需求，请使用 {@link org.springframework.beans.factory.BeanFactory} 或 {@link org.springframework.beans.factory.ListableBeanFactory}。这个扩展接口只是为了允许框架内部的插拔和特殊访问 Bean 工厂配置方法。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 * @since 03.11.2003
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * 标准单例作用域的作用域标识符：{@value}。
	 * <p>可以通过 {@code registerScope} 添加自定义作用域。
	 *
	 * @see #registerScope
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * 标准原型作用域的作用域标识符：{@value}。
	 * <p>可以通过 {@code registerScope} 添加自定义作用域。
	 *
	 * @see #registerScope
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * 设置此 Bean 工厂的父级。
	 * <p>注意，父级不能更改：只有在工厂实例化时不可用时，才应该在构造函数之外设置它。
	 *
	 * @param parentBeanFactory 父 BeanFactory
	 * @throws IllegalStateException 如果此工厂已经与父 BeanFactory 关联
	 * @see #getParentBeanFactory()
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * 设置用于加载 Bean 类的类加载器。
	 * 默认为线程上下文类加载器。
	 * <p>请注意，此类加载器仅适用于尚未携带已解析 Bean 类的 Bean 定义。这是默认情况下的情况，从 Spring 2.0 开始：Bean 定义仅携带 Bean 类名，待工厂处理 Bean 定义后才解析。
	 *
	 * @param beanClassLoader 要使用的类加载器，或 {@code null} 表示建议使用默认类加载器
	 */
	void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);

	/**
	 * 返回此工厂用于加载 Bean 类的类加载器
	 * （仅当系统类加载器不可访问时才返回 {@code null}）。
	 *
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * 指定用于类型匹配目的的临时类加载器。
	 * 默认为无，简单地使用标准的 Bean 类加载器。
	 * <p>如果涉及<i>加载时编织</i>，通常只指定临时类加载器，以确保尽可能延迟加载实际的 Bean 类。
	 * 临时加载器在 BeanFactory 完成其引导阶段后将被移除。
	 *
	 * @since 2.5
	 */
	void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

	/**
	 * 返回用于类型匹配目的的临时类加载器，如果有的话。
	 *
	 * @since 2.5
	 */
	@Nullable
	ClassLoader getTempClassLoader();

	/**
	 * 设置是否缓存 bean 元数据，例如给定的 bean 定义（以合并方式）和已解析的 bean 类。
	 * 默认为开启。
	 * <p>关闭此标志以启用热刷新 bean 定义对象，特别是 bean 类。如果此标志关闭，则任何创建 bean 实例将重新查询 bean 类加载器以获取新解析的类。
	 *
	 * @param cacheBeanMetadata 是否缓存 bean 元数据
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * 返回是否缓存 bean 元数据，例如给定的 bean 定义（以合并方式）和已解析的 bean 类。
	 */
	boolean isCacheBeanMetadata();

	/**
	 * 指定 bean 定义值中表达式的解析策略。
	 * <p>默认情况下，BeanFactory 中没有表达式支持。
	 * ApplicationContext 通常会在这里设置一个标准的表达式策略，支持 "#{...}" 表达式，采用统一的 EL 兼容风格。
	 *
	 * @param resolver 要设置的解析器，或 {@code null} 表示禁用表达式解析
	 * @since 3.0
	 */
	void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);

	/**
	 * 返回 bean 定义值中表达式的解析策略。
	 *
	 * @return 表达式解析器，如果未设置则返回 {@code null}
	 * @since 3.0
	 */
	@Nullable
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * 指定要用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代。
	 *
	 * @param conversionService 要设置的转换服务，或 {@code null} 表示禁用转换服务
	 * @since 3.0
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * 返回关联的 ConversionService，如果有的话。
	 *
	 * @return 关联的转换服务，如果未设置则返回 {@code null}
	 * @since 3.0
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * 添加一个 PropertyEditorRegistrar，以应用于所有 bean 创建过程。
	 * <p>这样的注册器会为每次 bean 创建尝试创建新的 PropertyEditor 实例，并在给定的注册表上注册它们。
	 * 这样可以避免对自定义编辑器进行同步；因此，通常最好使用此方法而不是 {@link #registerCustomEditor}。
	 *
	 * @param registrar 要注册的 PropertyEditorRegistrar
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。在工厂配置期间调用。
	 * <p>注意，此方法将注册一个共享的自定义编辑器实例；对该实例的访问将同步进行以确保线程安全。
	 * 通常最好使用 {@link #addPropertyEditorRegistrar} 方法，而不是使用此方法，以避免对自定义编辑器进行同步。
	 *
	 * @param requiredType        属性的类型
	 * @param propertyEditorClass 要注册的 {@link PropertyEditor} 类
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * 使用已在此 BeanFactory 中注册的自定义编辑器初始化给定的 PropertyEditorRegistry。
	 *
	 * @param registry 要初始化的 PropertyEditorRegistry
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * 设置此 BeanFactory 应该使用的自定义类型转换器，用于转换 bean 属性值、构造函数参数值等。
	 * <p>这将覆盖默认的 PropertyEditor 机制，因此任何自定义编辑器或自定义编辑器注册器都将变得无关紧要。
	 *
	 * @param typeConverter 要设置的自定义类型转换器
	 * @see #addPropertyEditorRegistrar
	 * @see #registerCustomEditor
	 * @since 2.5
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * 获取此 BeanFactory 使用的类型转换器。每次调用可能都会返回一个新的实例，因为 TypeConverter 通常不是线程安全的。
	 * <p>如果默认的 PropertyEditor 机制处于活动状态，则返回的 TypeConverter 将意识到已注册的所有自定义编辑器。
	 *
	 * @return 此 BeanFactory 使用的类型转换器
	 * @since 2.5
	 */
	TypeConverter getTypeConverter();

	/**
	 * 为嵌入值（如注解属性）添加一个 String 解析器。
	 *
	 * @param valueResolver 要应用于嵌入值的 String 解析器
	 * @since 3.0
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * 确定是否已向此 bean 工厂注册了嵌入值解析器，以通过 {@link #resolveEmbeddedValue(String)} 应用。
	 *
	 * @return 如果已注册嵌入值解析器，则返回 true；否则返回 false
	 * @since 4.3
	 */
	boolean hasEmbeddedValueResolver();

	/**
	 * 解析给定的嵌入值，例如注解属性。
	 *
	 * @param value 要解析的值
	 * @return 已解析的值（可能是原始值）
	 * @since 3.0
	 */
	@Nullable
	String resolveEmbeddedValue(String value);

	/**
	 * 添加一个新的 BeanPostProcessor，将应用于此工厂创建的 bean。在工厂配置期间调用。
	 * <p>注意: 提交到此处的后处理器将按注册顺序应用；实现 {@link org.springframework.core.Ordered} 接口的任何排序语义将被忽略。请注意，自动检测到的后处理器（例如 ApplicationContext 中的 bean）将始终在以编程方式注册的后处理器之后应用。
	 *
	 * @param beanPostProcessor 要注册的后处理器
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * 返回当前注册的 BeanPostProcessor 的数量（如果有）。
	 *
	 * @return 当前注册的 BeanPostProcessor 的数量
	 */
	int getBeanPostProcessorCount();

	/**
	 * 注册给定的作用域，由给定的作用域实现支持。
	 *
	 * @param scopeName 作用域标识符
	 * @param scope     支持的作用域实现
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * 返回当前所有已注册作用域的名称。
	 * <p>这将只返回显式注册的作用域名称。内置作用域（例如“singleton”和“prototype”）不会被公开。
	 *
	 * @return 作用域名称数组，如果没有则返回空数组
	 * @see #registerScope
	 */
	String[] getRegisteredScopeNames();

	/**
	 * 返回给定作用域名称的作用域实现（如果有）。
	 * <p>这将只返回显式注册的作用域。内置作用域（例如“singleton”和“prototype”）不会被公开。
	 *
	 * @param scopeName 作用域的名称
	 * @return 注册的作用域实现，如果没有则为 {@code null}
	 * @see #registerScope
	 */
	@Nullable
	Scope getRegisteredScope(String scopeName);

	/**
	 * 设置此 Bean 工厂的 {@code ApplicationStartup}。
	 * <p>这允许应用程序上下文在应用程序启动期间记录指标。
	 *
	 * @param applicationStartup 新的应用程序启动
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * 返回此 Bean 工厂的 {@code ApplicationStartup}。
	 *
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * 提供与此工厂相关的安全访问控制上下文。
	 *
	 * @return 适用的 AccessControlContext（永远不会为 {@code null}）
	 * @since 3.0
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * 从给定的其他工厂复制所有相关的配置。
	 * <p>应包括所有标准的配置设置以及 BeanPostProcessors、Scopes 和工厂特定的内部设置。
	 * 不应包括任何实际 Bean 定义的元数据，例如 BeanDefinition 对象和 bean 名称别名。
	 *
	 * @param otherFactory 要复制配置的其他 BeanFactory
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * 给定一个bean名称，创建一个别名。我们通常使用此方法来支持在XML id (用于bean名称) 中非法的名称。
	 * <p> 通常在工厂配置期间调用，但也可以用于别名的运行时注册。因此，工厂实现应该同步别名访问。
	 *
	 * @param beanName 目标bean的规范名称
	 * @param alias    要为bean注册的别名
	 * @throws BeanDefinitionStoreException 如果别名已经在使用
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * 解析此工厂中注册的所有别名目标名称和别名，将给定的 StringValueResolver 应用于它们。
	 * <p>值解析器可以例如解析目标 bean 名称中的占位符，甚至可以解析别名中的占位符。
	 *
	 * @param valueResolver 要应用的 StringValueResolver
	 * @since 2.5
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * 返回给定bean名称的合并BeanDefinition，并在必要时将子bean定义与其父项合并。也考虑祖先工厂中的bean定义。
	 *
	 * @param beanName 要检索的合并定义的bean的名称
	 * @return 给定bean的 (可能合并的) bean 定义
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean定义
	 * @since 2.5
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 确定给定名称的 bean 是否为 FactoryBean。
	 *
	 * @param name 要检查的 bean 的名称
	 * @return bean 是否为 FactoryBean
	 * （{@code false} 表示 bean 存在但不是 FactoryBean）
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 bean
	 * @since 2.5
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 显式控制指定 bean 的当前创建状态。仅供容器内部使用。
	 *
	 * @param beanName   bean 的名称
	 * @param inCreation bean 当前是否正在创建
	 * @since 3.1
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * 确定指定的 bean 当前是否正在创建。
	 *
	 * @param beanName bean 的名称
	 * @return bean 当前是否正在创建
	 * @since 2.5
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * 为给定的 bean 注册一个依赖的 bean，在给定的 bean 被销毁之前要销毁。
	 *
	 * @param beanName          bean 名称
	 * @param dependentBeanName 依赖 bean 的名称
	 * @since 2.5
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * 返回所有依赖于指定 bean 的 bean 名称，如果有的话。
	 *
	 * @param beanName bean 的名称
	 * @return 依赖的 bean 名称数组，如果没有则返回空数组
	 * @since 2.5
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * 返回指定 bean 依赖的所有 bean 的名称，如果有的话。
	 *
	 * @param beanName bean 的名称
	 * @return bean 依赖的 bean 名称数组，如果没有则返回空数组
	 * @since 2.5
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * 根据其 bean 定义销毁给定的 bean 实例（通常是从此工厂获取的原型实例）。
	 * <p>销毁过程中引发的任何异常应该被捕获并记录，而不是传播给此方法的调用方。
	 *
	 * @param beanName     bean 定义的名称
	 * @param beanInstance 要销毁的 bean 实例
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * 在当前目标范围中销毁指定的作用域 bean（如果有的话）。
	 * <p>销毁过程中引发的任何异常应该被捕获并记录，而不是传播给此方法的调用方。
	 *
	 * @param beanName 作用域 bean 的名称
	 */
	void destroyScopedBean(String beanName);

	/**
	 * 销毁此工厂中的所有单例 bean，包括已注册为可销毁的内部 bean。在关闭工厂时调用。
	 * <p>销毁过程中引发的任何异常应该被捕获并记录，而不是传播给此方法的调用方。
	 */
	void destroySingletons();

}
