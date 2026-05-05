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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于从 BeanFactory 检索标准 Spring Advisors 的助手，
 * 用于自动代理。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AbstractAdvisorAutoProxyCreator
 */
public class BeanFactoryAdvisorRetrievalHelper {

	private static final Log logger = LogFactory.getLog(BeanFactoryAdvisorRetrievalHelper.class);

	private final ConfigurableListableBeanFactory beanFactory;

	@Nullable
	private volatile String[] cachedAdvisorBeanNames;


	/**
	 * 为给定的 BeanFactory 创建新的 BeanFactoryAdvisorRetrievalHelper。
	 * @param beanFactory 要扫描的 ListableBeanFactory
	 */
	public BeanFactoryAdvisorRetrievalHelper(ConfigurableListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	/**
	 * 在当前 bean 工厂中查找所有合格的 Advisor bean，
	 * 忽略 FactoryBeans 并排除当前正在创建的 bean。
	 * @return {@link org.springframework.aop.Advisor} bean 的列表
	 * @see #isEligibleBean
	 */
	public List<Advisor> findAdvisorBeans() {
		// 确定 Advisor bean 名称列表（如果尚未缓存）。
		// 获取缓存的 Advisor bean 名称数组
		String[] advisorNames = this.cachedAdvisorBeanNames;
		// 如果缓存为空
		if (advisorNames == null) {
			// 不要在此处初始化 FactoryBeans：我们需要让所有常规 bean
			// 保持未初始化状态，以便让自动代理创建器应用于它们！
			// ⚠️ 注意：这里不会初始化 FactoryBean
			// 原因：希望普通 bean 仍保持“未实例化”，以便后续能被 AOP 代理
			// 从 BeanFactory 中获取所有类型为 Advisor 的 bean 名称
			advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.beanFactory, Advisor.class, true, false);
			// 缓存 Advisor 名称，避免下次重复扫描
			this.cachedAdvisorBeanNames = advisorNames;
		}
		// 如果没有任何 Advisor，直接返回空列表
		if (advisorNames.length == 0) {
			return new ArrayList<>();
		}

		// 创建结果集合，用于存放最终可用的 Advisor 实例
		List<Advisor> advisors = new ArrayList<>();
		// 遍历所有 Advisor bean 名称
		for (String name : advisorNames) {
			// 判断该 bean 是否是合格的 bean
			if (isEligibleBean(name)) {
				// 如果当前 Advisor 正在创建中
				if (this.beanFactory.isCurrentlyInCreation(name)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Skipping currently created advisor '" + name + "'");
					}
				}
				else {
					try {
						// 从容器中获取 Advisor 实例，并加入结果集
						advisors.add(this.beanFactory.getBean(name, Advisor.class));
					}
					catch (BeanCreationException ex) {
						// 获取最底层异常原因
						Throwable rootCause = ex.getMostSpecificCause();
						// 如果是 Bean 当前正在创建异常
						if (rootCause instanceof BeanCurrentlyInCreationException) {
							BeanCreationException bce = (BeanCreationException) rootCause;
							// 获取导致异常的 bean 名称
							String bceBeanName = bce.getBeanName();
							// 如果这个 bean 也正处于创建中
							if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
								if (logger.isTraceEnabled()) {
									logger.trace("Skipping advisor '" + name +
											"' with dependency on currently created bean: " + ex.getMessage());
								}
								// 忽略：表示对我们要通知的 bean 的反向引用。
								// 我们要查找除当前创建的 bean 本身之外的其他 advisor。
								// 忽略该 Advisor，说明它依赖当前正在创建的 bean
								continue;
							}
						}
						// 其他异常直接抛出
						throw ex;
					}
				}
			}
		}
		return advisors;
	}

	/**
	 * 确定具有给定名称的切面 bean 是否合格。
	 * <p>默认实现始终返回 {@code true}。
	 * @param beanName 切面 bean 的名称
	 * @return bean 是否合格
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
