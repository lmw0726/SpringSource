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

package org.springframework.scheduling.config;

import org.springframework.scheduling.support.CronTrigger;

/**
 * {@link TriggerTask} 的实现，定义一个 {@code Runnable}，根据
 * {@linkplain org.springframework.scheduling.support.CronExpression#parse(String)
 * 标准 cron 表达式} 执行。
 *
 * @author Chris Beams
 * @since 3.2
 * @see org.springframework.scheduling.annotation.Scheduled#cron()
 * @see ScheduledTaskRegistrar#addCronTask(CronTask)
 */
public class CronTask extends TriggerTask {

	private final String expression;


	/**
	 * 创建一个新的 {@code CronTask}。
	 * @param runnable 要执行的底层任务
	 * @param expression 定义任务执行时间的 cron 表达式
	 */
	public CronTask(Runnable runnable, String expression) {
		this(runnable, new CronTrigger(expression));
	}

	/**
	 * 创建一个新的 {@code CronTask}。
	 * @param runnable 要执行的底层任务
	 * @param cronTrigger 定义任务执行时间的 cron 触发器
	 */
	public CronTask(Runnable runnable, CronTrigger cronTrigger) {
		super(runnable, cronTrigger);
		this.expression = cronTrigger.getExpression();
	}


	/**
	 * 返回定义任务执行时间的 cron 表达式。
	 */
	public String getExpression() {
		return this.expression;
	}

}
