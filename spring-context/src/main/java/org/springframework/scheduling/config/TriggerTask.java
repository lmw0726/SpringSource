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

import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * 根据给定的 {@link Trigger} 执行 {@code Runnable} 的 {@link Task} 实现。
 *
 * @author Chris Beams
 * @since 3.2
 * @see ScheduledTaskRegistrar#addTriggerTask(TriggerTask)
 * @see org.springframework.scheduling.TaskScheduler#schedule(Runnable, Trigger)
 */
public class TriggerTask extends Task {

	private final Trigger trigger;


	/**
	 * 创建一个新的 {@link TriggerTask}。
	 * @param runnable 要执行的底层任务
	 * @param trigger 指定任务应该何时执行
	 */
	public TriggerTask(Runnable runnable, Trigger trigger) {
		super(runnable);
		Assert.notNull(trigger, "Trigger must not be null");
		this.trigger = trigger;
	}


	/**
	 * 返回关联的触发器。
	 */
	public Trigger getTrigger() {
		return this.trigger;
	}

}
