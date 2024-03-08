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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * 支持多次调用 {@link #refresh()} 的 {@link org.springframework.context.ApplicationContext} 实现的基类，
 * 每次都会创建一个新的内部 Bean 工厂实例。通常（但不一定），这样的上下文将由一组配置位置驱动，以从中加载 bean 定义。
 *
 * <p>子类唯一需要实现的方法是 {@link #loadBeanDefinitions}，该方法在每次刷新时调用。
 * 具体的实现应该将 bean 定义加载到给定的 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 中，
 * 通常委托给一个或多个特定的 bean 定义读取器。
 *
 * <p><b>注意，WebApplicationContexts 也有一个类似的基类。</b>
 * {@link org.springframework.web.context.support.2222222222222222222222222222222222222222wwwwwwwwwww.2w3AbstractRefreshableWebApplicationContext}
 * 提供了相同的子类化策略，但此外还为 Web 环境预实现了所有上下文功能。还有一种预定义的方式来接收 Web 上下文的配置位置。
 *
 * <p>此基类的具体独立子类，按特定的 bean 定义格式读取，包括 {@link ClassPathXmlApplicationContext}
 * 和 {@link FileSystemXmlApplicationContext}，它们都派生自共同的 {@link AbstractXmlApplicationContext} 基类；
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * 支持将 {@code @Configuration} 注解的类作为 bean 定义的来源。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @since 1.1.3
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	/**
	 * 是否允许bean定义覆盖
	 */
	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	/**
	 * 是否允许循环引用
	 */
	@Nullable
	private Boolean allowCircularReferences;

	/**
	 * 此上下文的Bean工厂。
	 */
	@Nullable
	private volatile DefaultListableBeanFactory beanFactory;


	/**
	 * 创建一个没有父级的新的 {@link AbstractRefreshableApplicationContext}。
	 */
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * 使用给定的父级上下文创建一个新的 {@link AbstractRefreshableApplicationContext}。
	 *
	 * @param parent 父级上下文
	 */
	public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否允许通过注册具有相同名称的不同定义来覆盖 bean 定义，自动替换前者。
	 * 如果不允许，将抛出异常。默认值为 "true"。
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * 设置是否允许在 bean 之间存在循环引用，并自动尝试解决它们。
	 * <p>默认值为 "true"。将其关闭以在遇到循环引用时抛出异常，完全禁止它们。
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * 此实现执行刷新此上下文底层的 bean 工厂，关闭先前的 bean 工厂（如果有的话），
	 * 并为上下文生命周期的下一阶段初始化一个新的 bean 工厂。
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// 如果存在 BeanFactory，则销毁所有 bean 并关闭 BeanFactory
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}

		try {
			// 创建一个默认的可列表化的 BeanFactory 实例
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			// 设置 BeanFactory 的序列化标识
			beanFactory.setSerializationId(getId());
			// 定制 BeanFactory
			customizeBeanFactory(beanFactory);
			// 加载 Bean 定义到 BeanFactory
			loadBeanDefinitions(beanFactory);
			// 将创建的 BeanFactory 实例赋值给当前 ApplicationContext
			this.beanFactory = beanFactory;
		} catch (IOException ex) {
			// 如果发生 I/O 错误，则抛出 ApplicationContextException 异常
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * 确定此上下文当前是否持有一个 bean 工厂，
	 * 即已经刷新至少一次且尚未关闭。
	 */
	protected final boolean hasBeanFactory() {
		return (this.beanFactory != null);
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized or already closed - " +
					"call 'refresh' before accessing beans via the ApplicationContext");
		}
		return beanFactory;
	}

	/**
	 * 被覆盖为一个空操作：对于 AbstractRefreshableApplicationContext，
	 * {@link #getBeanFactory()} 在任何情况下都是对活动上下文的强烈断言。
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 为此上下文创建一个内部的 bean 工厂。
	 * 每次 {@link #refresh()} 尝试时调用。
	 * <p>默认实现创建一个
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}，
	 * 将此上下文的父上下文的{@linkplain #getInternalParentBeanFactory() 内部 bean 工厂}作为父 bean 工厂。
	 * 可以在子类中覆盖，例如自定义 DefaultListableBeanFactory 的设置。
	 *
	 * @return 此上下文的 bean 工厂
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 定制此上下文使用的内部 bean 工厂。
	 * 每次 {@link #refresh()} 尝试时调用。
	 * <p>默认实现应用此上下文的
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * 和 {@linkplain #setAllowCircularReferences "allowCircularReferences"} 设置，如果指定。
	 * 可以在子类中覆盖以定制任何 {@link DefaultListableBeanFactory} 的设置。
	 *
	 * @param beanFactory 为此上下文新创建的 bean 工厂
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * 将 bean 定义加载到给定的 bean 工厂中，通常通过委托给一个或多个 bean 定义读取器。
	 *
	 * @param beanFactory 要将 bean 定义加载到的 bean 工厂
	 * @throws BeansException 如果解析 bean 定义失败
	 * @throws IOException    如果加载 bean 定义文件失败
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
