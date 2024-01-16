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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 负责创建与根bean定义相对应的实例的接口。
 *
 * <p>这被提取为一个策略，因为有多种可能的方法，包括使用CGLIB动态创建子类以支持方法注入。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public interface InstantiationStrategy {

	/**
	 * 返回此工厂中具有给定名称的bean的实例。
	 *
	 * @param bd       bean定义
	 * @param beanName 在此上下文中创建bean时的bean的名称。
	 *                 如果我们正在自动装配不属于工厂的bean，则名称可以为{@code null}。
	 * @param owner    拥有的BeanFactory
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) throws BeansException;

	/**
	 * 返回通过给定构造函数创建的具有给定名称的bean的实例。
	 *
	 * @param bd       bean定义
	 * @param beanName 在此上下文中创建bean时的bean的名称。
	 *                 如果我们正在自动装配不属于工厂的bean，则名称可以为{@code null}。
	 * @param owner    拥有的BeanFactory
	 * @param ctor     要使用的构造函数
	 * @param args     要应用的构造函数参数
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
					   Constructor<?> ctor, Object... args) throws BeansException;

	/**
	 * 返回通过给定工厂方法创建的具有给定名称的bean的实例。
	 *
	 * @param bd            bean定义
	 * @param beanName      在此上下文中创建bean时的bean的名称。
	 *                      如果我们正在自动装配不属于工厂的bean，则名称可以为{@code null}。
	 * @param owner         拥有的BeanFactory
	 * @param factoryBean   要在其上调用工厂方法的工厂bean实例，如果是静态工厂方法则为{@code null}
	 * @param factoryMethod 要使用的工厂方法
	 * @param args          要应用的工厂方法参数
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
					   @Nullable Object factoryBean, Method factoryMethod, Object... args)
			throws BeansException;

}
