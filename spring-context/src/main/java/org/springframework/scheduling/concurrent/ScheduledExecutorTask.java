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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 描述定时执行器任务的 JavaBean，由 {@link Runnable} 和延迟时间加上周期组成。
 * 周期必须指定；没有合适的默认值。
 *
 * <p>{@link java.util.concurrent.ScheduledExecutorService} 不提供更复杂的调度选项，例如 cron 表达式。
 * 对于此类需求，请考虑使用 {@link ThreadPoolTaskScheduler}。
 *
 * <p>请注意，{@link java.util.concurrent.ScheduledExecutorService} 机制使用一个在重复执行之间共享的 {@link Runnable} 实例，
 * 而 Quartz 则为每次执行创建一个新的 Job 实例。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 */
public class ScheduledExecutorTask {

	@Nullable
	private Runnable runnable;

	private long delay = 0;

	private long period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;


	/**
	 * 创建一个新的 ScheduledExecutorTask，
	 * 通过 bean 属性进行填充。
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledExecutorTask() {
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask，默认为
	 * 不带延迟的一次性执行。
	 * @param executorTask 要调度的 Runnable
	 */
	public ScheduledExecutorTask(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask，默认为
	 * 带有给定延迟的一次性执行。
	 * @param executorTask 要调度的 Runnable
	 * @param delay 第一次启动任务前的延迟时间（毫秒）
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay) {
		this.runnable = executorTask;
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask。
	 * @param executorTask 要调度的 Runnable
	 * @param delay 第一次启动任务前的延迟时间（毫秒）
	 * @param period 重复任务执行之间的周期（毫秒）
	 * @param fixedRate 是否按固定速率执行
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
		this.runnable = executorTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 设置要作为执行器任务调度的 Runnable。
	 */
	public void setRunnable(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 返回要作为执行器任务调度的 Runnable。
	 */
	public Runnable getRunnable() {
		Assert.state(this.runnable != null, "No Runnable set");
		return this.runnable;
	}

	/**
	 * 设置第一次启动任务前的延迟时间，单位为毫秒。
	 * 默认值为 0，即在成功调度后立即启动任务。
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回第一次启动任务前的延迟时间。
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置重复任务执行之间的周期，单位为毫秒。
	 * <p>默认值为 -1，即只执行一次。如果值为正数，
	 * 任务将重复执行，每次执行之间间隔指定的时间。
	 * <p>请注意，周期值的语义在固定速率执行和
	 * 固定延迟执行之间有所不同。
	 * <p><b>注意：</b>不支持 0 周期（例如作为固定延迟），
	 * 因为 {@code java.util.concurrent.ScheduledExecutorService} 本身
	 * 不支持此值。因此，值为 0 将被视为一次性执行；
	 * 但是，这个值首先不应该被显式指定！
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复任务执行之间的周期。
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * 此任务是否只会执行一次？
	 * @return 如果此任务只会执行一次则返回 {@code true}
	 * @see #getPeriod()
	 */
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * 指定延迟和周期值的时间单位。
	 * 默认为毫秒 ({@code TimeUnit.MILLISECONDS})。
	 * @see java.util.concurrent.TimeUnit#MILLISECONDS
	 * @see java.util.concurrent.TimeUnit#SECONDS
	 */
	public void setTimeUnit(@Nullable TimeUnit timeUnit) {
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
	}

	/**
	 * 返回延迟和周期值的时间单位。
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * 设置是否按固定速率执行，而非固定延迟执行。默认为 "false"，即固定延迟。
	 * <p>有关这些执行模式的详细信息，请参见 ScheduledExecutorService 的 javadoc。
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否按固定速率执行。
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
