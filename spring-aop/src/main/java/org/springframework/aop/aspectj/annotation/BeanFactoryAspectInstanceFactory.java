/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 由 Spring {@link org.springframework.beans.factory.BeanFactory} 支持的
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} 实现。
 *
 * <p>请注意，如果使用原型，这可能会实例化多次，这可能不会给你期望的语义。
 * 使用 {@link LazySingletonAspectInstanceFactoryDecorator}
 * 包装此工厂以确保只返回一个新的切面实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

	private final BeanFactory beanFactory;

	private final String name;

	private final AspectMetadata aspectMetadata;


	/**
	 * 创建 BeanFactoryAspectInstanceFactory。将调用 AspectJ 来内省，
	 * 使用从 BeanFactory 为给定 bean 名称返回的类型创建 AJType 元数据。
	 * @param beanFactory 从中获取实例的 BeanFactory
	 * @param name bean 的名称
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
		this(beanFactory, name, null);
	}

	/**
	 * 创建 BeanFactoryAspectInstanceFactory，提供 AspectJ 应该内省以创建 AJType 元数据的类型。
	 * 如果 BeanFactory 可能将该类型视为子类（例如使用 CGLIB 时），并且信息应该与超类相关，则使用此方法。
	 * @param beanFactory 从中获取实例的 BeanFactory
	 * @param name bean 的名称
	 * @param type 应该由 AspectJ 内省的类型
	 * ({@code null} 表示通过 bean 名称经由 {@link BeanFactory#getType} 解析)
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, @Nullable Class<?> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(name, "Bean name must not be null");
		this.beanFactory = beanFactory;
		this.name = name;
		Class<?> resolvedType = type;
		if (type == null) {
			resolvedType = beanFactory.getType(name);
			Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
		}
		this.aspectMetadata = new AspectMetadata(resolvedType, name);
	}


	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.name);
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
				ClassUtils.getDefaultClassLoader());
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.aspectMetadata;
	}

	@Override
	@Nullable
	public Object getAspectCreationMutex() {
		if (this.beanFactory.isSingleton(this.name)) {
			// 依赖工厂提供的单例语义 -> 不需要本地锁。
			return null;
		}
		else if (this.beanFactory instanceof ConfigurableBeanFactory) {
			// 工厂不提供单例保证 -> 让我们本地加锁但
			// 重用工厂的单例锁，以防我们通知 bean 的
			// 延迟依赖隐式触发单例锁...
			return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			return this;
		}
	}

	/**
	 * 确定此工厂目标切面的顺序，可以是通过实现
	 * {@link org.springframework.core.Ordered} 接口表示的实例特定顺序
	 *（仅检查单例 bean），
	 * 也可以是通过类级别的 {@link org.springframework.core.annotation.Order}
	 * 注解表示的顺序。
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 */
	@Override
	public int getOrder() {
		Class<?> type = this.beanFactory.getType(this.name);
		if (type != null) {
			if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
				return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
			}
			return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bean name '" + this.name + "'";
	}

}
