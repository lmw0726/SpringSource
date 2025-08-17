/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 保存特定类的 bean 装配元数据信息的持有者。与
 * {@link org.springframework.beans.factory.annotation.Configurable} 注解
 * 以及 AspectJ 的 {@code AnnotationBeanConfigurerAspect} 配合使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see BeanWiringInfoResolver
 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory
 * @see org.springframework.beans.factory.annotation.Configurable
 */
public class BeanWiringInfo {

	/**
	 * 常量，表示按名称自动装配 bean 属性。
	 * @see #BeanWiringInfo(int, boolean)
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 常量，表示按类型自动装配 bean 属性。
	 * @see #BeanWiringInfo(int, boolean)
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;


	@Nullable
	private String beanName;

	private boolean isDefaultBeanName = false;

	private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;

	private boolean dependencyCheck = false;


	/**
	 * 创建一个默认的 BeanWiringInfo，表示对工厂和后处理器回调的普通初始化，
	 * 这些回调可能是 bean 类所期望的。
	 */
	public BeanWiringInfo() {
	}

	/**
	 * 创建一个新的 BeanWiringInfo，指向给定的 bean 名称。
	 * @param beanName 要从中获取属性值的 bean 定义的名称
	 * @throws IllegalArgumentException 如果提供的 beanName 为 {@code null}，
	 * 为空，或完全由空白字符组成
	 */
	public BeanWiringInfo(String beanName) {
		this(beanName, false);
	}

	/**
	 * 创建一个新的 BeanWiringInfo，指向给定的 bean 名称。
	 * @param beanName 要从中获取属性值的 bean 定义的名称
	 * @param isDefaultBeanName 指定的 bean 名称是否为建议的默认 bean 名称，
	 * 不一定与实际的 bean 定义匹配
	 * @throws IllegalArgumentException 如果提供的 beanName 为 {@code null}，
	 * 为空，或完全由空白字符组成
	 */
	public BeanWiringInfo(String beanName, boolean isDefaultBeanName) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.isDefaultBeanName = isDefaultBeanName;
	}

	/**
	 * 创建一个新的 BeanWiringInfo，表示自动装配。
	 * @param autowireMode 使用常量 {@link #AUTOWIRE_BY_NAME} /
	 * {@link #AUTOWIRE_BY_TYPE} 之一
	 * @param dependencyCheck 是否在自动装配后对 bean 实例中的对象引用进行依赖检查
	 * @throws IllegalArgumentException 如果提供的 {@code autowireMode}
	 * 不是允许的值之一
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public BeanWiringInfo(int autowireMode, boolean dependencyCheck) {
		if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
			throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
		}
		this.autowireMode = autowireMode;
		this.dependencyCheck = dependencyCheck;
	}


	/**
	 * 返回此 BeanWiringInfo 是否表示自动装配。
	 */
	public boolean indicatesAutowiring() {
		return (this.beanName == null);
	}

	/**
	 * 返回此 BeanWiringInfo 指向的特定 bean 名称（如果有）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回特定的 bean 名称是否为建议的默认 bean 名称，
	 * 不一定与工厂中的实际 bean 定义匹配。
	 */
	public boolean isDefaultBeanName() {
		return this.isDefaultBeanName;
	}

	/**
	 * 返回常量 {@link #AUTOWIRE_BY_NAME} / {@link #AUTOWIRE_BY_TYPE}
	 * 之一，如果表示自动装配。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回是否在 bean 实例中对对象引用进行依赖检查（在自动装配之后）。
	 */
	public boolean getDependencyCheck() {
		return this.dependencyCheck;
	}

}
