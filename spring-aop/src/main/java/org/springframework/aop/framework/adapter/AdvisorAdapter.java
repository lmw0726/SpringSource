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

package org.springframework.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * 允许扩展 Spring AOP 框架以处理新 Advisor 和 Advice 类型的接口。
 *
 * <p>实现对象可以从自定义通知类型创建 AOP Alliance 拦截器，
 * 使这些通知类型能够在底层使用拦截的 Spring AOP 框架中使用。
 *
 * <p>大多数 Spring 用户无需实现此接口；
 * 仅当您需要向 Spring 引入更多 Advisor 或 Advice 类型时才需要实现。
 *
 * @author Rod Johnson
 */
public interface AdvisorAdapter {

	/**
	 * 此适配器是否理解此通知对象？使用包含此通知作为参数的 Advisor
	 * 调用 {@code getInterceptors} 方法是否有效？
	 * @param advice 通知，例如 BeforeAdvice
	 * @return 此适配器是否理解给定的通知对象
	 * @see #getInterceptor(org.springframework.aop.Advisor)
	 * @see org.springframework.aop.BeforeAdvice
	 */
	boolean supportsAdvice(Advice advice);

	/**
	 * 返回一个 AOP Alliance MethodInterceptor，向基于拦截的 AOP 框架
	 * 暴露给定通知的行为。
	 * <p>不必担心 Advisor 中包含的任何切点；
	 * AOP 框架将负责检查切点。
	 * @param advisor Advisor。supportsAdvice() 方法必须已
	 * 在此对象上返回 true
	 * @return 此 Advisor 的 AOP Alliance 拦截器。无需为了效率
	 * 缓存实例，因为 AOP 框架会缓存通知链。
	 */
	MethodInterceptor getInterceptor(Advisor advisor);

}
