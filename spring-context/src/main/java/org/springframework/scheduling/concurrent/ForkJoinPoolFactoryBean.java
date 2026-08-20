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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 一个构建并暴露预配置 {@link ForkJoinPool} 的 Spring {@link FactoryBean}。
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

	private boolean commonPool = false;

	private int parallelism = Runtime.getRuntime().availableProcessors();

	private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

	@Nullable
	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	private boolean asyncMode = false;

	private int awaitTerminationSeconds = 0;

	@Nullable
	private ForkJoinPool forkJoinPool;


	/**
	 * 设置是否暴露 JDK 8 的"公共" {@link ForkJoinPool}。
	 * <p>默认为 "false"，即基于此 FactoryBean 上的
	 * {@link #setParallelism "parallelism"}、{@link #setThreadFactory "threadFactory"}、
	 * {@link #setUncaughtExceptionHandler "uncaughtExceptionHandler"} 和
	 * {@link #setAsyncMode "asyncMode"} 属性创建一个本地 {@link ForkJoinPool} 实例。
	 * <p><b>注意：</b>将此标志设置为 "true" 将有效忽略此 FactoryBean 上的所有其他属性，
	 * 转而复用共享的公共 JDK {@link ForkJoinPool}。这在 JDK 8 上是一个不错的选择，
	 * 但会移除应用程序自定义 ForkJoinPool 行为的能力，尤其是使用自定义线程的能力。
	 * @since 3.2
	 * @see java.util.concurrent.ForkJoinPool#commonPool()
	 */
	public void setCommonPool(boolean commonPool) {
		this.commonPool = commonPool;
	}

	/**
	 * 指定并行度级别。默认值为 {@link Runtime#availableProcessors()}。
	 */
	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	/**
	 * 设置用于创建新 ForkJoinWorkerThread 的工厂。
	 * 默认值为 {@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}。
	 */
	public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 设置内部工作线程因执行任务时遇到不可恢复错误而终止时的处理器。
	 * 默认为无。
	 */
	public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	/**
	 * 指定是否为从未被 join 的分叉任务建立本地先进先出调度模式。
	 * 在工作线程仅处理事件风格异步任务的应用程序中，
	 * 此模式（asyncMode = {@code true}）可能比默认的基于本地栈的模式更合适。
	 * 默认为 {@code false}。
	 */
	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	/**
	 * 设置此 ForkJoinPool 在关闭时最大应阻塞的秒数，以便在容器其余部分继续关闭之前，
	 * 等待剩余任务完成其执行。如果您剩余的任务可能需要访问同样由容器管理的其他资源，
	 * 此设置尤其有用。
	 * <p>默认情况下，此 ForkJoinPool 不会等待任务终止。
	 * 它将继续与容器其余部分的关闭并行地完全执行所有正在进行的任务以及队列中的所有剩余任务。
	 * 相反，如果您使用此属性指定了等待终止的时间段，
	 * 此执行器将等待给定的时间（最大值）以使任务终止。
	 * <p>请注意，此功能同样适用于 {@link #setCommonPool "commonPool"} 模式。
	 * 在这种情况下，底层的 ForkJoinPool 实际上不会终止，但会等待所有任务终止。
	 * @see java.util.concurrent.ForkJoinPool#shutdown()
	 * @see java.util.concurrent.ForkJoinPool#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	public void afterPropertiesSet() {
		this.forkJoinPool = (this.commonPool ? ForkJoinPool.commonPool() :
				new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode));
	}


	@Override
	@Nullable
	public ForkJoinPool getObject() {
		return this.forkJoinPool;
	}

	@Override
	public Class<?> getObjectType() {
		return ForkJoinPool.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.forkJoinPool != null) {
			// 对公共线程池忽略。
			this.forkJoinPool.shutdown();

			// 等待所有任务终止 - 对公共线程池同样有效。
			if (this.awaitTerminationSeconds > 0) {
				try {
					this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
