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

package org.springframework.scheduling.commonj;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.naming.NamingException;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 委托给 CommonJ WorkManager 的 TaskExecutor 实现，
 * 该类实现了 {@link commonj.work.WorkManager} 接口，
 * 需要通过引用或 JNDI 名称来指定 WorkManager。
 *
 * <p><b>这是在 Spring 上下文中设置 CommonJ WorkManager 的核心便捷类。</b>
 *
 * <p>同时实现了 CommonJ WorkManager 接口本身，将所有调用委托给目标 WorkManager。
 * 因此，调用者可以选择通过 Spring TaskExecutor 接口或
 * CommonJ WorkManager 接口与此执行器进行交互。
 *
 * <p>CommonJ WorkManager 通常从应用服务器的 JNDI 环境中获取，
 * 具体定义在服务器的管理控制台中。
 *
 * <p>注意：对于符合 EE 7/8 标准的 WebLogic 和 WebSphere 版本，
 * 应优先使用 {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}，
 * 它遵循 Java EE 7/8 中的 JSR-236 规范。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated 从 5.1 版本起弃用，推荐使用基于 EE 7/8 的
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}
 */
@Deprecated
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, InitializingBean {

	@Nullable
	private WorkManager workManager;

	@Nullable
	private String workManagerName;

	@Nullable
	private WorkListener workListener;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * 指定要委托的 CommonJ WorkManager。
	 * <p>或者，您也可以指定目标 WorkManager 的 JNDI 名称。
	 * @see #setWorkManagerName
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * 设置 CommonJ WorkManager 的 JNDI 名称。
	 * <p>这可以是完全限定的 JNDI 名称，也可以是相对于当前环境命名上下文的 JNDI 名称
	 *（前提是要将 "resourceRef" 设置为 "true"）。
	 * @see #setWorkManager
	 * @see #setResourceRef
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * 指定要应用的 CommonJ WorkListener（如果有的话）。
	 * <p>此共享的 WorkListener 实例将通过此 TaskExecutor 上的所有
	 * {@link #execute} 调用传递给 WorkManager。
	 */
	public void setWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	/**
	 * 指定一个自定义的 {@link TaskDecorator}，应用于即将执行的任何 {@link Runnable}。
	 * <p>请注意，此装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，
	 * 而是应用于实际的执行回调（它可能是用户所提供任务的包装器）。
	 * <p>主要使用场景是在任务调用前后设置一些执行上下文，
	 * 或者为任务执行提供一些监控/统计功能。
	 * <p><b>注意：</b>{@code TaskDecorator} 实现中的异常处理
	 * 仅限于通过 {@code execute} 调用的普通 {@code Runnable} 执行。
	 * 对于 {@code #submit} 调用，暴露的 {@code Runnable} 将是一个
	 * {@code FutureTask}，它不会传播任何异常；您可能需要将其转型
	 * 并调用 {@code Future#get} 来处理异常。
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName == null) {
				throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
			}
			this.workManager = lookup(this.workManagerName, WorkManager.class);
		}
	}

	private WorkManager obtainWorkManager() {
		Assert.state(this.workManager != null, "No WorkManager specified");
		return this.workManager;
	}


	//-------------------------------------------------------------------------
	// Spring SchedulingTaskExecutor 接口的实现
	//-------------------------------------------------------------------------

	@Override
	public void execute(Runnable task) {
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.workListener != null) {
				obtainWorkManager().schedule(work, this.workListener);
			}
			else {
				obtainWorkManager().schedule(work);
			}
		}
		catch (WorkRejectedException ex) {
			throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
		}
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future);
		return future;
	}


	//-------------------------------------------------------------------------
	// CommonJ WorkManager 接口的实现
	//-------------------------------------------------------------------------

	@Override
	public WorkItem schedule(Work work) throws WorkException, IllegalArgumentException {
		return obtainWorkManager().schedule(work);
	}

	@Override
	public WorkItem schedule(Work work, WorkListener workListener) throws WorkException {
		return obtainWorkManager().schedule(work, workListener);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean waitForAll(Collection workItems, long timeout) throws InterruptedException {
		return obtainWorkManager().waitForAll(workItems, timeout);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Collection waitForAny(Collection workItems, long timeout) throws InterruptedException {
		return obtainWorkManager().waitForAny(workItems, timeout);
	}

}
