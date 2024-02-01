/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * {@link org.springframework.context.ApplicationContext} 的实现，支持以编程方式注册bean和消息，
 * 而不是从外部配置源中读取bean定义。主要用于测试。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerPrototype
 * @see #registerBeanDefinition
 * @see #refresh
 */
public class StaticApplicationContext extends GenericApplicationContext {
	/**
	 * 静态消息源
	 */
	private final StaticMessageSource staticMessageSource;


	/**
	 * 创建一个新的StaticApplicationContext。
	 *
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext() throws BeansException {
		this(null);
	}

	/**
	 * 创建一个具有给定父级的新的StaticApplicationContext。
	 *
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext(@Nullable ApplicationContext parent) throws BeansException {
		super(parent);

		// 初始化并注册StaticMessageSource。
		this.staticMessageSource = new StaticMessageSource();
		getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
	}


	/**
	 * 被重写为不执行任何操作，以对测试用例更宽松一些。
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 返回此上下文使用的内部StaticMessageSource。
	 * 可以用于在其上注册消息。
	 *
	 * @see #addMessage
	 */
	public final StaticMessageSource getStaticMessageSource() {
		return this.staticMessageSource;
	}

	/**
	 * 使用底层的bean工厂注册一个单例bean。
	 * <p>对于更高级的需求，请直接在底层的BeanFactory中注册。
	 *
	 * @param name  bean的名称
	 * @param clazz bean的类
	 * @throws BeansException 如果注册失败
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz) throws BeansException {
		// 创建一个GenericBeanDefinition
		GenericBeanDefinition bd = new GenericBeanDefinition();

		// 设置Bean的类
		bd.setBeanClass(clazz);

		// 获取DefaultListableBeanFactory并注册BeanDefinition
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 使用底层的bean工厂注册一个单例bean。
	 * <p>对于更高级的需求，请直接在底层的BeanFactory中注册。
	 *
	 * @param name  bean的名称
	 * @param clazz bean的类
	 * @param pvs   属性值
	 * @throws BeansException 如果注册失败
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		// 创建一个GenericBeanDefinition
		GenericBeanDefinition bd = new GenericBeanDefinition();

		// 设置Bean的类
		bd.setBeanClass(clazz);

		// 设置Bean的属性值
		bd.setPropertyValues(pvs);

		// 获取DefaultListableBeanFactory并注册BeanDefinition
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 使用底层的bean工厂注册一个原型bean。
	 * <p>对于更高级的需求，请直接在底层的BeanFactory中注册。
	 *
	 * @param name  bean的名称
	 * @param clazz bean的类
	 * @throws BeansException 如果注册失败
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz) throws BeansException {
		// 创建一个GenericBeanDefinition
		GenericBeanDefinition bd = new GenericBeanDefinition();

		// 设置Bean的作用域为SCOPE_PROTOTYPE
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		// 设置Bean的类
		bd.setBeanClass(clazz);

		// 获取DefaultListableBeanFactory并注册BeanDefinition
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 使用底层的bean工厂注册一个原型bean。
	 * <p>对于更高级的需求，请直接在底层的BeanFactory中注册。
	 *
	 * @param name  bean的名称
	 * @param clazz bean的类
	 * @param pvs   属性值
	 * @throws BeansException 如果注册失败
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		// 创建一个GenericBeanDefinition
		GenericBeanDefinition bd = new GenericBeanDefinition();

		// 设置Bean的作用域为SCOPE_PROTOTYPE
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		// 设置Bean的类
		bd.setBeanClass(clazz);

		// 设置Bean的属性值
		bd.setPropertyValues(pvs);

		// 获取DefaultListableBeanFactory并注册BeanDefinition
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 将给定的消息与给定的代码关联。
	 *
	 * @param code             查找代码
	 * @param locale           应在其中查找消息的区域设置
	 * @param defaultMessage   与此查找代码关联的消息
	 * @see #getStaticMessageSource
	 */
	public void addMessage(String code, Locale locale, String defaultMessage) {
		getStaticMessageSource().addMessage(code, locale, defaultMessage);
	}

}
