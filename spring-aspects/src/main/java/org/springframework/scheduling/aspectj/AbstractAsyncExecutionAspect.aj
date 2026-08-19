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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * 抽象切面（aspect），将选定的方法以异步方式路由执行。
 *
 * <p>该切面需要被注入一个面向任务的 {@link java.util.concurrent.Executor} 实现，
 * 以便针对特定的线程池激活它；或者注入一个 {@link org.springframework.beans.factory.BeanFactory}
 * 以进行默认执行器的查找。否则它将简单地以同步方式委托所有调用。
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.0.5
 * @see #setExecutor
 * @see #setBeanFactory
 * @see #getDefaultExecutor
 */
public abstract aspect AbstractAsyncExecutionAspect extends AsyncExecutionAspectSupport {

	/**
	 * 创建一个默认执行器为 {@code null} 的 {@code AnnotationAsyncExecutionAspect}，
	 * 该执行器应改为通过 {@code #aspectOf} 和 {@link #setExecutor} 设置。
	 * {@link #setExceptionHandler} 的设置方式与此相同。
	 */
	public AbstractAsyncExecutionAspect() {
		super(null);
	}


	/**
	 * 对匹配 {@link #asyncMethod()} 切点（pointcut）的方法应用环绕通知（around advice），
	 * 将方法的实际调用提交给正确的任务执行器，并立即返回给调用者。
	 * @return 如果原方法返回 {@code Future}，则返回 {@link Future}；否则返回 {@code null}
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	Object around() : asyncMethod() {
		final MethodSignature methodSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();

		AsyncTaskExecutor executor = determineAsyncExecutor(methodSignature.getMethod());
		if (executor == null) {
			return proceed();
		}

		Callable<Object> task = new Callable<Object>() {
			public Object call() throws Exception {
				try {
					Object result = proceed();
					if (result instanceof Future) {
						return ((Future<?>) result).get();
					}
				}
				catch (Throwable ex) {
					handleError(ex, methodSignature.getMethod(), thisJoinPoint.getArgs());
				}
				return null;
			}};

		return doSubmit(task, executor, methodSignature.getReturnType());
	}

	/**
	 * 返回应应用异步通知（advice）的连接点（joinpoint）集合。
	 */
	public abstract pointcut asyncMethod();

}
