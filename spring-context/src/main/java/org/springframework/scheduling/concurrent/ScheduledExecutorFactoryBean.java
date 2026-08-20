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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于创建 {@link java.util.concurrent.ScheduledExecutorService}
 *（默认为 {@link java.util.concurrent.ScheduledThreadPoolExecutor}）
 * 并将其暴露为 bean 引用的 {@link org.springframework.beans.factory.FactoryBean}。
 *
 * <p>允许注册 {@link ScheduledExecutorTask ScheduledExecutorTask}，
 * 在初始化时自动启动 {@link ScheduledExecutorService}，
 * 并在 context 销毁时取消它。在仅需在启动时静态注册任务的场景中，
 * 应用代码根本无需访问 {@link ScheduledExecutorService} 实例本身；
 * 此时 {@code ScheduledExecutorFactoryBean} 仅用于生命周期集成。
 *
 * <p>作为替代方案，可以直接通过构造器注入设置 {@link ScheduledThreadPoolExecutor} 实例，
 * 或使用指向 {@link java.util.concurrent.Executors} 类的工厂方法定义。
 * <b>特别强烈建议在配置类中的常规 {@code @Bean} 方法中使用此方式，
 * 因为使用 {@code FactoryBean} 变体将迫使你返回 {@code FactoryBean} 类型
 * 而不是 {@code ScheduledExecutorService}。</b>
 *
 * <p>注意 {@link java.util.concurrent.ScheduledExecutorService}
 * 使用的 {@link Runnable} 实例在多次执行之间是共享的，
 * 这与 Quartz 为每次执行实例化新的 Job 不同。
 *
 * <p><b>警告：</b>通过原生 {@link java.util.concurrent.ScheduledExecutorService}
 * 提交的 {@link Runnable} 在抛出异常后将从执行计划中移除。如果你希望在
 * 异常发生后继续执行，请将此 FactoryBean 的
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
 * 属性设置为 "true"。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setThreadFactory
 * @see ScheduledExecutorTask
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
@SuppressWarnings("serial")
public class ScheduledExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ScheduledExecutorService> {

	private int poolSize = 1;

	@Nullable
	private ScheduledExecutorTask[] scheduledExecutorTasks;

	private boolean removeOnCancelPolicy = false;

	private boolean continueScheduledExecutionAfterException = false;

	private boolean exposeUnconfigurableExecutor = false;

	@Nullable
	private ScheduledExecutorService exposedExecutor;


	/**
	 * 设置 ScheduledExecutorService 的线程池大小。
	 * 默认值为 1。
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
	}

	/**
	 * 向此 FactoryBean 创建的 ScheduledExecutorService 注册一组 ScheduledExecutorTask 对象。
	 * 根据每个 ScheduledExecutorTask 的设置，它将通过 ScheduledExecutorService 的
	 * schedule 方法之一进行注册。
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setScheduledExecutorTasks(ScheduledExecutorTask... scheduledExecutorTasks) {
		this.scheduledExecutorTasks = scheduledExecutorTasks;
	}

	/**
	 * 设置 {@link ScheduledThreadPoolExecutor} 的取消时移除模式。
	 * <p>默认值为 {@code false}。如果设置为 {@code true}，
	 * 目标执行器将切换到取消时移除模式（如果可能，否则回退到软模式）。
	 */
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
	}

	/**
	 * 指定在计划任务抛出异常后是否继续执行。
	 * <p>默认值为 "false"，与 {@link java.util.concurrent.ScheduledExecutorService}
	 * 的原生行为一致。将此标志切换为 "true" 可实现每个任务的异常保护执行，
	 * 即使发生异常也继续按计划执行（如同成功执行时一样）。
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate
	 */
	public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
		this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
	}

	/**
	 * 指定此 FactoryBean 是否应暴露一个不可配置的
	 * 装饰器来包装创建的执行器。
	 * <p>默认值为 "false"，将原始执行器作为 bean 引用暴露。
	 * 将此标志切换为 "true" 可严格阻止客户端修改执行器的配置。
	 * @see java.util.concurrent.Executors#unconfigurableScheduledExecutorService
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		ScheduledExecutorService executor =
				createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (executor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.debug("Could not apply remove-on-cancel policy - not a ScheduledThreadPoolExecutor");
			}
		}

		// 如有必要，注册指定的 ScheduledExecutorTask。
		if (!ObjectUtils.isEmpty(this.scheduledExecutorTasks)) {
			registerTasks(this.scheduledExecutorTasks, executor);
		}

		// 使用不可配置的装饰器包装执行器。
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableScheduledExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * 创建一个新的 {@link ScheduledExecutorService} 实例。
	 * <p>默认实现创建一个 {@link ScheduledThreadPoolExecutor}。
	 * 可以在子类中重写以提供自定义的 {@link ScheduledExecutorService} 实例。
	 * @param poolSize 指定的线程池大小
	 * @param threadFactory 要使用的 ThreadFactory
	 * @param rejectedExecutionHandler 要使用的 RejectedExecutionHandler
	 * @return 新的 ScheduledExecutorService 实例
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 在指定的 {@link ScheduledExecutorService} 上注册
	 * {@link ScheduledExecutorTask ScheduledExecutorTask}。
	 * @param tasks 指定的 ScheduledExecutorTask（不为空）
	 * @param executor 要注册任务的 ScheduledExecutorService。
	 */
	protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
		for (ScheduledExecutorTask task : tasks) {
			Runnable runnable = getRunnableToSchedule(task);
			if (task.isOneTimeTask()) {
				executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
			}
			else {
				if (task.isFixedRate()) {
					executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
				else {
					executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
			}
		}
	}

	/**
	 * 确定要为给定任务调度的实际 Runnable。
	 * <p>将任务的 Runnable 包装在
	 * {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable} 中，
	 * 该包装器会捕获并记录异常。如有必要，将根据
	 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
	 * 标志抑制异常。
	 * @param task 要调度的 ScheduledExecutorTask
	 * @return 要调度的实际 Runnable（可能是装饰器）
	 */
	protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
		return (this.continueScheduledExecutionAfterException ?
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER) :
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
	}


	@Override
	@Nullable
	public ScheduledExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ScheduledExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ScheduledExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
