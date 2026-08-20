/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

/**
 * {@link AsyncExecutionInterceptor} 的特化实现，它将方法执行委托给基于 {@link Async} 注解的 {@code Executor}。专门设计用于支持 Spring 3.1.2 引入的 {@link Async#value()} 执行器限定符机制。支持通过方法或声明类级别的 {@code @Async} 检测限定符元数据。详见 {@link #getExecutorQualifier(Method)}。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1.2
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * 使用给定的执行器和简单的 {@link AsyncUncaughtExceptionHandler} 创建新的 {@code AnnotationAsyncExecutionInterceptor}。
	 * @param defaultExecutor 如果在方法级别没有通过 {@link Async#value()} 指定更具体的执行器时使用的默认执行器；从 4.2.6 版本开始，如果未指定，则为此拦截器构建一个本地执行器
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * 使用给定的执行器创建新的 {@code AnnotationAsyncExecutionInterceptor}。
	 * @param defaultExecutor 如果在方法级别没有通过 {@link Async#value()} 指定更具体的执行器时使用的默认执行器；从 4.2.6 版本开始，如果未指定，则为此拦截器构建一个本地执行器
	 * @param exceptionHandler 用于处理具有 {@code void} 返回类型的异步方法执行所抛出异常的 {@link AsyncUncaughtExceptionHandler}
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * 返回执行给定方法时要使用的执行器的限定符或 Bean 名称，该限定符通过方法或声明类级别的 {@link Async#value} 指定。如果在方法和类级别都指定了 {@code @Async}，则方法的 {@code #value} 优先（即使是空字符串，表示应优先使用默认执行器）。
	 * @param method 要检查执行器限定符元数据的方法
	 * @return 如果指定了限定符则返回限定符，否则返回空字符串表示应使用 {@linkplain #setExecutor(Executor) 默认执行器}
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	@Nullable
	protected String getExecutorQualifier(Method method) {
		// 维护者注意：此处所做的更改也应在 AnnotationAsyncExecutionAspect#getExecutorQualifier 中进行
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}

}
