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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 异步处理方法调用的 AOP Alliance {@code MethodInterceptor}，
 * 使用给定的 {@link org.springframework.core.task.AsyncTaskExecutor}。
 * 通常与 {@link org.springframework.scheduling.annotation.Async} 注解一起使用。
 *
 * <p>就目标方法签名而言，支持任何参数类型。
 * 但是，返回类型被限制为 {@code void} 或 {@code java.util.concurrent.Future}。
 * 在后一种情况下，从代理返回的 Future 句柄将是实际的异步 Future，
 * 可用于跟踪异步方法执行的结果。然而，由于目标方法需要实现相同签名，
 * 它必须返回一个临时 Future 句柄，该句柄只是传递返回值
 * （例如 Spring 的 {@link org.springframework.scheduling.annotation.AsyncResult}
 * 或 EJB 3.1 的 {@code javax.ejb.AsyncResult}）。
 *
 * <p>当返回类型为 {@code java.util.concurrent.Future} 时，执行期间抛出的任何异常
 * 都可以由调用者访问和管理。但对于 {@code void} 返回类型，
 * 此类异常无法传回。在这种情况下，可以注册
 * {@link AsyncUncaughtExceptionHandler} 来处理此类异常。
 *
 * <p>自 Spring 3.1.2 起，首选使用 {@code AnnotationAsyncExecutionInterceptor} 子类，
 * 因为它支持结合 Spring 的 {@code @Async} 注解进行执行器限定。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.0
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 * @see org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor
 */
public class AsyncExecutionInterceptor extends AsyncExecutionAspectSupport implements MethodInterceptor, Ordered {

	/**
	 * 使用默认 {@link AsyncUncaughtExceptionHandler} 创建一个新实例。
	 * @param defaultExecutor 要委托给的 {@link Executor}
	 * （通常是 Spring {@link AsyncTaskExecutor} 或 {@link java.util.concurrent.ExecutorService}）；
	 * 自 4.2.6 起，如果未提供，则会为此拦截器构建本地执行器
	 */
	public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * 创建新的 {@code AsyncExecutionInterceptor}。
	 * @param defaultExecutor 要委托给的 {@link Executor}
	 * （通常是 Spring {@link AsyncTaskExecutor} 或 {@link java.util.concurrent.ExecutorService}）；
	 * 自 4.2.6 起，如果未提供，则会为此拦截器构建本地执行器
	 * @param exceptionHandler 要使用的 {@link AsyncUncaughtExceptionHandler}
	 */
	public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * 拦截给定方法调用，将该方法的实际调用提交给正确的任务执行器，
	 * 并立即返回给调用者。
	 * @param invocation 要拦截并异步化的方法
	 * @return 如果原始方法返回 {@code Future}，则返回 {@link Future}；
	 * 否则返回 {@code null}。
	 */
	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
		Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		final Method userDeclaredMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		AsyncTaskExecutor executor = determineAsyncExecutor(userDeclaredMethod);
		if (executor == null) {
			throw new IllegalStateException(
					"No executor specified and no default executor set on AsyncExecutionInterceptor either");
		}

		Callable<Object> task = () -> {
			try {
				Object result = invocation.proceed();
				if (result instanceof Future) {
					return ((Future<?>) result).get();
				}
			}
			catch (ExecutionException ex) {
				handleError(ex.getCause(), userDeclaredMethod, invocation.getArguments());
			}
			catch (Throwable ex) {
				handleError(ex, userDeclaredMethod, invocation.getArguments());
			}
			return null;
		};

		return doSubmit(task, executor, invocation.getMethod().getReturnType());
	}

	/**
	 * 为了兼容 Spring 3.1.2，此实现是 no-op。
	 * 子类可以重写以支持提取限定符信息，
	 * 例如通过给定方法上的注解。
	 * @return 始终为 {@code null}
	 * @since 3.1.2
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	@Nullable
	protected String getExecutorQualifier(Method method) {
		return null;
	}

	/**
	 * 此实现会在上下文中查找唯一的 {@link org.springframework.core.task.TaskExecutor} bean，
	 * 否则查找名为 "taskExecutor" 的 {@link Executor} bean。
	 * 如果两者都无法解析（例如完全没有配置 {@code BeanFactory}），
	 * 并且未找到默认值，则此实现会回退到新创建的 {@link SimpleAsyncTaskExecutor} 实例
	 * 以供本地使用。
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	@Override
	@Nullable
	protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
		Executor defaultExecutor = super.getDefaultExecutor(beanFactory);
		return (defaultExecutor != null ? defaultExecutor : new SimpleAsyncTaskExecutor());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
