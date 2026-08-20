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

package org.springframework.scheduling.config;

import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * 定时任务在运行时的表示，
 * 用作调度方法的返回值。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see ScheduledTaskRegistrar#scheduleCronTask(CronTask)
 * @see ScheduledTaskRegistrar#scheduleFixedRateTask(FixedRateTask)
 * @see ScheduledTaskRegistrar#scheduleFixedDelayTask(FixedDelayTask)
 * @see ScheduledFuture
 */
public final class ScheduledTask {

	private final Task task;

	@Nullable
	volatile ScheduledFuture<?> future;


	ScheduledTask(Task task) {
		this.task = task;
	}


	/**
	 * 返回底层任务（通常是 {@link CronTask}、
	 * {@link FixedRateTask} 或 {@link FixedDelayTask}）。
	 * @since 5.0.2
	 */
	public Task getTask() {
		return this.task;
	}

	/**
	 * 触发取消此定时任务。
	 * <p>此重载方法会在任务仍在运行时强制中断任务。
	 * @see #cancel(boolean)
	 */
	public void cancel() {
		cancel(true);
	}

	/**
	 * 触发取消此定时任务。
	 * @param mayInterruptIfRunning 是否在任务仍在运行时强制中断任务
	 * （指定 {@code false} 以允许任务完成）
	 * @since 5.3.18
	 * @see ScheduledFuture#cancel(boolean)
	 */
	public void cancel(boolean mayInterruptIfRunning) {
		ScheduledFuture<?> future = this.future;
		if (future != null) {
			future.cancel(mayInterruptIfRunning);
		}
	}

	@Override
	public String toString() {
		return this.task.toString();
	}

}
