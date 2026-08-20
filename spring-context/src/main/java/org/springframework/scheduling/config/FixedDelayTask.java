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
 * {@link IntervalTask} 的固定延迟语义特化。
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 * @see org.springframework.scheduling.annotation.Scheduled#fixedDelay()
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 */
public class FixedDelayTask extends IntervalTask {

	/**
	 * 创建一个新的 {@code FixedDelayTask}。
	 * @param runnable 要执行的底层任务
	 * @param interval 任务执行的间隔时间（毫秒）
	 * @param initialDelay 首次执行前的初始延迟时间（毫秒）
	 */
	public FixedDelayTask(Runnable runnable, long interval, long initialDelay) {
		super(runnable, interval, initialDelay);
	}

}
