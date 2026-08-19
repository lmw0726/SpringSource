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

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AdvisorAdapterRegistry} 接口的默认实现。
 * 支持 {@link org.aopalliance.intercept.MethodInterceptor}、
 * {@link org.springframework.aop.MethodBeforeAdvice}、
 * {@link org.springframework.aop.AfterReturningAdvice}、
 * {@link org.springframework.aop.ThrowsAdvice}。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * 创建新的 DefaultAdvisorAdapterRegistry，注册众所周知的适配器。
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}


	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			// 太知名了，甚至不需要适配器。
			return new DefaultPointcutAdvisor(advice);
		}
		for (AdvisorAdapter adapter : this.adapters) {
			// 检查该 Advice 是否受支持。
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		// 创建拦截器列表，用于存储转换出来的多个拦截器
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		// 获取 Advisor 中的 Advice（增强逻辑）
		Advice advice = advisor.getAdvice();
		// 如果 Advice 本身就是 MethodInterceptor，直接使用（无需适配）
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
		// 遍历所有适配器，将不同类型的 Advice 转换为 MethodInterceptor
		for (AdvisorAdapter adapter : this.adapters) {
			// 判断该适配器是否支持当前 Advice 类型
			if (adapter.supportsAdvice(advice)) {
				// 使用适配器将 Advisor 转换为 MethodInterceptor
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		// 如果没有任何适配结果，说明该 Advice 类型无法识别
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		// 返回拦截器数组（供 AOP 调用链使用）
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
