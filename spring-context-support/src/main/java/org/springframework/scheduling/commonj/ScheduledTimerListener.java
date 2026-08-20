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

package org.springframework.scheduling.commonj;

import commonj.timers.TimerListener;

import org.springframework.lang.Nullable;

/**
 * 描述一个定时调度的 TimerListener 的 JavaBean，包括
 * TimerListener 本身（或一个用于创建 TimerListener 的 Runnable）
 * 以及延迟时间和执行周期。执行周期必须指定，
 * 因为它没有有意义的默认值。
 *
 * <p>CommonJ TimerManager 不提供更复杂的调度选项，
 * 例如 cron 表达式。如需此类高级功能，请考虑使用 Quartz。
 *
 * <p>请注意，TimerManager 在重复执行时使用同一个 TimerListener 实例，
 * 而 Quartz 则为每次执行实例化一个新的 Job。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated 从 5.1 版本起已弃用，请改用 EE 7 的
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public class ScheduledTimerListener {


	@Nullable
	private TimerListener timerListener;

	private long delay = 0;

	private long period = -1;

	private boolean fixedRate = false;


	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 通过 bean 属性进行配置。
	 * @see #setTimerListener
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledTimerListener() {
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 默认仅执行一次且不延迟。
	 * @param timerListener 要调度的 TimerListener
	 */
	public ScheduledTimerListener(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 默认仅执行一次，带指定的延迟时间。
	 * @param timerListener 要调度的 TimerListener
	 * @param delay 首次执行任务前的延迟时间（毫秒）
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay) {
		this.timerListener = timerListener;
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener。
	 * @param timerListener 要调度的 TimerListener
	 * @param delay 首次执行任务前的延迟时间（毫秒）
	 * @param period 重复执行之间的周期（毫秒）
	 * @param fixedRate 是否以固定速率方式调度执行
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay, long period, boolean fixedRate) {
		this.timerListener = timerListener;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 默认仅执行一次且不延迟。
	 * @param timerTask 要作为 TimerListener 调度的 Runnable
	 */
	public ScheduledTimerListener(Runnable timerTask) {
		setRunnable(timerTask);
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 默认仅执行一次，带指定的延迟时间。
	 * @param timerTask 要作为 TimerListener 调度的 Runnable
	 * @param delay 首次执行任务前的延迟时间（毫秒）
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay) {
		setRunnable(timerTask);
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener。
	 * @param timerTask 要作为 TimerListener 调度的 Runnable
	 * @param delay 首次执行任务前的延迟时间（毫秒）
	 * @param period 重复执行之间的周期（毫秒）
	 * @param fixedRate 是否以固定速率方式调度执行
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay, long period, boolean fixedRate) {
		setRunnable(timerTask);
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 设置要作为 TimerListener 调度的 Runnable。
	 * @see DelegatingTimerListener
	 */
	public void setRunnable(Runnable timerTask) {
		this.timerListener = new DelegatingTimerListener(timerTask);
	}

	/**
	 * 设置要调度的 TimerListener。
	 */
	public void setTimerListener(@Nullable TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 返回要调度的 TimerListener。
	 */
	@Nullable
	public TimerListener getTimerListener() {
		return this.timerListener;
	}

	/**
	 * 设置首次执行任务前的延迟时间，单位为毫秒。
	 * 默认值为 0，即调度成功后立即执行任务。
	 * <p>如果指定了 "firstTime" 属性，则此属性将被忽略。
	 * 请指定其中一个属性，不要同时指定两个。
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回首次执行任务前的延迟时间。
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置重复执行之间的周期，单位为毫秒。
	 * <p>默认值为 -1，表示仅执行一次。如果值为零或正数，
	 * 任务将以给定的间隔重复执行。
	 * <p>请注意，周期值的语义在固定速率执行和固定延迟执行之间有所不同。
	 * <p><b>注意：</b>周期值为 0（例如作为固定延迟）<i>是</i>
	 * 受支持的，因为 CommonJ 规范将其定义为合法值。
	 * 因此，值为 0 将导致任务完成后立即重新执行
	 * （不同于 {@code java.util.Timer} 的仅执行一次行为）。
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复执行之间的周期。
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
		return (this.period < 0);
	}

	/**
	 * 设置是否以固定速率方式调度执行，而非固定延迟执行。
	 * 默认值为 "false"，即固定延迟。
	 * <p>有关这些执行模式的详细信息，请参阅 TimerManager 的 javadoc。
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否以固定速率方式调度执行。
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
