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

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * 也是静态切点的 Advisor 的便捷基类。
 * 如果 Advice 和子类可序列化，则它也可序列化。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class StaticMethodMatcherPointcutAdvisor extends StaticMethodMatcherPointcut
		implements PointcutAdvisor, Ordered, Serializable {

	private Advice advice = EMPTY_ADVICE;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 创建新的 StaticMethodMatcherPointcutAdvisor，
	 * 期望通过 Bean 风格进行配置。
	 * @see #setAdvice
	 */
	public StaticMethodMatcherPointcutAdvisor() {
	}

	/**
	 * 为给定通知创建新的 StaticMethodMatcherPointcutAdvisor。
	 * @param advice 要使用的 Advice
	 */
	public StaticMethodMatcherPointcutAdvisor(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setAdvice(Advice advice) {
		this.advice = advice;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public boolean isPerInstance() {
		return true;
	}

	@Override
	public Pointcut getPointcut() {
		return this;
	}

}
