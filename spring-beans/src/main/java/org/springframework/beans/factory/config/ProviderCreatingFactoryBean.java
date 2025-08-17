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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.inject.Provider;
import java.io.Serializable;

/**
 * 一个 {@link org.springframework.beans.factory.FactoryBean} 实现，
 * 返回一个值，该值是 JSR-330 {@link javax.inject.Provider}，
 * 而后者又返回来源于 {@link org.springframework.beans.factory.BeanFactory} 的bean。
 *
 * <p>这基本上是 Spring 传统的 {@link ObjectFactoryCreatingFactoryBean} 的
 * JSR-330 兼容变体。它可以用于传统的外部依赖注入配置，目标是类型为
 * {@code javax.inject.Provider} 的属性或构造函数参数，
 * 作为 JSR-330 {@code @Inject} 注解驱动方法的替代方案。
 *
 * @author Juergen Hoeller
 * @since 3.0.2
 * @see javax.inject.Provider
 * @see ObjectFactoryCreatingFactoryBean
 */
public class ProviderCreatingFactoryBean extends AbstractFactoryBean<Provider<Object>> {

	@Nullable
	private String targetBeanName;


	/**
	 * 设置目标bean的名称。
	 * <p>目标<i>不必</i>是非单例bean，但实际上通常都是
	 * （因为如果目标bean是单例，那么该单例bean可以直接注入到依赖对象中，
	 * 从而避免了这种工厂方法提供的额外间接层的需要）。
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return Provider.class;
	}

	@Override
	protected Provider<Object> createInstance() {
		BeanFactory beanFactory = getBeanFactory();
		Assert.state(beanFactory != null, "No BeanFactory available");
		Assert.state(this.targetBeanName != null, "No target bean name specified");
		return new TargetBeanProvider(beanFactory, this.targetBeanName);
	}


	/**
	 * 独立的内部类 - 用于序列化目的。
	 */
	@SuppressWarnings("serial")
	private static class TargetBeanProvider implements Provider<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanProvider(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object get() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
