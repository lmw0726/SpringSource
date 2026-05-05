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

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 处理 AspectJ 代理的实用方法。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AspectJProxyUtils {

	/**
	 * 如有必要，添加特殊的 Advisor 以与包含 AspectJ Advisor 的代理链一起工作：
	 * 具体来说，在列表开头添加 {@link ExposeInvocationInterceptor}。
	 * <p>这将暴露当前的 Spring AOP 调用（某些 AspectJ 切点匹配需要），
	 * 并使当前的 AspectJ JoinPoint 可用。如果 Advisor 链中没有 AspectJ Advisor，
	 * 则该调用不会产生任何效果。
	 * @param advisors 可用的 Advisor
	 * @return 如果将 {@link ExposeInvocationInterceptor} 添加到列表中，则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
		// 不要在空列表中添加 advisor；这可能意味着根本不需要进行代理
		if (!advisors.isEmpty()) {
			boolean foundAspectJAdvice = false;
			for (Advisor advisor : advisors) {
				// 注意：不要在没有保护的情况下直接获取 Advice，
				// 因为这可能会提前实例化一个非单例的 AspectJ 切面……
				if (isAspectJAdvice(advisor)) {
					foundAspectJAdvice = true;
					break;
				}
			}
			if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
				advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定的 Advisor 是否包含 AspectJ 通知。
	 * @param advisor 要检查的 Advisor
	 */
	private static boolean isAspectJAdvice(Advisor advisor) {
		return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
				advisor.getAdvice() instanceof AbstractAspectJAdvice ||
				(advisor instanceof PointcutAdvisor &&
						((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
	}

	static boolean isVariableName(@Nullable String name) {
		if (!StringUtils.hasLength(name)) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(name.charAt(0))) {
			return false;
		}
		for (int i = 1; i < name.length(); i++) {
			if (!Character.isJavaIdentifierPart(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

}
