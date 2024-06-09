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

package org.springframework.web.context.request.async;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.concurrent.Callable;

/**
 * 携带一个 {@link Callable}、一个超时值和一个任务执行器的持有者。
 *
 * @param <V> 值的类型
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

	/**
	 * 并发处理的可调用对象。
	 */
	private final Callable<V> callable;

	/**
	 * 超时时间（以毫秒为单位）。
	 */
	private Long timeout;

	/**
	 * 用于并发处理的执行器。
	 */
	private AsyncTaskExecutor executor;

	/**
	 * 要使用的执行器 bean 的名称。
	 */
	private String executorName;

	/**
	 * Bean 工厂，用于解析执行器名称。
	 */
	private BeanFactory beanFactory;

	/**
	 * 异步请求超时时要执行的回调。
	 */
	private Callable<V> timeoutCallback;

	/**
	 * 在异步请求处理期间发生错误时要调用的回调。
	 */
	private Callable<V> errorCallback;

	/**
	 * 在异步请求完成时要调用的回调。
	 */
	private Runnable completionCallback;


	/**
	 * 使用给定的 {@link Callable} 创建一个 {@code WebAsyncTask}。
	 *
	 * @param callable 并发处理的可调用对象
	 */
	public WebAsyncTask(Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
	}

	/**
	 * 使用超时值和 {@link Callable} 创建一个 {@code WebAsyncTask}。
	 *
	 * @param timeout  超时值（以毫秒为单位）
	 * @param callable 并发处理的可调用对象
	 */
	public WebAsyncTask(long timeout, Callable<V> callable) {
		this(callable);
		this.timeout = timeout;
	}

	/**
	 * 使用超时值、执行器名称和 {@link Callable} 创建一个 {@code WebAsyncTask}。
	 *
	 * @param timeout      超时值（以毫秒为单位）；如果为 {@code null} 则忽略
	 * @param executorName 要使用的执行器 bean 的名称
	 * @param callable     并发处理的可调用对象
	 */
	public WebAsyncTask(@Nullable Long timeout, String executorName, Callable<V> callable) {
		this(callable);
		Assert.notNull(executorName, "Executor name must not be null");
		this.executorName = executorName;
		this.timeout = timeout;
	}

	/**
	 * 使用超时值、执行器实例和 {@link Callable} 创建一个 {@code WebAsyncTask}。
	 *
	 * @param timeout  超时值（以毫秒为单位）；如果为 {@code null} 则忽略
	 * @param executor 要使用的执行器
	 * @param callable 并发处理的可调用对象
	 */
	public WebAsyncTask(@Nullable Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
		this(callable);
		Assert.notNull(executor, "Executor must not be null");
		this.executor = executor;
		this.timeout = timeout;
	}


	/**
	 * 返回用于并发处理的 {@link Callable}（永远不会为 {@code null}）。
	 */
	public Callable<?> getCallable() {
		return this.callable;
	}

	/**
	 * 返回超时值（以毫秒为单位），如果未设置超时，则返回 {@code null}。
	 */
	@Nullable
	public Long getTimeout() {
		return this.timeout;
	}

	/**
	 * 用于解析执行器名称的 {@link BeanFactory}。
	 * <p>当在 Spring MVC 控制器中使用 {@code WebAsyncTask} 时，此工厂引用将自动设置。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回用于并发处理的 AsyncTaskExecutor，如果未指定，则返回 {@code null}。
	 */
	@Nullable
	public AsyncTaskExecutor getExecutor() {
		// 如果已经设置了执行器，则直接返回执行器
		if (this.executor != null) {
			return this.executor;
		} else if (this.executorName != null) {
			// 如果执行器名称不为空
			Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
			// 通过 Bean工厂 按名称查找 异步任务执行器 类型的执行器 Bean
			return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
		} else {
			// 如果既没有设置执行器，也没有设置执行器名称，则返回空
			return null;
		}
	}


	/**
	 * 注册在异步请求超时时调用的代码。
	 * <p>在 {@code Callable} 完成之前，当异步请求超时时，此方法从容器线程调用。
	 * 回调在同一个线程中执行，因此应该返回而不阻塞。它可以返回要使用的替代值，包括 {@link Exception}，
	 * 或返回 {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}。
	 */
	public void onTimeout(Callable<V> callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求处理过程中发生错误时调用的代码。
	 * <p>当在 {@code Callable} 完成之前，处理异步请求时发生错误时，从容器线程调用此方法。
	 * 回调在同一个线程中执行，因此应该返回而不阻塞。它可以返回要使用的替代值，包括 {@link Exception}，
	 * 或返回 {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}。
	 *
	 * @since 5.0
	 */
	public void onError(Callable<V> callback) {
		this.errorCallback = callback;
	}

	/**
	 * 注册在异步请求完成时调用的代码。
	 * <p>当任何原因导致异步请求完成时，包括超时和网络错误时，从容器线程调用此方法。
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	CallableProcessingInterceptor getInterceptor() {
		return new CallableProcessingInterceptor() {
			@Override
			public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
				// 如果超时回调不为空，则执行超时回调；否则，返回空的结果值
				return (timeoutCallback != null ? timeoutCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}

			@Override
			public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
				// 如果错误回调不为空，则执行错误回调；否则，返回空的结果值
				return (errorCallback != null ? errorCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}

			@Override
			public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
				if (completionCallback != null) {
					// 如果完成回调函数不为空，则执行回调函数
					completionCallback.run();
				}
			}
		};
	}

}
