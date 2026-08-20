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

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ErrorHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * Spring {@link TaskScheduler} 接口的实现，包装了
 * 原生的 {@link java.util.concurrent.ScheduledThreadPoolExecutor}。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
 * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
 * @see #setThreadFactory
 * @see #setErrorHandler
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy;

	private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;

	private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;

	@Nullable
	private volatile ErrorHandler errorHandler;

	private Clock clock = Clock.systemDefaultZone();

	@Nullable
	private ScheduledExecutorService scheduledExecutor;

	// 底层 ScheduledFutureTask 到用户级 ListenableFuture 句柄的映射（如果有的话）
	private final Map<Object, ListenableFuture<?>> listenableFutureMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * 设置 ScheduledExecutorService 的线程池大小。
	 * 默认值为 1。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setCorePoolSize(poolSize);
		}
		this.poolSize = poolSize;
	}

	/**
	 * 设置 {@link ScheduledThreadPoolExecutor} 的取消后删除模式。
	 * <p>默认值为 {@code false}。如果设置为 {@code true}，目标执行器将
	 * 切换到取消后删除模式（如果可能）。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @see ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy
	 */
	public void setRemoveOnCancelPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(flag);
		}
		this.removeOnCancelPolicy = flag;
	}

	/**
	 * 设置即使在此执行器关闭后是否继续执行现有的周期性任务。
	 * <p>默认值为 {@code false}。如果设置为 {@code true}，目标执行器将
	 * 切换为继续执行周期性任务（如果可能）。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setContinueExistingPeriodicTasksAfterShutdownPolicy
	 */
	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setContinueExistingPeriodicTasksAfterShutdownPolicy(flag);
		}
		this.continueExistingPeriodicTasksAfterShutdownPolicy = flag;
	}

	/**
	 * 设置即使在此执行器关闭后是否执行现有的延迟任务。
	 * <p>默认值为 {@code true}。如果设置为 {@code false}，目标执行器将
	 * 切换为丢弃剩余任务（如果可能）。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setExecuteExistingDelayedTasksAfterShutdownPolicy
	 */
	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setExecuteExistingDelayedTasksAfterShutdownPolicy(flag);
		}
		this.executeExistingDelayedTasksAfterShutdownPolicy = flag;
	}

	/**
	 * 设置自定义的 {@link ErrorHandler} 策略。
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 设置用于调度目的的时钟。
	 * <p>默认时钟是默认时区的系统时钟。
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	public void setClock(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		this.scheduledExecutor = createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			ScheduledThreadPoolExecutor scheduledPoolExecutor = (ScheduledThreadPoolExecutor) this.scheduledExecutor;
			if (this.removeOnCancelPolicy) {
				scheduledPoolExecutor.setRemoveOnCancelPolicy(true);
			}
			if (this.continueExistingPeriodicTasksAfterShutdownPolicy) {
				scheduledPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
			}
			if (!this.executeExistingDelayedTasksAfterShutdownPolicy) {
				scheduledPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			}
		}

		return this.scheduledExecutor;
	}

	/**
	 * 创建一个新的 {@link ScheduledExecutorService} 实例。
	 * <p>默认实现创建一个 {@link ScheduledThreadPoolExecutor}。
	 * 可以在子类中重写以提供自定义的 {@link ScheduledExecutorService} 实例。
	 * @param poolSize 指定的线程池大小
	 * @param threadFactory 要使用的 ThreadFactory
	 * @param rejectedExecutionHandler 要使用的 RejectedExecutionHandler
	 * @return 一个新的 ScheduledExecutorService 实例
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 返回底层的 ScheduledExecutorService 以进行原生访问。
	 * @return 底层的 ScheduledExecutorService（不为 {@code null}）
	 * @throws IllegalStateException 如果 ThreadPoolTaskScheduler 尚未初始化
	 */
	public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor != null, "ThreadPoolTaskScheduler not initialized");
		return this.scheduledExecutor;
	}

	/**
	 * 返回底层的 ScheduledThreadPoolExecutor（如果可用）。
	 * @return 底层的 ScheduledExecutorService（不为 {@code null}）
	 * @throws IllegalStateException 如果 ThreadPoolTaskScheduler 尚未初始化
	 * 或者底层的 ScheduledExecutorService 不是 ScheduledThreadPoolExecutor
	 * @see #getScheduledExecutor()
	 */
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor instanceof ScheduledThreadPoolExecutor,
				"No ScheduledThreadPoolExecutor available");
		return (ScheduledThreadPoolExecutor) this.scheduledExecutor;
	}

	/**
	 * 返回当前的线程池大小。
	 * <p>需要底层的 {@link ScheduledThreadPoolExecutor}。
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.scheduledExecutor == null) {
			// 尚未初始化：假设为初始线程池大小。
			return this.poolSize;
		}
		return getScheduledThreadPoolExecutor().getPoolSize();
	}

	/**
	 * 返回当前活跃线程的数量。
	 * <p>需要底层的 {@link ScheduledThreadPoolExecutor}。
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.scheduledExecutor == null) {
			// 尚未初始化：假设没有活跃线程。
			return 0;
		}
		return getScheduledThreadPoolExecutor().getActiveCount();
	}

	/**
	 * 返回取消后删除模式的当前设置。
	 * <p>需要底层的 {@link ScheduledThreadPoolExecutor}。
	 * @deprecated 从 5.3.9 开始，建议直接使用
	 * {@link #getScheduledThreadPoolExecutor()} 访问
	 */
	@Deprecated
	public boolean isRemoveOnCancelPolicy() {
		if (this.scheduledExecutor == null) {
			// 尚未初始化：暂时返回我们的设置。
			return this.removeOnCancelPolicy;
		}
		return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
	}


	// SchedulingTaskExecutor 实现

	@Override
	public void execute(Runnable task) {
		Executor executor = getScheduledExecutor();
		try {
			executor.execute(errorHandlingTask(task, false));
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
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			Callable<T> taskToUse = task;
			ErrorHandler errorHandler = this.errorHandler;
			if (errorHandler != null) {
				taskToUse = new DelegatingErrorHandlingCallable<>(task, errorHandler);
			}
			return executor.submit(taskToUse);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<Object> listenableFuture = new ListenableFutureTask<>(task, null);
			executeAndTrack(executor, listenableFuture);
			return listenableFuture;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<T> listenableFuture = new ListenableFutureTask<>(task);
			executeAndTrack(executor, listenableFuture);
			return listenableFuture;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	private void executeAndTrack(ExecutorService executor, ListenableFutureTask<?> listenableFuture) {
		Future<?> scheduledFuture = executor.submit(errorHandlingTask(listenableFuture, false));
		this.listenableFutureMap.put(scheduledFuture, listenableFuture);
		listenableFuture.addCallback(result -> this.listenableFutureMap.remove(scheduledFuture),
				ex -> this.listenableFutureMap.remove(scheduledFuture));
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// 同时取消关联的用户级 ListenableFuture 句柄
		ListenableFuture<?> listenableFuture = this.listenableFutureMap.get(task);
		if (listenableFuture != null) {
			listenableFuture.cancel(true);
		}
	}


	// TaskScheduler 实现

	@Override
	@Nullable
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			ErrorHandler errorHandler = this.errorHandler;
			if (errorHandler == null) {
				errorHandler = TaskUtils.getDefaultErrorHandler(true);
			}
			return new ReschedulingRunnable(task, trigger, this.clock, executor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.schedule(errorHandlingTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}


	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}


	private static class DelegatingErrorHandlingCallable<V> implements Callable<V> {

		private final Callable<V> delegate;

		private final ErrorHandler errorHandler;

		public DelegatingErrorHandlingCallable(Callable<V> delegate, ErrorHandler errorHandler) {
			this.delegate = delegate;
			this.errorHandler = errorHandler;
		}

		@Override
		@Nullable
		public V call() throws Exception {
			try {
				return this.delegate.call();
			}
			catch (Throwable ex) {
				this.errorHandler.handleError(ex);
				return null;
			}
		}
	}

}
