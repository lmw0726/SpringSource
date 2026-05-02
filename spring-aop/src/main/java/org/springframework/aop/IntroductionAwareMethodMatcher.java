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

package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * {@link MethodMatcher} 的一种特殊类型，在匹配方法时会考虑引介。
 * 例如，如果目标类上没有引介，方法匹配器可能能够更有效地优化匹配。
 *
 * @author Adrian Colyer
 * @since 2.0
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

	/**
	 * 执行静态检查，判断给定方法是否匹配。如果调用方支持扩展的
	 * IntroductionAwareMethodMatcher 接口，则可以调用此方法，
	 * 而不是 2 参数的 {@link #matches(java.lang.reflect.Method, Class)} 方法。
	 * @param method 候选方法
	 * @param targetClass 目标类
	 * @param hasIntroductions 如果我们代表其询问的对象是一个或多个引介的主体，
	 * 则为 {@code true}；否则为 {@code false}
	 * @return 此方法是否静态匹配
	 */
	boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions);

}
