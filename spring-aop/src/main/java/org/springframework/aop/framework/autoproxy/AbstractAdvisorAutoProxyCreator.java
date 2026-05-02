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

package org.springframework.aop.framework.autoproxy;

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 通用自动代理创建器，基于为每个 bean 检测到的 Advisor，
 * 为特定 bean 构建 AOP 代理。
 *
 * <p>子类可以重写 {@link #findCandidateAdvisors()} 方法，
 * 返回适用于任意对象的自定义 Advisor 列表。子类还可以
 * 重写继承的 {@link #shouldSkip} 方法，以排除某些对象
 * 使其不参与自动代理。
 *
 * <p>需要排序的 Advisor 或 advice 应使用
 * {@link org.springframework.core.annotation.Order @Order} 注解，或实现
 * {@link org.springframework.core.Ordered} 接口。此类使用
 * {@link AnnotationAwareOrderComparator} 对 advisor 排序。未使用
 * {@code @Order} 注解且未实现 {@code Ordered} 接口的 Advisor
 * 将被视为无序；它们会以未定义的顺序出现在 advisor 链末尾。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #findCandidateAdvisors
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	@Nullable
	private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}


	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

	/**
	 * 查找此类自动代理可用的所有合格 Advisor。
	 * @param beanClass 要为其查找 advisor 的类
	 * @param beanName 当前被代理的 bean 名称
	 * @return 空 List，而不是 {@code null}，
	 * 如果没有 pointcut 或拦截器
	 * @see #findCandidateAdvisors
	 * @see #sortAdvisors
	 * @see #extendAdvisors
	 */
	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

	/**
	 * 查找所有用于自动代理的候选 Advisor。
	 * @return 候选 Advisor 的 List
	 */
	protected List<Advisor> findCandidateAdvisors() {
		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

	/**
	 * 搜索给定候选 Advisor，找出所有可以应用于
	 * 指定 bean 的 Advisor。
	 * @param candidateAdvisors 候选 Advisor
	 * @param beanClass 目标的 bean 类
	 * @param beanName 目标的 bean 名称
	 * @return 可应用 Advisor 的 List
	 * @see ProxyCreationContext#getCurrentProxiedBeanName()
	 */
	protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}

	/**
	 * 返回给定名称的 Advisor bean 首先是否有资格参与代理。
	 * @param beanName Advisor bean 的名称
	 * @return 该 bean 是否合格
	 */
	protected boolean isEligibleAdvisorBean(String beanName) {
		return true;
	}

	/**
	 * 根据顺序对 advisor 排序。子类可以选择重写此方法
	 * 来自定义排序策略。
	 * @param advisors 源 Advisor List
	 * @return 排序后的 Advisor List
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		AnnotationAwareOrderComparator.sort(advisors);
		return advisors;
	}

	/**
	 * 扩展钩子，子类可以重写以注册额外的 Advisor，
	 * 参数是到目前为止获得的已排序 Advisor。
	 * <p>默认实现为空。
	 * <p>通常用于添加暴露上下文信息的 Advisor，
	 * 供后续某些 advisor 使用。
	 * @param candidateAdvisors 已被识别为适用于给定 bean 的 Advisor
	 */
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
	}

	/**
	 * 此自动代理创建器始终返回预过滤的 Advisor。
	 */
	@Override
	protected boolean advisorsPreFiltered() {
		return true;
	}


	/**
	 * BeanFactoryAdvisorRetrievalHelper 的子类，委托给
	 * 周围的 AbstractAdvisorAutoProxyCreator 设施。
	 */
	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}

}
