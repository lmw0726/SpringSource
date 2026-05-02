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

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link AspectJAwareAdvisorAutoProxyCreator} 子类，处理当前应用程序上下文中的
 * 所有 AspectJ 注解切面以及 Spring Advisor。
 *
 * <p>任何带 AspectJ 注解的类都会被自动识别；如果 Spring AOP
 * 基于代理的模型能够应用其 advice，则会应用该 advice。
 * 这涵盖方法执行连接点。
 *
 * <p>如果使用 &lt;aop:include&gt; 元素，则只会考虑名称匹配
 * include 模式的 @AspectJ bean，将它们作为用于 Spring 自动代理的切面定义。
 *
 * <p>Spring Advisor 的处理遵循
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * 中建立的规则。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory
 */
@SuppressWarnings("serial")
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

	@Nullable
	private List<Pattern> includePatterns;

	@Nullable
	private AspectJAdvisorFactory aspectJAdvisorFactory;

	@Nullable
	private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;


	/**
	 * 设置正则表达式模式列表，用于匹配合格的 @AspectJ bean 名称。
	 * <p>默认将所有 @AspectJ bean 视为合格。
	 */
	public void setIncludePatterns(List<String> patterns) {
		this.includePatterns = new ArrayList<>(patterns.size());
		for (String patternText : patterns) {
			this.includePatterns.add(Pattern.compile(patternText));
		}
	}

	public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
		Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
	}

	@Override
	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		super.initBeanFactory(beanFactory);
		if (this.aspectJAdvisorFactory == null) {
			this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
		}
		this.aspectJAdvisorsBuilder =
				new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
	}


	@Override
	protected List<Advisor> findCandidateAdvisors() {
		// 添加根据父类规则找到的所有 Spring advisor。
		List<Advisor> advisors = super.findCandidateAdvisors();
		// 为 bean 工厂中的所有 AspectJ 切面构建 Advisor。
		if (this.aspectJAdvisorsBuilder != null) {
			advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
		}
		return advisors;
	}

	@Override
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		// 以前我们在构造函数中 setProxyTargetClass(true)，但影响范围过大。
		// 现在改为重写 isInfrastructureClass 以避免代理切面。我对此并不完全满意，
		// 因为除了会导致 advice 调用经过代理之外，并没有充分理由不对切面进行 advising；
		// 如果切面实现了例如 Ordered 接口，它会通过该接口被代理，并在运行时失败，
		// 因为 advice 方法并未定义在该接口上。将来我们或许可以放宽
		// 不对切面进行 advising 的限制。
		return (super.isInfrastructureClass(beanClass) ||
				(this.aspectJAdvisorFactory != null && this.aspectJAdvisorFactory.isAspect(beanClass)));
	}

	/**
	 * 检查给定切面 bean 是否适合自动代理。
	 * <p>如果未使用 &lt;aop:include&gt; 元素，则 "includePatterns" 为
	 * {@code null}，并包含所有 bean。如果 "includePatterns" 非 null，
	 * 则必须有一个模式匹配。
	 */
	protected boolean isEligibleAspectBean(String beanName) {
		if (this.includePatterns == null) {
			return true;
		}
		else {
			for (Pattern pattern : this.includePatterns) {
				if (pattern.matcher(beanName).matches()) {
					return true;
				}
			}
			return false;
		}
	}


	/**
	 * BeanFactoryAspectJAdvisorsBuilderAdapter 的子类，委托给
	 * 周围的 AnnotationAwareAspectJAutoProxyCreator 设施。
	 */
	private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

		public BeanFactoryAspectJAdvisorsBuilderAdapter(
				ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {

			super(beanFactory, advisorFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
		}
	}

}
