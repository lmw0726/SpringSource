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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedTask;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 适配器，接受一个 {@code java.util.concurrent.Executor} 并将其暴露为
 * Spring 的 {@link org.springframework.core.task.TaskExecutor}。
 * 同时检测扩展的 {@code java.util.concurrent.ExecutorService}，
 * 相应地适配 {@link org.springframework.core.task.AsyncTaskExecutor} 接口。
 *
 * <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService}，
 * 以便为其暴露 {@link javax.enterprise.concurrent.ManagedTask} 适配器，
 * 基于 {@link SchedulingAwareRunnable} 暴露长时间运行提示，并基于给定的
 * Runnable/Callable 的 {@code toString()} 提供标识名称。对于 Java EE 7 环境中的
 * JSR-236 风格查找，请考虑使用 {@link DefaultManagedTaskExecutor}。
 *
 * <p>请注意，已有一个预构建的 {@link ThreadPoolTaskExecutor}，允许以 bean 风格
 * 定义 {@link java.util.concurrent.ThreadPoolExecutor}，并直接将其暴露为
 * Spring 的 {@link org.springframework.core.task.TaskExecutor}。
 * 这是原始 ThreadPoolExecutor 定义配合本适配器类单独定义的一种便捷替代方案。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see DefaultManagedTaskExecutor
 * @see ThreadPoolTaskExecutor
 */
public class ConcurrentTaskExecutor implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	@Nullable
	private static Class<?> managedExecutorServiceClass;

	static {
		try {
			managedExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API 不可用...
			managedExecutorServiceClass = null;
		}
	}

	private Executor concurrentExecutor;

	private TaskExecutorAdapter adaptedExecutor;


	/**
		 * 创建新的 ConcurrentTaskExecutor，使用单线程执行器作为默认值。
	 * @see java.util.concurrent.Executors#newSingleThreadExecutor()
	 */
	public ConcurrentTaskExecutor() {
		this.concurrentExecutor = Executors.newSingleThreadExecutor();
		this.adaptedExecutor = new TaskExecutorAdapter(this.concurrentExecutor);
	}

	/**
		 * 创建新的 ConcurrentTaskExecutor，使用给定的 {@link java.util.concurrent.Executor}。
		 * <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService}，
		 * 以便为其暴露 {@link javax.enterprise.concurrent.ManagedTask} 适配器。
		 * @param executor 要委托的 {@link java.util.concurrent.Executor}
	 */
	public ConcurrentTaskExecutor(@Nullable Executor executor) {
		this.concurrentExecutor = (executor != null ? executor : Executors.newSingleThreadExecutor());
		this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
	}


	/**
		 * 指定要委托的 {@link java.util.concurrent.Executor}。
		 * <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService}，
		 * 以便为其暴露 {@link javax.enterprise.concurrent.ManagedTask} 适配器。
	 */
	public final void setConcurrentExecutor(@Nullable Executor executor) {
		this.concurrentExecutor = (executor != null ? executor : Executors.newSingleThreadExecutor());
		this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
	}

	/**
		 * 返回此适配器委托的 {@link java.util.concurrent.Executor}。
	 */
	public final Executor getConcurrentExecutor() {
		return this.concurrentExecutor;
	}

	/**
		 * 指定要应用于即将执行的任何 {@link Runnable} 的自定义 {@link TaskDecorator}。
		 * <p>请注意，此类装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，
		 * 而是应用于实际的执行回调（可能是用户提供的任务的包装器）。
		 * <p>主要用例是在任务调用前后设置一些执行上下文，或为任务执行提供一些监控/统计信息。
		 * <p><b>注意：</b> {@code TaskDecorator} 实现中的异常处理仅限于通过
		 * {@code execute} 调用的普通 {@code Runnable} 执行。
		 * 在 {@code #submit} 调用的情况下，暴露的 {@code Runnable} 将是
		 * 不会传播任何异常的 {@code FutureTask}；您可能需要对其进行强制转换
		 * 并调用 {@code Future#get} 来评估异常。
	 * @since 4.3
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.adaptedExecutor.setTaskDecorator(taskDecorator);
	}


	@Override
	public void execute(Runnable task) {
		this.adaptedExecutor.execute(task);
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		this.adaptedExecutor.execute(task, startTimeout);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		return this.adaptedExecutor.submitListenable(task);
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		return this.adaptedExecutor.submitListenable(task);
	}


	private static TaskExecutorAdapter getAdaptedExecutor(Executor concurrentExecutor) {
		if (managedExecutorServiceClass != null && managedExecutorServiceClass.isInstance(concurrentExecutor)) {
			return new ManagedTaskExecutorAdapter(concurrentExecutor);
		}
		return new TaskExecutorAdapter(concurrentExecutor);
	}


	/**
		 * TaskExecutorAdapter 子类，将所有提供的 Runnable 和 Callable
		 * 包装为 JSR-236 ManagedTask，基于 {@link SchedulingAwareRunnable}
		 * 暴露长时间运行提示，并基于任务的 {@code toString()} 表示提供标识名称。
	 */
	private static class ManagedTaskExecutorAdapter extends TaskExecutorAdapter {

		public ManagedTaskExecutorAdapter(Executor concurrentExecutor) {
			super(concurrentExecutor);
		}

		@Override
		public void execute(Runnable task) {
			super.execute(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public ListenableFuture<?> submitListenable(Runnable task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}
	}


	/**
		 * 委托类，将给定的 Runnable/Callable 包装为 JSR-236 ManagedTask，
		 * 基于 {@link SchedulingAwareRunnable} 暴露长时间运行提示，
		 * 并提供给定的标识名称。
	 */
	protected static class ManagedTaskBuilder {

		public static Runnable buildManagedTask(Runnable task, String identityName) {
			Map<String, String> properties;
			if (task instanceof SchedulingAwareRunnable) {
				properties = new HashMap<>(4);
				properties.put(ManagedTask.LONGRUNNING_HINT,
						Boolean.toString(((SchedulingAwareRunnable) task).isLongLived()));
			}
			else {
				properties = new HashMap<>(2);
			}
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}

		public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
			Map<String, String> properties = new HashMap<>(2);
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}
	}

}
