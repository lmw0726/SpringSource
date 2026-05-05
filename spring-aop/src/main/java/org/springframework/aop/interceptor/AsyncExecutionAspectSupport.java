/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.function.SingletonSupplier;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 异步方法执行切面的基类，例如
 * {@code org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * 或 {@code org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect}。
 *
 * <p>支持逐个方法级别的 <i>执行器限定</i>。
 * {@code AsyncExecutionAspectSupport} 对象必须使用默认的 {@code Executor}
 * 构造，但每个单独的方法可以进一步限定在执行时要使用的特定
 * {@code Executor} bean，例如通过注解属性。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1.2
 */
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {

	/**
	 * 要获取的 {@link TaskExecutor} bean 的默认名称："taskExecutor"。
	 * <p>请注意，初始查找是按类型进行的；这只是当在上下文中
	 * 找到多个执行器 bean 时的回退方案。
	 * @since 4.2.6
	 */
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME = "taskExecutor";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<>(16);

	private SingletonSupplier<Executor> defaultExecutor;

	private SingletonSupplier<AsyncUncaughtExceptionHandler> exceptionHandler;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 使用默认的 {@link AsyncUncaughtExceptionHandler} 创建一个新实例。
	 * @param defaultExecutor 要委托给的 {@code Executor}
	 * （通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}），
	 * 除非通过异步方法上的限定符请求了更具体的执行器，
	 * 在这种情况下，将在调用时根据封闭的 bean 工厂查找执行器
	 */
	public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor) {
		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = SingletonSupplier.of(SimpleAsyncUncaughtExceptionHandler::new);
	}

	/**
	 * 使用给定的异常处理器创建新的 {@link AsyncExecutionAspectSupport}。
	 * @param defaultExecutor 要委托给的 {@code Executor}
	 * （通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}），
	 * 除非通过异步方法上的限定符请求了更具体的执行器，
	 * 在这种情况下，将在调用时根据封闭的 bean 工厂查找执行器
	 * @param exceptionHandler 要使用的 {@link AsyncUncaughtExceptionHandler}
	 */
	public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}


	/**
	 * 使用给定的执行器和异常处理器提供者配置此切面，
	 * 如果提供者无法解析，则应用相应的默认值。
	 * @since 5.1
	 */
	public void configure(@Nullable Supplier<Executor> defaultExecutor,
			@Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = new SingletonSupplier<>(exceptionHandler, SimpleAsyncUncaughtExceptionHandler::new);
	}

	/**
	 * 提供执行异步方法时要使用的执行器。
	 * @param defaultExecutor 要委托给的 {@code Executor}
	 * （通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}），
	 * 除非通过异步方法上的限定符请求了更具体的执行器，
	 * 在这种情况下，将在调用时根据封闭的 bean 工厂查找执行器
	 * @see #getExecutorQualifier(Method)
	 * @see #setBeanFactory(BeanFactory)
	 * @see #getDefaultExecutor(BeanFactory)
	 */
	public void setExecutor(Executor defaultExecutor) {
		this.defaultExecutor = SingletonSupplier.of(defaultExecutor);
	}

	/**
	 * 提供 {@link AsyncUncaughtExceptionHandler} 用于处理
	 * 调用具有 {@code void} 返回类型的异步方法时抛出的异常。
	 */
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}

	/**
	 * 设置在按限定符查找执行器或依赖默认执行器查找算法时要使用的
	 * {@link BeanFactory}。
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 * @see #getDefaultExecutor(BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * 确定执行给定方法时要使用的特定执行器。
	 * 最好返回 {@link AsyncListenableTaskExecutor} 实现。
	 * @return 要使用的执行器（或 {@code null}，但仅当没有默认执行器可用时）
	 */
	@Nullable
	protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
		// 从缓存中获取该方法对应的执行器
		AsyncTaskExecutor executor = this.executors.get(method);
		// 如果缓存中没有
		if (executor == null) {
			Executor targetExecutor;
			// 获取方法上的执行器限定符（例如 @Async("myExecutor")）
			String qualifier = getExecutorQualifier(method);
			// 如果指定了 限定符
			if (StringUtils.hasLength(qualifier)) {
				// 根据名称从 BeanFactory 中查找对应的 Executor
				targetExecutor = findQualifiedExecutor(this.beanFactory, qualifier);
			}
			else {
				// 否则使用默认的 Executor
				targetExecutor = this.defaultExecutor.get();
			}
			// 如果没有找到任何 Executor，返回 null
			if (targetExecutor == null) {
				return null;
			}
			// 如果目标 Executor 已经是 AsyncListenableTaskExecutor 类型，直接使用
			// 否则使用 TaskExecutorAdapter 进行包装，统一接口
			executor = (targetExecutor instanceof AsyncListenableTaskExecutor ?
					(AsyncListenableTaskExecutor) targetExecutor : new TaskExecutorAdapter(targetExecutor));
			// 放入缓存
			this.executors.put(method, executor);
		}
		// 返回最终确定的异步执行器
		return executor;
	}

	/**
	 * 返回执行给定异步方法时要使用的执行器的限定符或 bean 名称，
	 * 通常以注解属性的形式指定。
	 * 返回空字符串或 {@code null} 表示未指定特定执行器，
	 * 应该使用 {@linkplain #setExecutor(Executor) 默认执行器}。
	 * @param method 要检查执行器限定符元数据的方法
	 * @return 如果指定了限定符则返回，否则返回空字符串或 {@code null}
	 * @see #determineAsyncExecutor(Method)
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 */
	@Nullable
	protected abstract String getExecutorQualifier(Method method);

	/**
	 * 检索给定限定符的目标执行器。
	 * @param qualifier 要解析的限定符
	 * @return 目标执行器，如果没有则返回 {@code null}
	 * @since 4.2.6
	 * @see #getExecutorQualifier(Method)
	 */
	@Nullable
	protected Executor findQualifiedExecutor(@Nullable BeanFactory beanFactory, String qualifier) {
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory must be set on " + getClass().getSimpleName() +
					" to access qualified executor '" + qualifier + "'");
		}
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, Executor.class, qualifier);
	}

	/**
	 * 检索或构建此通知实例的默认执行器。
	 * 此处返回的执行器将被缓存以供进一步使用。
	 * <p>默认实现会在上下文中搜索唯一的 {@link TaskExecutor} bean，
	 * 或者在其他情况下搜索名为 "taskExecutor" 的 {@link Executor} bean。
	 * 如果两者都无法解析，此实现将返回 {@code null}。
	 * @param beanFactory 用于默认执行器查找的 BeanFactory
	 * @return 默认执行器，如果没有则返回 {@code null}
	 * @since 4.2.6
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	@Nullable
	protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
		if (beanFactory != null) {
			try {
				// 搜索 TaskExecutor bean... 不是普通的 Executor，因为那也会
				// 匹配 ScheduledExecutorService，这对我们的目的来说不可用。
				// TaskExecutor 更明确地为此设计。
				return beanFactory.getBean(TaskExecutor.class);
			}
			catch (NoUniqueBeanDefinitionException ex) {
				logger.debug("Could not find unique TaskExecutor bean. " +
						"Continuing search for an Executor bean named 'taskExecutor'", ex);
				try {
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					if (logger.isInfoEnabled()) {
						logger.info("More than one TaskExecutor bean found within the context, and none is named " +
								"'taskExecutor'. Mark one of them as primary or name it 'taskExecutor' (possibly " +
								"as an alias) in order to use it for async processing: " + ex.getBeanNamesFound());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				logger.debug("Could not find default TaskExecutor bean. " +
						"Continuing search for an Executor bean named 'taskExecutor'", ex);
				try {
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					logger.info("No task executor bean found for async processing: " +
							"no bean of type TaskExecutor and no bean named 'taskExecutor' either");
				}
				// 放弃 -> 要么使用本地默认执行器，要么根本不使用...
			}
		}
		return null;
	}


	/**
	 * 使用选定的执行器实际执行给定任务的委托。
	 * @param task 要执行的任务
	 * @param executor 选定的执行器
	 * @param returnType 声明的返回类型（可能是 {@link Future} 变体）
	 * @return 执行结果（可能是相应的 {@link Future} 句柄）
	 */
	@Nullable
	protected Object doSubmit(Callable<Object> task, AsyncTaskExecutor executor, Class<?> returnType) {
		if (CompletableFuture.class.isAssignableFrom(returnType)) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return task.call();
				}
				catch (Throwable ex) {
					throw new CompletionException(ex);
				}
			}, executor);
		}
		else if (ListenableFuture.class.isAssignableFrom(returnType)) {
			return ((AsyncListenableTaskExecutor) executor).submitListenable(task);
		}
		else if (Future.class.isAssignableFrom(returnType)) {
			return executor.submit(task);
		}
		else {
			executor.submit(task);
			return null;
		}
	}

	/**
	 * 处理异步调用指定的 {@link Method} 时抛出的致命错误。
	 * <p>如果方法的返回类型是 {@link Future} 对象，
	 * 则可以通过在更高级别直接抛出来传播原始异常。
	 * 然而，对于所有其他情况，异常将不会传回客户端。
	 * 在后一种情况下，将使用当前的 {@link AsyncUncaughtExceptionHandler}
	 * 来管理此类异常。
	 * @param ex 要处理的异常
	 * @param method 被调用的方法
	 * @param params 用于调用方法的参数
	 */
	protected void handleError(Throwable ex, Method method, Object... params) throws Exception {
		if (Future.class.isAssignableFrom(method.getReturnType())) {
			ReflectionUtils.rethrowException(ex);
		}
		else {
			// 无法使用默认执行器将异常传递给调用者
			try {
				this.exceptionHandler.obtain().handleUncaughtException(ex, method, params);
			}
			catch (Throwable ex2) {
				logger.warn("Exception handler for async method '" + method.toGenericString() +
						"' threw unexpected exception itself", ex2);
			}
		}
	}

}
