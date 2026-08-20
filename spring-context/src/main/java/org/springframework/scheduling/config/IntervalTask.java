/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * {@link Task} 的实现，定义了一个 {@code Runnable}，以给定的毫秒间隔执行，
 * 根据上下文可被视为固定速率或固定延迟。
 *
 * @author Chris Beams
 * @since 3.2
 * @see ScheduledTaskRegistrar#addFixedRateTask(IntervalTask)
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 */
public class IntervalTask extends Task {

	private final long interval;

	private final long initialDelay;


	/**
	 * 创建一个新的 {@code IntervalTask}。
	 * @param runnable 要执行的底层任务
	 * @param interval 任务执行的间隔（毫秒）
	 * @param initialDelay 任务首次执行前的初始延迟
	 */
	public IntervalTask(Runnable runnable, long interval, long initialDelay) {
		super(runnable);
		this.interval = interval;
		this.initialDelay = initialDelay;
	}

	/**
	 * 创建一个没有初始延迟的新 {@code IntervalTask}。
	 * @param runnable 要执行的底层任务
	 * @param interval 任务执行的间隔（毫秒）
	 */
	public IntervalTask(Runnable runnable, long interval) {
		this(runnable, interval, 0);
	}


	/**
	 * 返回任务执行的间隔（毫秒）。
	 */
	public long getInterval() {
		return this.interval;
	}

	/**
	 * 返回任务首次执行前的初始延迟。
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

}
