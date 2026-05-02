/*
 * Copyright 2002-2019 the original author or authors.
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
 * {@link Pointcut} 的一部分：检查目标方法是否符合应用通知的条件。
 *
 * <p>MethodMatcher 可以<b>静态</b>评估，也可以在<b>运行时</b>（动态）评估。
 * 静态匹配涉及方法和（可能的）方法属性。动态匹配还会提供
 * 特定调用的参数，以及先前应用到该连接点的通知所产生的任何影响。
 *
 * <p>如果某个实现的 {@link #isRuntime()} 方法返回 {@code false}，
 * 则可以静态执行评估，并且对于该方法的所有调用，
 * 无论参数如何，结果都相同。这意味着如果 {@link #isRuntime()} 方法
 * 返回 {@code false}，则永远不会调用 3 参数的
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法。
 *
 * <p>如果某个实现的 2 参数
 * {@link #matches(java.lang.reflect.Method, Class)} 方法返回 {@code true}，
 * 且其 {@link #isRuntime()} 方法返回 {@code true}，则会在
 * <i>相关通知每次可能执行之前立即</i>调用 3 参数的
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法，
 * 以决定是否应运行该通知。所有先前的通知（例如拦截器链中更早的拦截器）
 * 都已经运行，因此它们在参数或 ThreadLocal 状态中产生的任何状态变化
 * 都会在评估时可用。
 *
 * <p>此接口的具体实现通常应提供适当的
 * {@link Object#equals(Object)} 和 {@link Object#hashCode()} 实现，
 * 以便允许匹配器用于缓存场景 &mdash; 例如 CGLIB 生成的代理。
 *
 * @author Rod Johnson
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {

	/**
	 * 执行静态检查，判断给定方法是否匹配。
	 * <p>如果此方法返回 {@code false}，或者 {@link #isRuntime()}
	 * 方法返回 {@code false}，则不会进行运行时检查（即不会调用
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])}）。
	 * @param method 候选方法
	 * @param targetClass 目标类
	 * @return 此方法是否静态匹配
	 */
	boolean matches(Method method, Class<?> targetClass);

	/**
	 * 此 MethodMatcher 是否为动态的，也就是说，即使 2 参数 matches 方法
	 * 返回 {@code true}，运行时是否仍必须最终调用
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法？
	 * <p>可在创建 AOP 代理时调用，不需要在每次方法调用前再次调用。
	 * @return 如果静态匹配通过，是否需要通过 3 参数
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法进行运行时匹配
	 */
	boolean isRuntime();

	/**
	 * 检查此方法是否存在运行时（动态）匹配，
	 * 该方法必须已经静态匹配。
	 * <p>仅当 2 参数 matches 方法对给定方法和目标类返回 {@code true}，
	 * 且 {@link #isRuntime()} 方法返回 {@code true} 时，才会调用此方法。
	 * 在通知可能运行之前立即调用，此时通知链中更早的任何通知都已经运行。
	 * @param method 候选方法
	 * @param targetClass 目标类
	 * @param args 方法参数
	 * @return 是否存在运行时匹配
	 * @see MethodMatcher#matches(Method, Class)
	 */
	boolean matches(Method method, Class<?> targetClass, Object... args);


	/**
	 * 匹配所有方法的规范实例。
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
