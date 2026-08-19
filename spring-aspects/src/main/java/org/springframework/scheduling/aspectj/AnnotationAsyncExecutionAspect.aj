/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scheduling.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;

/**
 * 基于 Spring 的 {@link Async} 注解对方法进行路由的切面（aspect）。
 *
 * <p>该切面（aspect）对标记了 {@link Async} 注解的方法进行路由，同时也会路由到标记了
 * 相同注解的类中的方法。任何期望被异步（async）路由的方法都必须返回 {@code void}、
 * {@link Future} 或 {@link Future} 的子类型（特别是 Spring 的
 * {@link org.springframework.util.concurrent.ListenableFuture}）。因此，对于违反此
 * 返回类型约束的方法，该切面（aspect）将产生编译期错误。但是，如果标记了 {@code @Async}
 * 的类中包含违反此约束的方法，则只产生警告。
 *
 * <p>该切面（aspect）需要被注入一个面向任务的 {@link java.util.concurrent.Executor}
 * 实现，以便针对特定的线程池激活它；或者注入一个
 * {@link org.springframework.beans.factory.BeanFactory} 用于默认的
 * executor（执行器）查找。否则，它将简单地同步委托所有调用。
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 3.0.5
 * @see #setExecutor
 * @see #setBeanFactory
 * @see #getDefaultExecutor
 */
public aspect AnnotationAsyncExecutionAspect extends AbstractAsyncExecutionAspect {

	private pointcut asyncMarkedMethod() : execution(@Async (void || Future+) *(..));

	private pointcut asyncTypeMarkedMethod() : execution((void || Future+) (@Async *).*(..));

	public pointcut asyncMethod() : asyncMarkedMethod() || asyncTypeMarkedMethod();


	/**
	 * 该实现会检查给定的方法及其声明类上是否有 {@code @Async} 注解，
	 * 并返回由 {@link Async#value()} 表示的限定符（qualifier）值。
	 * 如果 {@code @Async} 同时标注在方法级别和类级别，则方法的
	 * {@code #value} 优先（即使是空字符串，也表明应优先使用默认的
	 * executor（执行器））。
	 * @return 如果指定了限定符（qualifier）则返回该限定符，否则返回空字符串，
	 * 表示应使用 {@linkplain #setExecutor 默认的 executor（执行器）}
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	protected String getExecutorQualifier(Method method) {
		// 维护者注：此处所做的更改也应同步到
		// AnnotationAsyncExecutionInterceptor#getExecutorQualifier
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}


	declare error:
		execution(@Async !(void || Future+) *(..)):
		"Only methods that return void or Future may have an @Async annotation";

	declare warning:
		execution(!(void || Future+) (@Async *).*(..)):
		"Methods in a class marked with @Async that do not return void or Future will be routed synchronously";

}
