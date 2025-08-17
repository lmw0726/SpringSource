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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 当{@code BeanFactory}被要求获取一个bean实例但无法找到其定义时抛出的异常。
 * 这可能指向一个不存在的bean、一个非唯一的bean，或者一个手动注册的单例实例
 * 但没有关联的bean定义。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @see NoUniqueBeanDefinitionException
 */
@SuppressWarnings("serial")
public class NoSuchBeanDefinitionException extends BeansException {

	@Nullable
	private final String beanName;

	@Nullable
	private final ResolvableType resolvableType;


	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param name 缺失bean的名称
	 */
	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' available");
		this.beanName = name;
		this.resolvableType = null;
	}

	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param name 缺失bean的名称
	 * @param message 描述问题的详细消息
	 */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' available: " + message);
		this.beanName = name;
		this.resolvableType = null;
	}

	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param type 缺失bean的所需类型
	 */
	public NoSuchBeanDefinitionException(Class<?> type) {
		this(ResolvableType.forClass(type));
	}

	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param type 缺失bean的所需类型
	 * @param message 描述问题的详细消息
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String message) {
		this(ResolvableType.forClass(type), message);
	}

	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param type 缺失bean的完整类型声明
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type) {
		super("No qualifying bean of type '" + type + "' available");
		this.beanName = null;
		this.resolvableType = type;
	}

	/**
	 * 创建一个新的{@code NoSuchBeanDefinitionException}。
	 * @param type 缺失bean的完整类型声明
	 * @param message 描述问题的详细消息
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type, String message) {
		super("No qualifying bean of type '" + type + "' available: " + message);
		this.beanName = null;
		this.resolvableType = type;
	}


	/**
	 * 如果是<em>按名称</em>查找失败，则返回缺失bean的名称。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 如果是<em>按类型</em>查找失败，则返回缺失bean的所需类型。
	 */
	@Nullable
	public Class<?> getBeanType() {
		return (this.resolvableType != null ? this.resolvableType.resolve() : null);
	}

	/**
	 * 如果是<em>按类型</em>查找失败，则返回缺失bean的所需{@link ResolvableType}。
	 * @since 4.3.4
	 */
	@Nullable
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回当只期望一个匹配bean时找到的bean数量。
	 * 对于常规的NoSuchBeanDefinitionException，这将始终为0。
	 * @see NoUniqueBeanDefinitionException
	 */
	public int getNumberOfBeansFound() {
		return 0;
	}

}
