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

package org.springframework.jca.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.WorkManager;

/**
 * 用于JCA ResourceAdapter的{@link org.springframework.context.ApplicationContext}实现。
 * 需要使用JCA {@link javax.resource.spi.BootstrapContext}初始化，
 * 将其传递给实现{@link BootstrapContextAware}的Spring管理的bean。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringContextResourceAdapter
 * @see BootstrapContextAware
 */
public class ResourceAdapterApplicationContext extends GenericApplicationContext {

	/**
	 * 引导上下文
	 */
	private final BootstrapContext bootstrapContext;


	/**
	 * 使用给定的BootstrapContext创建一个新的ResourceAdapterApplicationContext。
	 *
	 * @param bootstrapContext ResourceAdapter启动时使用的JCA BootstrapContext
	 */
	public ResourceAdapterApplicationContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.bootstrapContext = bootstrapContext;
	}


	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 向BeanFactory添加BootstrapContextAwareProcessor的BeanPostProcessor
		beanFactory.addBeanPostProcessor(new BootstrapContextAwareProcessor(this.bootstrapContext));

		// 忽略BootstrapContextAware接口的依赖
		beanFactory.ignoreDependencyInterface(BootstrapContextAware.class);

		// 注册BootstrapContext类的可解决依赖，使用提供的BootstrapContext实例
		beanFactory.registerResolvableDependency(BootstrapContext.class, this.bootstrapContext);

		// JCA WorkManager延迟解决-可能不可用。
		// 注册WorkManager类的可解决依赖，使用提供的工厂方法从BootstrapContext中获取WorkManager实例
		beanFactory.registerResolvableDependency(WorkManager.class,
				(ObjectFactory<WorkManager>) this.bootstrapContext::getWorkManager);
	}

}
