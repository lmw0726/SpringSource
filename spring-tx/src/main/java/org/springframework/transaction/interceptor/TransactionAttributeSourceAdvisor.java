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

package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 由 {@link TransactionAttributeSource} 驱动的 Advisor，
 * 仅用于为事务性方法包含 {@link TransactionInterceptor}。
 *
 * <p>由于 AOP 框架会缓存通知计算结果，通常这比直接让
 * TransactionInterceptor 运行并自行发现没有工作可做更快。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTransactionInterceptor
 * @see TransactionProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

	@Nullable
	private TransactionInterceptor transactionInterceptor;

	private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
		@Override
		@Nullable
		protected TransactionAttributeSource getTransactionAttributeSource() {
			return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
		}
	};


	/**
	 * 创建新的 TransactionAttributeSourceAdvisor。
	 */
	public TransactionAttributeSourceAdvisor() {
	}

	/**
	 * 创建新的 TransactionAttributeSourceAdvisor。
	 * @param interceptor 此 advisor 要使用的事务拦截器
	 */
	public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
		setTransactionInterceptor(interceptor);
	}


	/**
	 * 设置此 advisor 要使用的事务拦截器。
	 */
	public void setTransactionInterceptor(TransactionInterceptor interceptor) {
		this.transactionInterceptor = interceptor;
	}

	/**
	 * 设置此切点要使用的 {@link ClassFilter}。
	 * 默认值为 {@link ClassFilter#TRUE}。
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}


	@Override
	public Advice getAdvice() {
		Assert.state(this.transactionInterceptor != null, "No TransactionInterceptor set");
		return this.transactionInterceptor;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
