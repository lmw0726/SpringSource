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

package org.springframework.aop.target.dynamic;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * 可刷新的 TargetSource，从 BeanFactory 中获取全新的目标 Bean。
 *
 * <p>可以被子类化以重写 {@code requiresRefresh()} 方法来抑制不必要的刷新。
 * 默认情况下，每次 "refreshCheckDelay" 过后都会执行刷新。
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see #requiresRefresh()
 * @see #setRefreshCheckDelay
 */
public class BeanFactoryRefreshableTargetSource extends AbstractRefreshableTargetSource {

	private final BeanFactory beanFactory;

	private final String beanName;


	/**
	 * 为给定的 BeanFactory 和 Bean 名称创建一个新的 BeanFactoryRefreshableTargetSource。
	 * <p>注意，传入的 BeanFactory 应该已为给定的 Bean 名称设置好适当的 Bean 定义。
	 * @param beanFactory 用于获取 Bean 的 BeanFactory
	 * @param beanName 目标 Bean 的名称
	 */
	public BeanFactoryRefreshableTargetSource(BeanFactory beanFactory, String beanName) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(beanName, "Bean name is required");
		this.beanFactory = beanFactory;
		this.beanName = beanName;
	}


	/**
	 * 获取一个全新的目标对象。
	 */
	@Override
	protected final Object freshTarget() {
		return this.obtainFreshBean(this.beanFactory, this.beanName);
	}

	/**
	 * 模板方法，子类可以重写以提供给定 BeanFactory 和 Bean 名称的全新目标对象。
	 * <p>此默认实现从 BeanFactory 中获取一个新的目标 Bean 实例。
	 * @see org.springframework.beans.factory.BeanFactory#getBean
	 */
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return beanFactory.getBean(beanName);
	}

}
