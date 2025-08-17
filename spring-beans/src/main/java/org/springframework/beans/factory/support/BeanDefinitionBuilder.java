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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.function.Supplier;

/**
 * 使用构建器模式以编程方式构造
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions} 的工具类。
 * 主要用于实现 Spring 2.0 的
 * {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers} 时使用。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public final class BeanDefinitionBuilder {

	/**
	 * 创建一个新的{@code BeanDefinitionBuilder}，用于构造一个 {@link GenericBeanDefinition}。
	 */
	public static BeanDefinitionBuilder genericBeanDefinition() {
		return new BeanDefinitionBuilder(new GenericBeanDefinition());
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 *
	 * @param beanClassName 要为其创建定义的 bean 的类名
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 *
	 * @param beanClass 要为其创建定义的 bean 的 {@code Class}
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link GenericBeanDefinition}。
	 *
	 * @param beanClass        要为其创建定义的 bean 的 {@code Class}
	 * @param instanceSupplier 创建 bean 实例的回调函数
	 * @since 5.0
	 */
	public static <T> BeanDefinitionBuilder genericBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setInstanceSupplier(instanceSupplier);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanClassName 要为其创建定义的 bean 的类名
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
		return rootBeanDefinition(beanClassName, null);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanClassName     要为其创建定义的 bean 的类名
	 * @param factoryMethodName 用于构造 bean 实例的方法名称
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanClass 要为其创建定义的 bean 的 {@code Class}
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, (String) null);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanClass         要为其创建定义的 bean 的 {@code Class}
	 * @param factoryMethodName 用于构造 bean 实例的方法名称
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanType         要为其创建定义的 bean 的 {@link ResolvableType 类型}
	 * @param instanceSupplier 创建 bean 实例的回调函数
	 * @since 5.3.9
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(ResolvableType beanType, Supplier<T> instanceSupplier) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(beanType);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		return new BeanDefinitionBuilder(beanDefinition);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link RootBeanDefinition}。
	 *
	 * @param beanClass        要为其创建定义的 bean 的 {@code Class}
	 * @param instanceSupplier 创建 bean 实例的回调函数
	 * @see #rootBeanDefinition(ResolvableType, Supplier)
	 * @since 5.3.9
	 */
	public static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		return rootBeanDefinition(ResolvableType.forClass(beanClass), instanceSupplier);
	}

	/**
	 * 创建一个新的 {@code BeanDefinitionBuilder}，用于构造 {@link ChildBeanDefinition}。
	 *
	 * @param parentName 父级 bean 的名称
	 */
	public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
		return new BeanDefinitionBuilder(new ChildBeanDefinition(parentName));
	}


	/**
	 * 正在创建的 {@code BeanDefinition} 实例。
	 */
	private final AbstractBeanDefinition beanDefinition;

	/**
	 * 当前构造函数参数的索引位置。
	 */
	private int constructorArgIndex;


	/**
	 * 强制使用工厂方法。
	 */
	private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	/**
	 * 以其原始（未验证）形式返回当前 BeanDefinition 对象。
	 *
	 * @see #getBeanDefinition()
	 */
	public AbstractBeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 验证并返回创建的 BeanDefinition 对象。
	 */
	public AbstractBeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}


	/**
	 * 设置此 bean 定义的父定义名称。
	 */
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * 设置用于此定义的静态工厂方法名称，该方法将在该 bean 的类上调用。
	 */
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * 设置用于此定义的非静态工厂方法名称，并指定调用该方法的工厂实例的 bean 名称。
	 *
	 * @param factoryMethod 要调用的工厂方法名称
	 * @param factoryBean   要在其上调用指定工厂方法的 bean 的名称
	 * @since 4.3.6
	 */
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * 添加一个带索引的构造函数参数值。当前索引由内部维护，所有添加操作均按当前顺序进行。
	 */
	public BeanDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * 添加对命名 bean 的引用作为构造函数参数。
	 *
	 * @see #addConstructorArgValue(Object)
	 */
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 在给定的属性名下添加指定的属性值。
	 */
	public BeanDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * 为指定的属性添加对指定 bean 名称的引用。
	 *
	 * @param name     要添加引用的属性名称
	 * @param beanName 被引用的 bean 的名称
	 */
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 为指定 bean 上的指定属性添加自动注入标记。
	 *
	 * @param name 要标记为自动注入的属性名称
	 * @see AutowiredPropertyMarker
	 * @since 5.2
	 */
	public BeanDefinitionBuilder addAutowiredProperty(String name) {
		this.beanDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
		return this;
	}

	/**
	 * 设置此定义的初始化方法。
	 */
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * 设置此定义的销毁方法。
	 */
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * 设置此定义的作用域。
	 *
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * 设置此定义是否为抽象的。
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * 设置此定义的 bean 是否应延迟初始化。
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * 设置此定义的自动装配模式。
	 */
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		this.beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * 设置此定义的依赖检查模式。
	 */
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		this.beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * 将指定的 bean 名称添加到此定义所依赖的 bean 列表中。
	 */
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		} else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * 设置此 bean 是否为主要的自动注入候选对象。
	 *
	 * @since 5.1.11
	 */
	public BeanDefinitionBuilder setPrimary(boolean primary) {
		this.beanDefinition.setPrimary(primary);
		return this;
	}

	/**
	 * 设置此定义的角色。
	 */
	public BeanDefinitionBuilder setRole(int role) {
		this.beanDefinition.setRole(role);
		return this;
	}

	/**
	 * 设置此 bean 是否为“合成的”，即不是由应用程序自身定义的。
	 *
	 * @since 5.3.9
	 */
	public BeanDefinitionBuilder setSynthetic(boolean synthetic) {
		this.beanDefinition.setSynthetic(synthetic);
		return this;
	}

	/**
	 * 对底层 bean 定义应用给定的自定义器。
	 *
	 * @since 5.0
	 */
	public BeanDefinitionBuilder applyCustomizers(BeanDefinitionCustomizer... customizers) {
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(this.beanDefinition);
		}
		return this;
	}

}
