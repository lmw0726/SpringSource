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

package org.springframework.context.weaving;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * 的实现，它将上下文的默认 {@link LoadTimeWeaver} 传递给实现了
 * {@link LoadTimeWeaverAware} 接口的 Bean。
 *
 * <p>{@link org.springframework.context.ApplicationContext 应用上下文}
 * 会自动将其注册到底层的 {@link BeanFactory Bean 工厂}，
 * 前提是确实存在一个默认的 {@code LoadTimeWeaver}。
 *
 * <p>应用程序不应直接使用此类。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see LoadTimeWeaverAware
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public class LoadTimeWeaverAwareProcessor implements BeanPostProcessor, BeanFactoryAware {

	@Nullable
	private LoadTimeWeaver loadTimeWeaver;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 创建一个新的 {@code LoadTimeWeaverAwareProcessor}，它将从
	 * 所在的 {@link BeanFactory} 中自动检索 {@link LoadTimeWeaver}，
	 * 期望的 Bean 名称为
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}。
	 */
	public LoadTimeWeaverAwareProcessor() {
	}

	/**
	 * 为给定的 {@link LoadTimeWeaver} 创建一个新的 {@code LoadTimeWeaverAwareProcessor}。
	 * <p>如果给定的 {@code loadTimeWeaver} 为 {@code null}，则将从所在的
	 * {@link BeanFactory} 中自动检索 {@link LoadTimeWeaver}，
	 * 期望的 Bean 名称为
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}。
	 * @param loadTimeWeaver 要使用的特定 {@code LoadTimeWeaver}
	 */
	public LoadTimeWeaverAwareProcessor(@Nullable LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * 创建一个新的 {@code LoadTimeWeaverAwareProcessor}。
	 * <p>{@code LoadTimeWeaver} 将从给定的 {@link BeanFactory} 中自动检索，
	 * 期望的 Bean 名称为
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}。
	 * @param beanFactory 用于检索 LoadTimeWeaver 的 BeanFactory
	 */
	public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof LoadTimeWeaverAware) {
			LoadTimeWeaver ltw = this.loadTimeWeaver;
			if (ltw == null) {
				Assert.state(this.beanFactory != null,
						"BeanFactory required if no LoadTimeWeaver explicitly specified");
				ltw = this.beanFactory.getBean(
						ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
			}
			((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
