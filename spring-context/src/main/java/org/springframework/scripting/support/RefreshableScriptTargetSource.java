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

package org.springframework.scripting.support;

import org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * {@link BeanFactoryRefreshableTargetSource} 的子类，
 * 通过给定的 {@link ScriptFactory} 来确定是否需要刷新。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
public class RefreshableScriptTargetSource extends BeanFactoryRefreshableTargetSource {

	private final ScriptFactory scriptFactory;

	private final ScriptSource scriptSource;

	private final boolean isFactoryBean;


	/**
	 * 创建一个新的 RefreshableScriptTargetSource。
	 * @param beanFactory 用于获取脚本化 Bean 的 BeanFactory
	 * @param beanName 目标 Bean 的名称
	 * @param scriptFactory 委托以确定是否需要刷新的 ScriptFactory
	 * @param scriptSource 脚本定义的 ScriptSource
	 * @param isFactoryBean 目标脚本是否定义了一个 FactoryBean
	 */
	public RefreshableScriptTargetSource(BeanFactory beanFactory, String beanName,
			ScriptFactory scriptFactory, ScriptSource scriptSource, boolean isFactoryBean) {

		super(beanFactory, beanName);
		Assert.notNull(scriptFactory, "ScriptFactory must not be null");
		Assert.notNull(scriptSource, "ScriptSource must not be null");
		this.scriptFactory = scriptFactory;
		this.scriptSource = scriptSource;
		this.isFactoryBean = isFactoryBean;
	}


	/**
	 * 通过调用 ScriptFactory 的 {@code requiresScriptedObjectRefresh} 方法
	 * 来确定是否需要刷新。
	 * @see ScriptFactory#requiresScriptedObjectRefresh(ScriptSource)
	 */
	@Override
	protected boolean requiresRefresh() {
		return this.scriptFactory.requiresScriptedObjectRefresh(this.scriptSource);
	}

	/**
	 * 获取一个全新的目标对象，如有必要则获取 FactoryBean。
	 */
	@Override
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return super.obtainFreshBean(beanFactory,
				(this.isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName));
	}

}
