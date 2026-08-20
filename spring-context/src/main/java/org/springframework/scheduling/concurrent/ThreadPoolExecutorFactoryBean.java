/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * JavaBean，允许以 bean 风格（通过 "corePoolSize"、"maxPoolSize"、"keepAliveSeconds"、
 * "queueCapacity" 属性）配置 {@link java.util.concurrent.ThreadPoolExecutor}，
 * 并将其作为原生 {@link java.util.concurrent.ExecutorService} 类型的 bean 引用暴露。
 *
 * <p>默认配置为核心池大小为 1，最大池大小和队列容量均为无限制。
 * 这大致等价于 {@link java.util.concurrent.Executors#newSingleThreadExecutor()}，
 * 所有任务共享单个线程。将 {@link #setQueueCapacity "queueCapacity"} 设置为 0 则模拟
 * {@link java.util.concurrent.Executors#newCachedThreadPool()}，线程池中的线程会立即
 * 扩展到可能非常高的数量。此时还建议设置 {@link #setMaxPoolSize "maxPoolSize"}，
 * 以及可能更高的 {@link #setCorePoolSize "corePoolSize"}
 * （另请参见 {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} 扩展模式）。
 *
 * <p>作为替代方案，你可以直接使用构造函数注入设置 {@link ThreadPoolExecutor} 实例，
 * 或使用指向 {@link java.util.concurrent.Executors} 类的工厂方法定义。
 * <b>尤其对于配置类中的常见 {@code @Bean} 方法，强烈建议使用此方式，
 * 因为使用 {@code FactoryBean} 变体会迫使你返回 {@code FactoryBean} 类型
 * 而非实际的 {@code Executor} 类型。</b>
 *
 * <p>如果你需要基于定时的 {@link java.util.concurrent.ScheduledExecutorService}，
 * 请考虑使用 {@link ScheduledExecutorFactoryBean}。

 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 */
@SuppressWarnings("serial")
public class ThreadPoolExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ExecutorService> {

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean exposeUnconfigurableExecutor = false;

	@Nullable
	private ExecutorService exposedExecutor;


	/**
	 * 设置 ThreadPoolExecutor 的核心池大小。
	 * 默认值为 1。
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	/**
	 * 设置 ThreadPoolExecutor 的最大池大小。
	 * 默认值为 {@code Integer.MAX_VALUE}。
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * 设置 ThreadPoolExecutor 的线程空闲存活秒数。
	 * 默认值为 60。
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	/**
	 * 指定是否允许核心线程超时。这使得即使使用非零队列，线程池也能动态
	 * 扩展和收缩（因为最大池大小仅在队列满时才会增长）。
	 * <p>默认值为 "false"。
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * 指定是否预启动所有核心线程，使其空闲等待工作。
	 * <p>默认值为 "false"。
	 * @since 5.3.14
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
	 */
	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	/**
	 * 设置 ThreadPoolExecutor 的 BlockingQueue 容量。
	 * 默认值为 {@code Integer.MAX_VALUE}。
	 * <p>任何正值将创建 LinkedBlockingQueue 实例；
	 * 其他值将创建 SynchronousQueue 实例。
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * 指定此 FactoryBean 是否应为创建的执行器暴露不可配置的装饰器。
	 * <p>默认值为 "false"，将原始执行器作为 bean 引用暴露。
	 * 将此标志切换为 "true" 以严格防止客户端修改执行器的配置。
	 * @see java.util.concurrent.Executors#unconfigurableExecutorService
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
		ThreadPoolExecutor executor  = createExecutor(this.corePoolSize, this.maxPoolSize,
				this.keepAliveSeconds, queue, threadFactory, rejectedExecutionHandler);
		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}
		if (this.prestartAllCoreThreads) {
			executor.prestartAllCoreThreads();
		}

		// 使用不可配置的装饰器包装执行器。
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * 创建 {@link ThreadPoolExecutor} 或其子类的新实例。
	 * <p>默认实现创建标准的 {@link ThreadPoolExecutor}。
	 * 可被覆盖以提供自定义的 {@link ThreadPoolExecutor} 子类。
	 * @param corePoolSize 指定的核心池大小
	 * @param maxPoolSize 指定的最大池大小
	 * @param keepAliveSeconds 指定的线程空闲存活时间（秒）
	 * @param queue 要使用的 BlockingQueue
	 * @param threadFactory 要使用的 ThreadFactory
	 * @param rejectedExecutionHandler 要使用的 RejectedExecutionHandler
	 * @return 新的 ThreadPoolExecutor 实例
	 * @see #afterPropertiesSet()
	 */
	protected ThreadPoolExecutor createExecutor(
			int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 创建用于 ThreadPoolExecutor 的 BlockingQueue。
	 * <p>对于正值容量将创建 LinkedBlockingQueue 实例；
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


	@Override
	@Nullable
	public ExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
