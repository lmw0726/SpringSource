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

package org.springframework.scheduling;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * {@link org.springframework.core.task.TaskExecutor} 的扩展接口，
 * 向潜在的任务提交者暴露与调度相关的特性。
 *
 * <p>建议调度客户端提交的 {@link Runnable Runnables} 匹配所使用的
 * {@code TaskExecutor} 实现所公开的偏好设置。
 *
 * <p>注意：建议 {@link SchedulingTaskExecutor} 的实现类同时实现
 * {@link org.springframework.core.task.AsyncListenableTaskExecutor} 接口。
 * 这并非强制要求，因为它依赖于 Spring 4.0 新引入的
 * {@link org.springframework.util.concurrent.ListenableFuture} 接口，
 * 这会使第三方执行器实现无法同时兼容 Spring 4.0 和 Spring 3.x。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulingAwareRunnable
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

	/**
	 * 此 {@code TaskExecutor} 是否偏好短期任务而非长期任务？
	 * <p>{@code SchedulingTaskExecutor} 的实现可以表明它是否偏好提交的任务
	 * 在单次任务执行中执行尽可能少的工作。例如，提交的任务可能会将一个重复循环
	 * 拆分为多个单独的子任务，在每个子任务之后提交后续任务（如果可行的话）。
	 * <p>这应被视为一个提示。当然，{@code TaskExecutor} 客户端可以自由地
	 * 忽略此标志，从而也忽略 {@code SchedulingTaskExecutor} 接口的整体约束。
	 * 然而，线程池通常会表明对短期任务的偏好，以允许更细粒度的调度。
	 * @return 如果此执行器偏好短期任务（默认值）则返回 {@code true}，
	 * 否则返回 {@code false}（此时作为常规 {@code TaskExecutor} 处理）
	 */
	default boolean prefersShortLivedTasks() {
		return true;
	}

}
