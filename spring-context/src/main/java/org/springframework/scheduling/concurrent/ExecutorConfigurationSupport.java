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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 设置 {@link java.util.concurrent.ExecutorService} 的基类
 * （通常是 {@link java.util.concurrent.ThreadPoolExecutor} 或
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}）。
 * 定义通用配置设置和通用生命周期处理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private long awaitTerminationMillis = 0;

	@Nullable
	private String beanName;

	@Nullable
	private ExecutorService executor;


	/**
	 * 设置 ExecutorService 线程池使用的 ThreadFactory。
	 * 默认是底层 ExecutorService 的默认线程工厂。
	 * <p>在支持 JSR-236 的 Java EE 7 或其他托管环境中，
	 * 可以考虑指定 JNDI 定位的 ManagedThreadFactory：默认情况下，
	 * 位于 "java:comp/DefaultManagedThreadFactory"。
	 * 使用 XML 中的 "jee:jndi-lookup" 命名空间元素或编程式的
	 * {@link org.springframework.jndi.JndiLocatorDelegate} 进行便捷查找。
	 * 或者，考虑使用 Spring 的 {@link DefaultManagedAwareThreadFactory}，
	 * 它可以在找不到托管线程工厂时回退到本地线程。
	 * @see java.util.concurrent.Executors#defaultThreadFactory()
	 * @see javax.enterprise.concurrent.ManagedThreadFactory
	 * @see DefaultManagedAwareThreadFactory
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * 设置 ExecutorService 使用的 RejectedExecutionHandler。
	 * 默认是 ExecutorService 的默认终止策略。
	 * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * 设置关闭时是否等待计划任务完成，
	 * 不中断运行中的任务并执行队列中的所有任务。
	 * <p>默认为 "false"，通过中断正在执行的任务并清空队列立即关闭。
	 * 如果您更倾向于完全完成任务但代价是更长的关闭阶段，请将此标志切换为 "true"。
	 * <p>请注意，Spring 的容器关闭会在正在完成的任务时继续进行。
	 * 如果您希望此执行器在容器其余部分继续关闭之前阻塞并等待任务终止
	 * （例如，为了保持任务可能需要的其他资源），
	 * 请设置 {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}
	 * 属性，而不是或除了此属性之外。
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * 设置此执行器在关闭时应阻塞的最大秒数，
	 * 以便等待剩余任务完成执行，
	 * 然后容器的其余部分继续关闭。如果您的剩余任务可能需要访问容器管理的其他资源，则此功能特别有用。
	 * <p>默认情况下，此执行器根本不会等待任务终止。
	 * 它将立即关闭，中断正在执行的任务并清空剩余任务队列 - 或者，如果
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * 标志已设置为 {@code true}，它将继续完全执行所有正在执行的任务以及队列中所有剩余任务，
	 * 与容器的其余部分关闭并行进行。
	 * <p>无论哪种情况，如果您使用此属性指定等待终止时间，
	 * 此执行器将等待给定时间（最大值）以等待任务终止。
	 * 作为经验法则，如果您同时将 "waitForTasksToCompleteOnShutdown" 设置为 {@code true}，
	 * 请在此处指定明显更高的超时时间，
	 * 因为队列中所有剩余任务仍将被执行 - 与默认关闭行为相反，
	 * 默认行为只是等待当前正在执行且未响应线程中断的任务。
	 * @see #setAwaitTerminationMillis
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
	}

	/**
	 * {@link #setAwaitTerminationSeconds} 的毫秒精度变体。
	 * @since 5.2.4
	 * @see #setAwaitTerminationSeconds
	 */
	public void setAwaitTerminationMillis(long awaitTerminationMillis) {
		this.awaitTerminationMillis = awaitTerminationMillis;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 在容器应用所有属性值后调用 {@code initialize()}。
	 * @see #initialize()
	 */
	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * 设置 ExecutorService。
	 */
	public void initialize() {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
	}

	/**
	 * 创建目标 {@link java.util.concurrent.ExecutorService} 实例。
	 * 由 {@code afterPropertiesSet} 调用。
	 * @param threadFactory 要使用的 ThreadFactory
	 * @param rejectedExecutionHandler 要使用的 RejectedExecutionHandler
	 * @return 一个新的 ExecutorService 实例
	 * @see #afterPropertiesSet()
	 */
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * 当 BeanFactory 销毁任务执行器实例时调用 {@code shutdown}。
	 * @see #shutdown()
	 */
	@Override
	public void destroy() {
		shutdown();
	}

	/**
	 * 对底层 ExecutorService 执行关闭操作。
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public void shutdown() {
		if (logger.isDebugEnabled()) {
			logger.debug("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				for (Runnable remainingTask : this.executor.shutdownNow()) {
					cancelRemainingTask(remainingTask);
				}
			}
			awaitTerminationIfNecessary(this.executor);
		}
	}

	/**
	 * 取消给定的剩余任务，该任务从未开始执行，
	 * 如 {@link ExecutorService#shutdownNow()} 返回的那样。
	 * @param task 要取消的任务（通常是 {@link RunnableFuture}）
	 * @since 5.0.5
	 * @see #shutdown()
	 * @see RunnableFuture#cancel(boolean)
	 */
	protected void cancelRemainingTask(Runnable task) {
		if (task instanceof Future) {
			((Future<?>) task).cancel(true);
		}
	}

	/**
	 * 等待执行器终止，根据 {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} 属性的值。
	 */
	private void awaitTerminationIfNecessary(ExecutorService executor) {
		if (this.awaitTerminationMillis > 0) {
			try {
				if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Timed out while waiting for executor" +
								(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
					}
				}
			}
			catch (InterruptedException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Interrupted while waiting for executor" +
							(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
				}
				Thread.currentThread().interrupt();
			}
		}
	}

}
