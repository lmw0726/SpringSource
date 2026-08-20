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

package org.springframework.scheduling.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * JavaBean，允许以 Bean 方式配置 {@link java.util.concurrent.ThreadPoolExecutor}
 * （通过其 "corePoolSize"、"maxPoolSize"、"keepAliveSeconds"、"queueCapacity"
 * 属性），并将其暴露为 Spring 的 {@link org.springframework.core.task.TaskExecutor}。
 * 该类也非常适合管理与监控（例如通过 JMX），
 * 提供了多个有用的属性："corePoolSize"、"maxPoolSize"、"keepAliveSeconds"
 * （均支持运行时更新）；"poolSize"、"activeCount"（仅用于自省）。
 *
 * <p>默认配置为 corePoolSize 为 1，maxPoolSize 无限制，
 * 队列容量无限制。这大致等同于
 * {@link java.util.concurrent.Executors#newSingleThreadExecutor()}，即所有任务共享
 * 单个线程。将 {@link #setQueueCapacity "queueCapacity"} 设置为 0 则模拟
 * {@link java.util.concurrent.Executors#newCachedThreadPool()}，线程池中的线程会
 * 立即扩展到可能非常大的数量。此时建议同时设置
 * {@link #setMaxPoolSize "maxPoolSize"}，以及可能更高的
 * {@link #setCorePoolSize "corePoolSize"}（另请参阅
 * {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} 的扩展模式）。
 *
 * <p><b>注意：</b>本类实现了 Spring 的
 * {@link org.springframework.core.task.TaskExecutor} 接口以及
 * {@link java.util.concurrent.Executor} 接口，其中前者为主接口，
 * 后者仅作为辅助便利接口。因此，异常处理遵循 TaskExecutor 契约
 * 而非 Executor 契约，
 * 特别是在 {@link org.springframework.core.task.TaskRejectedException} 方面。
 *
 * <p>作为替代方案，您可以直接通过构造函数注入来配置 ThreadPoolExecutor 实例，
 * 或者使用指向 {@link java.util.concurrent.Executors} 类的工厂方法定义。
 * 要将此类原始 Executor 暴露为 Spring
 * {@link org.springframework.core.task.TaskExecutor}，只需使用
 * {@link org.springframework.scheduling.concurrent.ConcurrentTaskExecutor} 适配器进行包装即可。
 *
 * @author Juergen Hoeller
 * @author Rémy Guihard
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see ThreadPoolExecutorFactoryBean
 * @see ConcurrentTaskExecutor
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	private final Object poolSizeMonitor = new Object();

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	@Nullable
	private TaskDecorator taskDecorator;

	@Nullable
	private ThreadPoolExecutor threadPoolExecutor;

	// Runnable 装饰器到用户级 FutureTask 的映射（如果不同的话）
	private final Map<Runnable, Object> decoratedTaskMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * 设置 ThreadPoolExecutor 的核心线程池大小。
	 * 默认值为 1。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 */
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
			this.corePoolSize = corePoolSize;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的核心线程池大小。
	 */
	public int getCorePoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.corePoolSize;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的最大线程池大小。
	 * 默认值为 {@code Integer.MAX_VALUE}。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
			this.maxPoolSize = maxPoolSize;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的最大线程池大小。
	 */
	public int getMaxPoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.maxPoolSize;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的线程保持活跃时间（秒）。
	 * <p>默认值为 60。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
			this.keepAliveSeconds = keepAliveSeconds;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的线程保持活跃时间（秒）。
	 */
	public int getKeepAliveSeconds() {
		synchronized (this.poolSizeMonitor) {
			return this.keepAliveSeconds;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的 BlockingQueue 容量。
	 * <p>默认值为 {@code Integer.MAX_VALUE}。
	 * <p>任何正数值将创建 LinkedBlockingQueue 实例；
	 * 其他值将创建 SynchronousQueue 实例。
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * 返回 ThreadPoolExecutor 的 BlockingQueue 容量。
	 * @since 5.3.21
	 * @see #setQueueCapacity(int)
	 */
	public int getQueueCapacity() {
		return this.queueCapacity;
	}

	/**
	 * 指定是否允许核心线程超时。这使得即使在非零队列的情况下也能
	 * 动态扩展和收缩（因为最大线程池大小仅在队列满时才会增长）。
	 * <p>默认值为 "false"。
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * 指定是否启动所有核心线程，使其空闲等待工作。
	 * <p>默认值为 "false"。
	 * @since 5.3.14
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
	 */
	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	/**
	 * 指定一个自定义 {@link TaskDecorator}，应用于即将执行的任何 {@link Runnable}。
	 * <p>请注意，该装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，
	 * 而是应用于实际的执行回调（它可能是用户所提供任务的包装器）。
	 * <p>主要用例是在任务调用前后设置执行上下文，
	 * 或为任务执行提供监控/统计功能。
	 * <p><b>注意：</b>{@code TaskDecorator} 实现中的异常处理
	 * 仅限于通过 {@code execute} 调用执行的普通 {@code Runnable}。
	 * 在 {@code #submit} 调用的情况下，暴露的 {@code Runnable} 将是一个
	 * {@code FutureTask}，它不会传播任何异常；您可能需要
	 * 进行类型转换并调用 {@code Future#get} 来评估异常。
	 * 有关如何在 {@code Future} 情况下访问异常的示例，
	 * 请参阅 {@code ThreadPoolExecutor#afterExecute} 的 Javadoc。
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}


	/**
	 * 注意：此方法将 {@link ExecutorService} 暴露给其基类，
	 * 但在内部存储实际的 {@link ThreadPoolExecutor} 句柄。
	 * 不要重写此方法来替换执行器，而仅用于
	 * 装饰其 {@code ExecutorService} 句柄或存储自定义状态。
	 */
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		ThreadPoolExecutor executor;
		if (this.taskDecorator != null) {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler) {
				@Override
				public void execute(Runnable command) {
					Runnable decorated = taskDecorator.decorate(command);
					if (decorated != command) {
						decoratedTaskMap.put(decorated, command);
					}
					super.execute(decorated);
				}
			};
		}
		else {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler);

		}

		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}
		if (this.prestartAllCoreThreads) {
			executor.prestartAllCoreThreads();
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	/**
	 * 创建供 ThreadPoolExecutor 使用的 BlockingQueue。
	 * <p>当容量值为正数时将创建 LinkedBlockingQueue 实例；
	 * 否则创建 SynchronousQueue 实例。
	 * @param queueCapacity 指定的队列容量
	 * @return BlockingQueue 实例
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<>(queueCapacity);
		}
		else {
			return new SynchronousQueue<>();
		}
	}

	/**
	 * 返回底层的 ThreadPoolExecutor 以进行原生访问。
	 * @return 底层的 ThreadPoolExecutor（不为 {@code null}）
	 * @throws IllegalStateException 如果 ThreadPoolTaskExecutor 尚未初始化
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * 返回当前线程池大小。
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.threadPoolExecutor == null) {
			// 尚未初始化：假设为核心线程池大小。
			return this.corePoolSize;
		}
		return this.threadPoolExecutor.getPoolSize();
	}

	/**
	 * 返回当前队列大小。
	 * @since 5.3.21
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public int getQueueSize() {
		if (this.threadPoolExecutor == null) {
			// 尚未初始化：假设没有排队任务。
			return 0;
		}
		return this.threadPoolExecutor.getQueue().size();
	}

	/**
	 * 返回当前活跃线程数。
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			// 尚未初始化：假设没有活跃线程。
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}


	@Override
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// 同时取消关联的用户级 Future 句柄
		Object original = this.decoratedTaskMap.get(task);
		if (original instanceof Future) {
			((Future<?>) original).cancel(true);
		}
	}

}
