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

package org.springframework.core.task;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

/**
 * {@link TaskExecutor} 实现，为每个任务启动一个新线程，
 * 异步执行任务。
 *
 * <p>支持通过 "concurrencyLimit" 属性限制并发线程数。
 * 默认情况下，并发线程数无限制。
 *
 * <p><b>注意：该实现不会重用线程！</b> 对于大量短生命周期任务，
 * 建议考虑使用线程池 {@code TaskExecutor} 实现。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConcurrencyLimit
 * @see SyncTaskExecutor
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
		implements AsyncListenableTaskExecutor, Serializable {

	/**
	 * 允许任意数量的并发调用，即不限制并发数。
	 * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
	 */
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * 禁用并发，即不允许任何并发调用。
	 * @see ConcurrencyThrottleSupport#NO_CONCURRENCY
	 */
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/** 内部用于限流的适配器 */
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

	@Nullable
	private ThreadFactory threadFactory;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * 创建一个默认线程名前缀的 SimpleAsyncTaskExecutor。
	 */
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * 创建一个指定线程名前缀的 SimpleAsyncTaskExecutor。
	 * @param threadNamePrefix 新线程名的前缀
	 */
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	/**
	 * 创建一个指定外部线程工厂的 SimpleAsyncTaskExecutor。
	 * @param threadFactory 用于创建新线程的工厂
	 */
	public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}


	/**
	 * 指定用于创建新线程的外部工厂，
	 * 代替本地属性配置的线程创建策略。
	 * <p>可指定内部的 ThreadFactory Bean，或通过 JNDI 等方式获取的线程工厂引用。
	 * @see #setThreadNamePrefix
	 * @see #setThreadPriority
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 返回用于创建新线程的外部工厂（如果有）。
	 */
	@Nullable
	public final ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}

	/**
	 * 指定一个 {@link TaskDecorator}，用于装饰即将执行的 {@link Runnable}。
	 * <p>装饰器不一定直接应用于用户提供的 Runnable/Callable，
	 * 而是应用于实际执行的回调（可能是用户任务的包装）。
	 * <p>主要用于设置任务执行上下文，或提供执行监控/统计功能。
	 * <p><b>注意：</b>在 {@code TaskDecorator} 中的异常处理有限，
	 * 对于 {@code submit} 调用，Runnable 会被封装为 FutureTask，
	 * 不会传播异常；需要自行调用 Future#get 获取异常。
	 * @since 4.3
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * 设置允许的最大并发访问数。
	 * -1 表示无限制。
	 * <p>原则上该值可运行时变更，但一般设计为配置时设定。
	 * 注意不要在运行时在 -1 与具体限制间切换，
	 * 否则会导致并发计数不一致。
	 * @see #UNBOUNDED_CONCURRENCY
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * 返回最大并发访问数。
	 */
	public final int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * 返回当前限流是否生效。
	 * @return {@code true} 表示限流生效
	 * @see #getConcurrencyLimit()
	 * @see #setConcurrencyLimit
	 */
	public final boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * 在并发限流（如配置）内执行给定任务。
	 * @see #doExecute(Runnable)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * 在并发限流（如配置）内执行给定任务。
	 * <p>对于紧急任务（立即执行），直接跳过限流执行。
	 * 其他任务均受限流控制。
	 * @see #TIMEOUT_IMMEDIATE
	 * @see #doExecute(Runnable)
	 */
	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new ConcurrencyThrottlingRunnable(taskToUse));
		}
		else {
			doExecute(taskToUse);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * 实际执行任务的模板方法。
	 * <p>默认实现是创建一个新线程并启动。
	 * @param task 要执行的 Runnable
	 * @see #setThreadFactory
	 * @see #createThread
	 * @see java.lang.Thread#start()
	 */
	protected void doExecute(Runnable task) {
		Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		thread.start();
	}


	/**
	 * 并发限流适配器，使得 {@code beforeAccess()} 和 {@code afterAccess()}
	 * 方法对外部类可见。
	 */
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		@Override
		protected void beforeAccess() {
			super.beforeAccess();
		}

		@Override
		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * 该 Runnable 在目标 Runnable 执行完后调用 {@code afterAccess()}。
	 */
	private class ConcurrencyThrottlingRunnable implements Runnable {

		private final Runnable target;

		public ConcurrencyThrottlingRunnable(Runnable target) {
			this.target = target;
		}

		@Override
		public void run() {
			try {
				this.target.run();
			}
			finally {
				concurrencyThrottle.afterAccess();
			}
		}
	}

}
