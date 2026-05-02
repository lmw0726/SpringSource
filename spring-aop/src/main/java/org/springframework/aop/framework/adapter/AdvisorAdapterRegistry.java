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

package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Advisor 适配器注册表的接口。
 *
 * <p><i>这是一个 SPI 接口，不应由任何 Spring 用户实现。</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
public interface AdvisorAdapterRegistry {

	/**
	 * 返回包装给定通知的 {@link Advisor}。
	 * <p>默认情况下应至少支持
	 * {@link org.aopalliance.intercept.MethodInterceptor}、
	 * {@link org.springframework.aop.MethodBeforeAdvice}、
	 * {@link org.springframework.aop.AfterReturningAdvice}、
	 * {@link org.springframework.aop.ThrowsAdvice}。
	 * @param advice 应该是通知的对象
	 * @return 包装给定通知的 Advisor（绝不为 {@code null}；
	 * 如果 advice 参数是 Advisor，则原样返回）
	 * @throws UnknownAdviceTypeException 如果没有注册的 advisor 适配器
	 * 可以包装假定通知
	 */
	Advisor wrap(Object advice) throws UnknownAdviceTypeException;

	/**
	 * 返回 AOP Alliance MethodInterceptor 数组，以允许
	 * 在基于拦截的框架中使用给定的 Advisor。
	 * <p>如果 {@link Advisor} 是 {@link org.springframework.aop.PointcutAdvisor}，
	 * 则不必担心与之关联的切点：只需返回一个拦截器。
	 * @param advisor 要为其查找拦截器的 Advisor
	 * @return MethodInterceptor 数组，用于暴露此 Advisor 的行为
	 * @throws UnknownAdviceTypeException 如果 Advisor 类型
	 * 不被任何注册的 AdvisorAdapter 理解
	 */
	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	/**
	 * 注册给定的 {@link AdvisorAdapter}。请注意，不需要为
	 * AOP Alliance Interceptors 或 Spring Advices 注册适配器：
	 * 这些必须由 {@code AdvisorAdapterRegistry} 实现自动识别。
	 * @param adapter 理解特定 Advisor 或 Advice 类型的 AdvisorAdapter
	 */
	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
