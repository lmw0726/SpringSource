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

package org.springframework.aop.framework.autoproxy.target;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.autoproxy.TargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.aop.framework.autoproxy.TargetSourceCreator}
 * 实现的便捷超类，这些实现需要创建原型bean的多个实例。
 *
 * <p>使用内部 BeanFactory 来管理目标实例，
 * 将原始 bean 定义复制到此内部工厂。
 * 这是必要的，因为原始 BeanFactory 将只包含
 * 通过自动代理创建的代理实例。
 *
 * <p>需要在 {@link org.springframework.beans.factory.support.AbstractBeanFactory} 中运行。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 */
public abstract class AbstractBeanFactoryBasedTargetSourceCreator
		implements TargetSourceCreator, BeanFactoryAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableBeanFactory beanFactory;

	/** 内部使用的 DefaultListableBeanFactory 实例，以 bean 名称为键。 */
	private final Map<String, DefaultListableBeanFactory> internalBeanFactories =
			new HashMap<>();


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	/**
	 * 返回此 TargetSourceCreator 运行所在的 BeanFactory。
	 */
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// TargetSourceCreator 接口的实现
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public final TargetSource getTargetSource(Class<?> beanClass, String beanName) {
		AbstractBeanFactoryBasedTargetSource targetSource =
				createBeanFactoryBasedTargetSource(beanClass, beanName);
		if (targetSource == null) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuring AbstractBeanFactoryBasedTargetSource: " + targetSource);
		}

		DefaultListableBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);

		// 我们只需要覆盖此 bean 定义，因为它可能引用其他 bean，
		// 而我们很乐意采用这些 bean 的父级定义。
		// 如果有要求，始终使用原型范围。
		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		GenericBeanDefinition bdCopy = new GenericBeanDefinition(bd);
		if (isPrototypeBased()) {
			bdCopy.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

		// 完成 PrototypeTargetSource 的配置。
		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(internalBeanFactory);

		return targetSource;
	}

	/**
	 * 返回要用于指定 bean 的内部 BeanFactory。
	 * @param beanName 目标 bean 的名称
	 * @return 要使用的内部 BeanFactory
	 */
	protected DefaultListableBeanFactory getInternalBeanFactoryForBean(String beanName) {
		synchronized (this.internalBeanFactories) {
			return this.internalBeanFactories.computeIfAbsent(beanName,
					name -> buildInternalBeanFactory(this.beanFactory));
		}
	}

	/**
	 * 构建一个内部 BeanFactory 用于解析目标 bean。
	 * @param containingFactory 最初定义 bean 的包含 BeanFactory
	 * @return 一个独立的内部 BeanFactory，用于保存某些目标 bean 的副本
	 */
	protected DefaultListableBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
		// 设置父级，以便（向上容器层次结构）的引用被正确解析。
		DefaultListableBeanFactory internalBeanFactory = new DefaultListableBeanFactory(containingFactory);

		// 必要的，以便所有 BeanPostProcessors、Scopes 等都变得可用。
		internalBeanFactory.copyConfigurationFrom(containingFactory);

		// 过滤掉作为 AOP 基础设施一部分的 BeanPostProcessors，
		// 因为那些只适用于在原始工厂中定义的 bean。
		internalBeanFactory.getBeanPostProcessors().removeIf(beanPostProcessor ->
				beanPostProcessor instanceof AopInfrastructureBean);

		return internalBeanFactory;
	}

	/**
	 * 在 TargetSourceCreator 关闭时销毁内部 BeanFactory。
	 * @see #getInternalBeanFactoryForBean
	 */
	@Override
	public void destroy() {
		synchronized (this.internalBeanFactories) {
			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
				bf.destroySingletons();
			}
		}
	}


	//---------------------------------------------------------------------
	// 需要由子类实现的模板方法
	//---------------------------------------------------------------------

	/**
	 * 返回此 TargetSourceCreator 是否基于原型。
	 * 目标 bean 定义的范围将相应地设置。
	 * <p>默认为 "true"。
	 * @see org.springframework.beans.factory.config.BeanDefinition#isSingleton()
	 */
	protected boolean isPrototypeBased() {
		return true;
	}

	/**
	 * 子类必须实现此方法以返回新的 AbstractPrototypeBasedTargetSource，
	 * 如果它们希望为此 bean 创建自定义 TargetSource，或者如果它们对它不感兴趣
	 * 则返回 {@code null}，在这种情况下将不会创建特殊的目标源。
	 * 子类不应在 AbstractPrototypeBasedTargetSource 上调用
	 * {@code setTargetBeanName} 或 {@code setBeanFactory}：
	 * 此类的 {@code getTargetSource()} 实现将执行此操作。
	 * @param beanClass 要为其创建 TargetSource 的 bean 的类
	 * @param beanName bean 的名称
	 * @return AbstractPrototypeBasedTargetSource，如果不匹配则返回 {@code null}
	 */
	@Nullable
	protected abstract AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName);

}
