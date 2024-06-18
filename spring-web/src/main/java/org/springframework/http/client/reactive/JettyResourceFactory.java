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

package org.springframework.http.client.reactive;


import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * 管理 Jetty 资源的工厂，例如 {@link Executor}、{@link ByteBufferPool} 和 {@link Scheduler}，
 * 在 Spring {@code ApplicationContext} 的生命周期内管理这些资源。
 *
 * <p>该工厂实现了 {@link InitializingBean} 和 {@link DisposableBean}，
 * 通常会被声明为一个由 Spring 管理的 bean。
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public class JettyResourceFactory implements InitializingBean, DisposableBean {
	/**
	 * 要使用的执行器
	 */
	@Nullable
	private Executor executor;

	/**
	 * 字节缓冲池
	 */
	@Nullable
	private ByteBufferPool byteBufferPool;

	/**
	 * 调度程序
	 */
	@Nullable
	private Scheduler scheduler;

	/**
	 * 线程前缀
	 */
	private String threadPrefix = "jetty-http";


	/**
	 * 配置要使用的 {@link Executor}。
	 * <p>默认情况下，使用 {@link QueuedThreadPool} 进行初始化。
	 *
	 * @param executor 要使用的执行器
	 */
	public void setExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * 配置要使用的 {@link ByteBufferPool}。
	 * <p>默认情况下，使用 {@link MappedByteBufferPool} 进行初始化。
	 *
	 * @param byteBufferPool 要使用的 {@link ByteBuffer} 池
	 */
	public void setByteBufferPool(@Nullable ByteBufferPool byteBufferPool) {
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * 配置要使用的 {@link Scheduler}。
	 * <p>默认情况下，使用 {@link ScheduledExecutorScheduler} 进行初始化。
	 *
	 * @param scheduler 要使用的 {@link Scheduler}
	 */
	public void setScheduler(@Nullable Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * 配置用于初始化 {@link QueuedThreadPool} 执行器的线程前缀。仅在未提供 {@link Executor} 实例时使用。
	 * <p>默认设置为 "jetty-http"。
	 *
	 * @param threadPrefix 要使用的线程前缀
	 */
	public void setThreadPrefix(String threadPrefix) {
		Assert.notNull(threadPrefix, "Thread prefix is required");
		this.threadPrefix = threadPrefix;
	}

	/**
	 * 返回配置的 {@link Executor}。
	 *
	 * @return 配置的 {@link Executor}
	 */
	@Nullable
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * 返回配置的 {@link ByteBufferPool}。
	 *
	 * @return 配置的 {@link ByteBufferPool}
	 */
	@Nullable
	public ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * 返回配置的 {@link Scheduler}。
	 *
	 * @return 配置的 {@link Scheduler}
	 */
	@Nullable
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 根据线程前缀和对象哈希码生成线程池的名称
		String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());

		// 如果执行器（线程池）为空
		if (this.executor == null) {
			// 创建一个新的 排队线程池
			QueuedThreadPool threadPool = new QueuedThreadPool();
			// 设置名称
			threadPool.setName(name);
			// 设置执行器
			this.executor = threadPool;
		}

		// 如果字节缓冲池为空
		if (this.byteBufferPool == null) {
			// 如果执行器实现了 SizedThreadPool 接口，则使用其最大线程数的一半作为缓冲池的容量
			this.byteBufferPool = new MappedByteBufferPool(2048,
					this.executor instanceof ThreadPool.SizedThreadPool
							? ((ThreadPool.SizedThreadPool) this.executor).getMaxThreads() / 2
							// 否则，使用当前可用处理器数目的两倍作为缓冲池的容量
							: ProcessorUtils.availableProcessors() * 2);
		}

		// 如果调度器为空
		if (this.scheduler == null) {
			// 创建一个新的 ScheduledExecutorScheduler，并设置名称和非 daemon 模式
			this.scheduler = new ScheduledExecutorScheduler(name + "-scheduler", false);
		}

		// 如果执行器实现了 LifeCycle 接口
		if (this.executor instanceof LifeCycle) {
			// 启动执行器
			((LifeCycle) this.executor).start();
		}

		// 启动调度器
		this.scheduler.start();
	}

	@Override
	public void destroy() throws Exception {
		try {
			// 如果执行器实现了 LifeCycle 接口，停止执行器（线程池）
			if (this.executor instanceof LifeCycle) {
				((LifeCycle) this.executor).stop();
			}
		} catch (Throwable ex) {
			// 忽略
		}

		try {
			// 如果调度器不为 null，停止调度器
			if (this.scheduler != null) {
				this.scheduler.stop();
			}
		} catch (Throwable ex) {
			// 忽略
		}
	}

}
