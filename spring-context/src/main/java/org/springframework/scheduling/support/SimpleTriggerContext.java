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

package org.springframework.scheduling.support;

import java.time.Clock;
import java.util.Date;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TriggerContext;

/**
 * {@link TriggerContext} 接口的简单数据持有者实现。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SimpleTriggerContext implements TriggerContext {

	private final Clock clock;

	@Nullable
	private volatile Date lastScheduledExecutionTime;

	@Nullable
	private volatile Date lastActualExecutionTime;

	@Nullable
	private volatile Date lastCompletionTime;


	/**
	 * 创建一个所有时间值均设为 {@code null} 的 SimpleTriggerContext，
	 * 并使用默认时区的系统时钟。
	 */
	public SimpleTriggerContext() {
		this.clock = Clock.systemDefaultZone();
	}

	/**
	 * 使用给定的时间值创建 SimpleTriggerContext，
	 * 并使用默认时区的系统时钟。
	 * @param lastScheduledExecutionTime 上一次 <i>计划</i> 执行时间
	 * @param lastActualExecutionTime 上一次 <i>实际</i> 执行时间
	 * @param lastCompletionTime 上一次完成时间
	 */
	public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this();
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}

	/**
	 * 创建一个所有时间值均设为 {@code null} 的 SimpleTriggerContext，
	 * 并使用给定的时钟。
	 * @param clock 用于触发器计算的时钟
	 * @since 5.3
	 * @see #update(Date, Date, Date)
	 */
	public SimpleTriggerContext(Clock clock) {
		this.clock = clock;
	}


	/**
	 * 使用最新的时间值更新此持有者的状态。
	 * @param lastScheduledExecutionTime 上一次 <i>计划</i> 执行时间
	 * @param lastActualExecutionTime 上一次 <i>实际</i> 执行时间
	 * @param lastCompletionTime 上一次完成时间
	 */
	public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}


	@Override
	public Clock getClock() {
		return this.clock;
	}

	@Override
	@Nullable
	public Date lastScheduledExecutionTime() {
		return this.lastScheduledExecutionTime;
	}

	@Override
	@Nullable
	public Date lastActualExecutionTime() {
		return this.lastActualExecutionTime;
	}

	@Override
	@Nullable
	public Date lastCompletionTime() {
		return this.lastCompletionTime;
	}

}
