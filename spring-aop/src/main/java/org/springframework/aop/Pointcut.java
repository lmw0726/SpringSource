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

package org.springframework.aop;

/**
 * Spring 的核心切点抽象。
 *
 * <p>切点由一个 {@link ClassFilter} 和一个 {@link MethodMatcher} 组成。
 * 这些基本术语以及 Pointcut 本身都可以组合起来构建复合匹配条件
 * （例如通过 {@link org.springframework.aop.support.ComposablePointcut}）。
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */
public interface Pointcut {

	/**
	 * 返回此切点的 ClassFilter。
	 * @return ClassFilter（永远不会为 {@code null}）
	 */
	ClassFilter getClassFilter();

	/**
	 * 返回此切点的 MethodMatcher。
	 * @return MethodMatcher（永远不会为 {@code null}）
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * 始终匹配的规范 Pointcut 实例。
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
