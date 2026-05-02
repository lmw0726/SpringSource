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

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;

/**
 * 便捷的、由 Pointcut 驱动的 Advisor 实现。
 *
 * <p>这是最常用的 Advisor 实现。它可与任何切点和通知类型一起使用，
 * 但引介除外。通常不需要对此类进行子类化，
 * 也不需要实现自定义 Advisor。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPointcut
 * @see #setAdvice
 */
@SuppressWarnings("serial")
public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 创建空的 DefaultPointcutAdvisor。
	 * <p>使用前必须通过 setter 方法设置 Advice。
	 * 通常也会设置 Pointcut，但默认值为 {@code Pointcut.TRUE}。
	 */
	public DefaultPointcutAdvisor() {
	}

	/**
	 * 创建匹配所有方法的 DefaultPointcutAdvisor。
	 * <p>{@code Pointcut.TRUE} 将用作 Pointcut。
	 * @param advice 要使用的 Advice
	 */
	public DefaultPointcutAdvisor(Advice advice) {
		this(Pointcut.TRUE, advice);
	}

	/**
	 * 创建 DefaultPointcutAdvisor，指定 Pointcut 和 Advice。
	 * @param pointcut 作用于 Advice 的 Pointcut
	 * @param advice Pointcut 匹配时要运行的 Advice
	 */
	public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
		this.pointcut = pointcut;
		setAdvice(advice);
	}


	/**
	 * 指定 advice 要作用的切点。
	 * <p>默认值为 {@code Pointcut.TRUE}。
	 * @see #setAdvice
	 */
	public void setPointcut(@Nullable Pointcut pointcut) {
		this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
	}

}
