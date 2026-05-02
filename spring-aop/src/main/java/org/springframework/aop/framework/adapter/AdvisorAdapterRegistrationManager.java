/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * BeanPostProcessor，将 BeanFactory 中的 {@link AdvisorAdapter} bean
 * 注册到 {@link AdvisorAdapterRegistry} 中（默认为 {@link GlobalAdvisorAdapterRegistry}）。
 *
 * <p>它工作的唯一要求是需要在应用程序上下文中定义它，
 * 以及需要被 Spring AOP 框架"识别"的"非原生" Spring AdvisorAdapters。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 27.02.2004
 * @see #setAdvisorAdapterRegistry
 * @see AdvisorAdapter
 */
public class AdvisorAdapterRegistrationManager implements BeanPostProcessor {

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();


	/**
	 * 指定要向其注册 AdvisorAdapter bean 的 AdvisorAdapterRegistry。
	 * 默认为全局 AdvisorAdapterRegistry。
	 * @see GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AdvisorAdapter){
			this.advisorAdapterRegistry.registerAdvisorAdapter((AdvisorAdapter) bean);
		}
		return bean;
	}

}
