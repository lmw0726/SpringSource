/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Interface that defines a registry for shared bean instances.
 * Can be implemented by {@link org.springframework.beans.factory.BeanFactory}
 * implementations in order to expose their singleton management facility
 * in a uniform manner.
 *
 * <p>The {@link ConfigurableBeanFactory} interface extends this interface.
 *
 * @author Juergen Hoeller
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 * @since 2.0
 */
public interface SingletonBeanRegistry {

	/**
	 * 在给定的bean名称下，在bean注册表中将给定的现有对象注册为单例。
	 * <p> 给定的实例应该是完全初始化的; 注册表将不会执行任何初始化回调 (特别是，它不会调用InitializingBean的 {@code afterPropertiesSet} 方法)。
	 * 给定的实例也不会收到任何破坏回调 (如DisposableBean的 {@code destroy} 方法)。
	 * <p> 在完整的BeanFactory中运行时: <b> 如果您的bean应该接收初始化和或销毁回调，则注册bean定义而不是现有实例。</b>
	 * <p> 通常在注册表配置期间调用，但也可以用于单例的运行时注册。
	 * 因此，注册表实现应该同步单例访问; 如果它支持BeanFactory的单例延迟初始化，则无论如何都必须这样做。
	 *
	 * @param beanName        bean名称
	 * @param singletonObject 存在的单例对象
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.DisposableBean#destroy
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * 返回在给定名称下注册的 (原始) 单例对象。
	 * <p> 仅检查已经实例化的单例; 不会为尚未实例化的单例 bean定义返回对象。
	 * <p> 此方法的主要目的是访问手动注册的单例 (请参阅 {@link #registerSingleton})。
	 * 也可以用于以原始方式访问由已经创建的bean定义定义的单例。
	 * <p><b> 注意:</b> 此查找方法不发现FactoryBean前缀或别名。
	 * 在获取单例实例之前，您需要先解析规范bean名称。
	 *
	 * @param beanName 要找的bean名称
	 * @return 已注册的单例对象，如果找不到则为 {@code null}
	 * @see ConfigurableListableBeanFactory#getBeanDefinition
	 */
	@Nullable
	Object getSingleton(String beanName);

	/**
	 * 检查此注册表是否包含具有给定名称的单例实例。
	 * <p> 仅检查已经实例化的单例; 对于尚未实例化的单例 bean定义，不会返回 {@code true}。
	 * <p> 此方法的主要目的是检查手动注册的单例 (请参阅 {@link #registerSingleton})。
	 * 也可用于检查是否已经创建了由bean定义定义的单例。
	 * <p> 要检查bean工厂是否包含具有给定名称的bean定义，请使用ListableBeanFactory的 {@code containsBeanDefinition}。
	 * 同时调用 {@code containsBeanDefinition} 和 {@code containsSingleton} 解决特定的bean工厂是否包含具有给定名称的本地bean实例。
	 * <p> 使用BeanFactory的 {@code containsBean} 进行常规检查工厂是否知道具有给定名称的bean (无论是手动注册的单例实例还是通过bean定义创建的bean)，还检查祖先工厂。
	 * <p><b> 注意:</b> 此查找方法不会发现FactoryBean前缀或别名。
	 * 在检查单例状态之前，您需要先解析规范bean名称。
	 *
	 * @param beanName 要找的bean名称
	 * @return 如果此bean工厂包含一个具有给定名称的单例实例
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 * @see org.springframework.beans.factory.BeanFactory#containsBean
	 */
	boolean containsSingleton(String beanName);

	/**
	 * 返回在此注册表中注册的单例 bean的名称。
	 * <p> 仅检查已经实例化的单例; 不会返回尚未实例化的单例 bean定义的名称。
	 * <p> 此方法的主要目的是检查手动注册的单例 (请参阅 {@link #registerSingleton})。
	 * 还可以用于检查已经创建了由bean定义定义的单例。
	 *
	 * @return 作为字符串数组的名称列表 (从不 {@code null})
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionNames
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames
	 */
	String[] getSingletonNames();

	/**
	 * 返回在此注册表中注册的单例bean的数量。
	 * <p> 仅检查已经实例化的单例; 不计算尚未实例化的单例 bean定义。
	 * <p> 此方法的主要目的是检查手动注册的singleton (请参阅 {@link #registerSingleton})。
	 * 也可以用来计算已经创建的bean定义定义的单例数。
	 *
	 * @return 单例bean的数量
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionCount
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount
	 */
	int getSingletonCount();

	/**
	 * 返回此注册表使用的单例互斥体 (用于外部合作者)。
	 *
	 * @return 互斥对象 (从不 {@ code null})
     * @since 4.2
	 */
	Object getSingletonMutex();

}
