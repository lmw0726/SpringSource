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

package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Spring AOP 环绕通知（MethodInterceptor），用于包装
 * 一个 AspectJ 通知方法。暴露 ProceedingJoinPoint。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {

	public AspectJAroundAdvice(
			Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJAroundAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return false;
	}

	@Override
	protected boolean supportsProceedingJoinPoint() {
		return true;
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		// 判断当前 MethodInvocation 是否为 Spring 的 ProxyMethodInvocation
		if (!(mi instanceof ProxyMethodInvocation)) {
			// 如果不是，说明当前不在 Spring AOP 代理调用链中，直接抛异常
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		// 懒加载获取 ProceedingJoinPoint
		ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
		// 获取当前连接点匹配信息
		JoinPointMatch jpm = getJoinPointMatch(pmi);
		// 调用 Advice 方法（即 @Around 对应的方法）
		return invokeAdviceMethod(pjp, jpm, null, null);
	}

	/**
	 * 返回当前调用的 ProceedingJoinPoint，
	 * 如果尚未绑定到线程，则延迟实例化它。
	 * @param rmi 当前 Spring AOP ReflectiveMethodInvocation，
	 * 我们将使用它进行属性绑定
	 * @return 可供通知方法使用的 ProceedingJoinPoint
	 */
	protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
		return new MethodInvocationProceedingJoinPoint(rmi);
	}

}
