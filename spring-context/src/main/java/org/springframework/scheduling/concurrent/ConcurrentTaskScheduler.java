/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

/**
 * 适配器，接收一个 {@code java.util.concurrent.ScheduledExecutorService} 并为其暴露
 * Spring 的 {@link org.springframework.scheduling.TaskScheduler} 接口。
 * 继承 {@link ConcurrentTaskExecutor} 以同时实现
 * {@link org.springframework.scheduling.SchedulingTaskExecutor} 接口。
 *
 * <p>自动检测 JSR-236 的 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
 * 若可用则优先使用基于触发器的调度方式，而非 Spring 的本地触发器管理（后者最终会委托给
 * {@code java.util.concurrent.ScheduledExecutorService} API 的常规延迟调度）。
 * 在 Java EE 7 环境中进行 JSR-236 风格的查找时，建议使用 {@link DefaultManagedTaskScheduler}。
 *
 * <p>请注意，已有一个预构建的 {@link ThreadPoolTaskScheduler}，它支持以 Bean 风格定义
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}，并直接将其暴露为
 * Spring 的 {@link org.springframework.scheduling.TaskScheduler}。
 * 相比直接使用原始的 ScheduledThreadPoolExecutor 定义并单独定义本适配器类，
 * 这是一种更便捷的替代方案。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see DefaultManagedTaskScheduler
 * @see ThreadPoolTaskScheduler
 */
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

	@Nullable
	private static Class<?> managedScheduledExecutorServiceClass;

	static {
		try {
			managedScheduledExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedScheduledExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API 不可用...
			managedScheduledExecutorServiceClass = null;
		}
	}


	private ScheduledExecutorService scheduledExecutor;

	private boolean enterpriseConcurrentScheduler = false;

	@Nullable
	private ErrorHandler errorHandler;

	private Clock clock = Clock.systemDefaultZone();


	/**
	 * 创建一个新的 ConcurrentTaskScheduler，
	 * 默认使用单线程执行器。
	 * @see java.util.concurrent.Executors#newSingleThreadScheduledExecutor()
	 */
	public ConcurrentTaskScheduler() {
		super();
		this.scheduledExecutor = initScheduledExecutor(null);
	}

	/**
	 * 创建一个新的 ConcurrentTaskScheduler，使用给定的
	 * {@link java.util.concurrent.ScheduledExecutorService} 作为共享委托。
	 * <p>自动检测 JSR-236 的 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	 * 若可用则优先使用基于触发器的调度方式，而非 Spring 的本地触发器管理。
	 * @param scheduledExecutor 要委托的 {@link java.util.concurrent.ScheduledExecutorService}，
	 * 用于 {@link org.springframework.scheduling.SchedulingTaskExecutor} 和
	 * {@link TaskScheduler} 的调用
	 */
	public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
		super(scheduledExecutor);
		this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
	}

	/**
	 * 创建一个新的 ConcurrentTaskScheduler，使用给定的
	 * {@link java.util.concurrent.Executor} 和
	 * {@link java.util.concurrent.ScheduledExecutorService} 作为委托。
	 * <p>自动检测 JSR-236 的 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	 * 若可用则优先使用基于触发器的调度方式，而非 Spring 的本地触发器管理。
	 * @param concurrentExecutor 要委托的 {@link java.util.concurrent.Executor}，
	 * 用于 {@link org.springframework.scheduling.SchedulingTaskExecutor} 的调用
	 * @param scheduledExecutor 要委托的 {@link java.util.concurrent.ScheduledExecutorService}，
	 * 用于 {@link TaskScheduler} 的调用
	 */
	public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
		super(concurrentExecutor);
		this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
	}


	private ScheduledExecutorService initScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
		if (scheduledExecutor != null) {
			this.scheduledExecutor = scheduledExecutor;
			this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null &&
					managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
		}
		else {
			this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			this.enterpriseConcurrentScheduler = false;
		}
		return this.scheduledExecutor;
	}

	/**
	 * 指定要委托的 {@link java.util.concurrent.ScheduledExecutorService}。
	 * <p>自动检测 JSR-236 的 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	 * 若可用则优先使用基于触发器的调度方式，而非 Spring 的本地触发器管理。
	 * <p>注意：此设置仅对 {@link TaskScheduler} 的调用生效。
	 * 如果希望指定的执行器也适用于
	 * {@link org.springframework.scheduling.SchedulingTaskExecutor} 的调用，
	 * 请将相同的执行器引用传递给 {@link #setConcurrentExecutor}。
	 * @see #setConcurrentExecutor
	 */
	public void setScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
		initScheduledExecutor(scheduledExecutor);
	}

	/**
	 * 提供 {@link ErrorHandler} 策略。
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * 设置用于调度的时钟。
	 * <p>默认时钟为默认时区的系统时钟。
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
	@Nullable
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		try {
			if (this.enterpriseConcurrentScheduler) {
				return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
			}
			else {
				ErrorHandler errorHandler =
						(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
				return new ReschedulingRunnable(task, trigger, this.clock, this.scheduledExecutor, errorHandler).schedule();
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.schedule(decorateTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
		Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
		if (this.enterpriseConcurrentScheduler) {
			result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
		}
		return result;
	}


	/**
	 * 委托类，将 Spring 的 Trigger 适配为 JSR-236 Trigger。
	 * 分离为内部类，以避免对 JSR-236 API 的硬依赖。
	 */
	private class EnterpriseConcurrentTriggerScheduler {

		public ScheduledFuture<?> schedule(Runnable task, final Trigger trigger) {
			ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
			return executor.schedule(task, new javax.enterprise.concurrent.Trigger() {
				@Override
				@Nullable
				public Date getNextRunTime(@Nullable LastExecution le, Date taskScheduledTime) {
					return (trigger.nextExecutionTime(le != null ?
							new SimpleTriggerContext(le.getScheduledStart(), le.getRunStart(), le.getRunEnd()) :
							new SimpleTriggerContext()));
				}
				@Override
				public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
					return false;
				}
			});
		}
	}

}
