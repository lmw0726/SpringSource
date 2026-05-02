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

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.BeforeAdvice;
import org.springframework.lang.Nullable;

/**
 * 处理 AspectJ Advisor 的实用方法。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AspectJAopUtils {

	/**
	 * 如果 Advisor 是 before 通知的一种形式，则返回 {@code true}。
	 */
	public static boolean isBeforeAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isBeforeAdvice();
		}
		return (anAdvisor.getAdvice() instanceof BeforeAdvice);
	}

	/**
	 * 如果 Advisor 是 after 通知的一种形式，则返回 {@code true}。
	 */
	public static boolean isAfterAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isAfterAdvice();
		}
		return (anAdvisor.getAdvice() instanceof AfterAdvice);
	}

	/**
	 * 返回此 Advisor 或其 Advice 提供的 AspectJPrecedenceInformation。
	 * 如果 Advisor 和 Advice 都没有优先级信息，此方法将返回 {@code null}。
	 */
	@Nullable
	public static AspectJPrecedenceInformation getAspectJPrecedenceInformationFor(Advisor anAdvisor) {
		if (anAdvisor instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) anAdvisor;
		}
		Advice advice = anAdvisor.getAdvice();
		if (advice instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) advice;
		}
		return null;
	}

}
