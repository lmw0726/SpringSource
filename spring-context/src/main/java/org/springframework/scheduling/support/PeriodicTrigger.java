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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * 用于周期性任务执行的触发器。周期可以按固定速率或固定延迟应用，
 * 也可以配置初始延迟值。默认初始延迟为 0，默认行为是固定延迟
 * （即每次执行之间的时间间隔是从每次<i>完成</i>时间开始计算的）。
 * 要改为按每次执行的计划<i>开始</i>时间来计算间隔，
 * 请将 'fixedRate' 属性设置为 {@code true}。
 *
 * <p>请注意，TaskScheduler 接口已经定义了按固定速率或固定延迟调度
 * 任务的方法。这些方法也都支持可选的初始延迟值。只要有可能，
 * 应该直接使用那些方法。这个 Trigger 实现的价值在于它可以用于
 * 依赖 Trigger 抽象的组件中。例如，允许周期触发器、基于 cron 的
 * 触发器，甚至自定义 Trigger 实现可以互换使用，这可能很方便。
 *
 * @author Mark Fisher
 * @since 3.0
 */
public class PeriodicTrigger implements Trigger {

	private final long period;

	private final TimeUnit timeUnit;

	private volatile long initialDelay;

	private volatile boolean fixedRate;


	/**
	 * 使用给定的毫秒周期创建触发器。
	 */
	public PeriodicTrigger(long period) {
		this(period, null);
	}

	/**
	 * 使用给定的周期和时间单位创建触发器。该时间单位不仅适用于周期，
	 * 也适用于后续通过 {@link #setInitialDelay(long)} 在此 Trigger 上
	 * 配置的任何 'initialDelay' 值。
	 */
	public PeriodicTrigger(long period, @Nullable TimeUnit timeUnit) {
		Assert.isTrue(period >= 0, "period must not be negative");
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
		this.period = this.timeUnit.toMillis(period);
	}


	/**
	 * 返回此触发器的周期。
	 * @since 5.0.2
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * 返回此触发器的时间单位（默认为毫秒）。
	 * @since 5.0.2
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * 指定初始执行的延迟时间。它将根据此触发器的 {@link TimeUnit} 进行评估。
	 * 如果实例化时未明确提供时间单位，则默认为毫秒。
	 */
	public void setInitialDelay(long initialDelay) {
		this.initialDelay = this.timeUnit.toMillis(initialDelay);
	}

	/**
	 * 返回初始延迟，如果没有则返回 0。
	 * @since 5.0.2
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

	/**
	 * 指定周期性间隔是否应在计划开始时间之间测量，而不是在实际完成时间之间测量。
	 * 后者，即"固定延迟"行为，是默认行为。
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回此触发器是否使用固定速率（{@code true}）或固定延迟（{@code false}）行为。
	 * @since 5.0.2
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}


	/**
	 * 返回任务应再次运行的时间。
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date lastExecution = triggerContext.lastScheduledExecutionTime();
		Date lastCompletion = triggerContext.lastCompletionTime();
		if (lastExecution == null || lastCompletion == null) {
			return new Date(triggerContext.getClock().millis() + this.initialDelay);
		}
		if (this.fixedRate) {
			return new Date(lastExecution.getTime() + this.period);
		}
		return new Date(lastCompletion.getTime() + this.period);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PeriodicTrigger)) {
			return false;
		}
		PeriodicTrigger otherTrigger = (PeriodicTrigger) other;
		return (this.fixedRate == otherTrigger.fixedRate && this.initialDelay == otherTrigger.initialDelay &&
				this.period == otherTrigger.period);
	}

	@Override
	public int hashCode() {
		return (this.fixedRate ? 17 : 29) + (int) (37 * this.period) + (int) (41 * this.initialDelay);
	}

}
