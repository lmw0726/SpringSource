/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

/**
 * {@code BeanPostProcessor} 实现，基于当前 {@code BeanFactory} 中
 * 所有候选 {@code Advisor} 创建 AOP 代理。此类完全通用；
 * 它不包含用于处理任何特定切面（例如池化切面）的特殊代码。
 *
 * <p>可以过滤 advisor，例如为了在同一个工厂中使用此类型的多个后处理器，
 * 可将 {@code usePrefix} 属性设置为 true；此时只会使用以
 * DefaultAdvisorAutoProxyCreator 的 bean 名称加点号开头（例如 "aapc."）
 * 的 advisor。可通过设置 {@code advisorBeanNamePrefix} 属性，
 * 将此默认前缀从 bean 名称更改为其他值。在这种情况下也会使用分隔符（.）。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

	/** bean 名称前缀和其余部分之间的分隔符。 */
	public static final String SEPARATOR = ".";


	private boolean usePrefix = false;

	@Nullable
	private String advisorBeanNamePrefix;


	/**
	 * 设置是否只包含 bean 名称中带有特定前缀的 advisor。
	 * <p>默认值为 {@code false}，包含所有 {@code Advisor} 类型的 bean。
	 * @see #setAdvisorBeanNamePrefix
	 */
	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * 返回是否只包含 bean 名称中带有特定前缀的 advisor。
	 */
	public boolean isUsePrefix() {
		return this.usePrefix;
	}

	/**
	 * 设置 bean 名称前缀，使这些 bean 名称被此对象纳入自动代理。
	 * 应设置此前缀以避免循环引用。默认值为此对象的 bean 名称 + 一个点号。
	 * @param advisorBeanNamePrefix 排除前缀
	 */
	public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
		this.advisorBeanNamePrefix = advisorBeanNamePrefix;
	}

	/**
	 * 返回会使 bean 名称被此对象纳入自动代理的前缀。
	 */
	@Nullable
	public String getAdvisorBeanNamePrefix() {
		return this.advisorBeanNamePrefix;
	}

	@Override
	public void setBeanName(String name) {
		// 如果尚未设置基础设施 bean 名称前缀，则覆盖它。
		if (this.advisorBeanNamePrefix == null) {
			this.advisorBeanNamePrefix = name + SEPARATOR;
		}
	}


	/**
	 * 如果已激活，则将带有指定前缀的 {@code Advisor} bean 视为合格。
	 * @see #setUsePrefix
	 * @see #setAdvisorBeanNamePrefix
	 */
	@Override
	protected boolean isEligibleAdvisorBean(String beanName) {
		if (!isUsePrefix()) {
			return true;
		}
		String prefix = getAdvisorBeanNamePrefix();
		return (prefix != null && beanName.startsWith(prefix));
	}

}
