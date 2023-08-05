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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 * @since 19.03.2004
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * 标准单例范围的范围标识符: {@value}。
	 * <p> 请注意，扩展的bean工厂可能会支持进一步的范围。
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 标准原型范围的范围标识符: {@value}。
	 * <p> 请注意，扩展的bean工厂可能会支持进一步的范围。
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 指示 {@code BeanDefinition} 是应用程序的主要部分的角色提示。通常对应于用户定义的bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 角色提示指示 {@code BeanDefinition} 是某些较大配置的支持部分，
	 * 通常是外部 {@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 * {@code SUPPORT} bean被认为足够重要，可以在更仔细地查看特定的
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition} 时意识到，但在查看应用程序的整体配置时却没有。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示指示 {@code BeanDefinition} 提供了完全的后台角色，并且与最终用户无关。
	 * 在注册bean时使用此提示，bean完全是
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition} 内部工作的一部分。
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// 可修改属性

	/**
	 * 设置此bean定义的父定义的名称 (如果有)。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此bean定义的父定义的名称 (如果有)。
	 */
	@Nullable
	String getParentName();

	/**
	 * 指定此bean定义的bean类名。
	 * <p> 可以在bean factory后处理过程中修改类名，通常用解析后的变体替换原始类名。
	 *
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 返回此bean定义的当前bean类名称。
	 * <p> 请注意，如果子定义覆盖从其父级继承类名，则不必是运行时使用的实际类名。
	 * 另外，这可能只是调用工厂方法的类，或者在调用方法的工厂bean引用的情况下甚至可能是空的。
	 * 因此，<i> 不 <i> 认为这是运行时的确定bean类型，而是仅将其用于单个bean定义级别的解析目的。
	 *
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 覆盖此bean的目标作用域，指定新的作用域名称。
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此bean的当前目标作用域的名称，如果还不知道，则返回 {@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * 设置这个bean是否应该被懒惰地初始化。
	 * <p> 如果 {@code false}，则bean将在启动时由执行急切初始化单例的bean工厂实例化。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回这个bean是否应该被懒惰地初始化，即在启动时不急切地实例化。仅适用于单例bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置此bean依赖于被初始化的bean的名称。bean工厂将保证这些bean首先得到初始化。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此bean依赖的bean名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置此bean是否适合将autowired变成其他bean。
	 * <p> 请注意，此标志旨在仅影响基于类型的自动装配。
	 * 它不会影响按名称进行的显式引用，即使指定的bean未标记为自动装配候选者，该名称也会得到解决。
	 * 因此，如果名称匹配，则按名称进行自动查询仍将注入一个bean。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此bean是否是将自动转换为其他bean的候选对象。
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此bean是否是主要的自动装配候选者。
	 * <p> 如果此值是多个匹配候选对象中恰好一个bean的 {@code true}，则它将用作平局决胜者。
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此bean是否是主要的自动装配候选者。
	 */
	boolean isPrimary();

	/**
	 * 指定要使用的工厂bean (如果有)。这是要调用指定工厂方法的bean的名称。
	 *
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂bean名称 (如果有)。
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * 指定工厂方法 (如果有)。
	 * 此方法将使用构造函数参数调用，如果未指定任何参数，则不使用任何参数调用。
	 * 该方法将在指定的工厂bean (如果有的话) 上调用，或者作为本地bean类上的静态方法调用。
	 *
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回工厂方法 (如果有)。
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 返回此bean的构造函数参数值。
	 * <p> 可以在bean工厂后处理过程中修改返回的实例。
	 *
	 * @return ConstructorArgumentValues对象 (从不 {@code null})
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 如果为此bean定义了构造函数参数值，则返回true。
	 *
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * 返回要应用于bean的新实例的属性值。<p> 可以在bean工厂后处理过程中修改返回的实例。
	 *
	 * @return MutablePropertyValues对象 (从不为{@code null})
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * 如果为此bean定义了属性值，则返回true。
	 *
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * 设置初始化程序方法名称
	 *
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * 返回初始化程序方法名称
	 *
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * 设置销毁方法的名称。
	 *
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * 返回销毁方法的名称。
	 *
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * 为此 {@code BeanDefinition} 设置角色提示。
	 * 角色提示为框架和工具提供了特定 {@code BeanDefinition} 的角色和重要性的指示。
	 *
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 * @since 5.1
	 */
	void setRole(int role);

	/**
	 * 获取此 {@code BeanDefinition} 的角色提示。
	 * 角色提示为框架和工具提供了特定 {@code BeanDefinition} 的角色和重要性的指示。
	 *
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * 设置此bean定义的人类可读描述。
	 *
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * 返回这个bean定义的人类可读的描述。
	 */
	@Nullable
	String getDescription();


	// 只读属性

	/**
	 * 基于bean类或其他特定元数据，返回此bean定义的可解析类型。
	 * <p> 这是传统地完全解决在运行时合并bean定义的方案，但不一定解决在配置时间定义实例上。
	 *
	 * @return 可解析类型 (可能 {@link ResolvableType#NONE})
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 * @since 5.2
	 */
	ResolvableType getResolvableType();

	/**
	 * 返回此 <b>单例<b> 是否在所有调用中返回单个共享实例。
	 *
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * 返回此是否为 <b> 原型 <b>，并为每个调用返回一个独立的实例。
	 *
	 * @see #SCOPE_PROTOTYPE
	 * @since 3.0
	 */
	boolean isPrototype();

	/**
	 * 返回这个bean是否是抽象的，也就是说，不意味着要实例化。
	 */
	boolean isAbstract();

	/**
	 * 返回此bean定义来自的资源的描述 (用于在出现错误时显示上下文)。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 返回原始BeanDefinition，如果没有，则返回 {@code null}。
	 * <p> 允许检索修饰的bean定义 (如果有)。
	 * <p> 请注意，此方法返回直接发起者。
	 * 遍历发起者链以找到用户定义的原始BeanDefinition。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
