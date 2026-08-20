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

package org.springframework.scheduling.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 用于向 {@link TaskScheduler} 注册任务的辅助 bean，通常使用 cron 表达式。
 *
 * <p>从 Spring 3.1 开始，{@code ScheduledTaskRegistrar} 在与 {@link
 * org.springframework.scheduling.annotation.EnableAsync @EnableAsync} 注解及其
 * org.springframework.scheduling.annotation.SchedulingConfigurer
 * SchedulingConfigurer} 回调接口配合使用时具有更突出的面向用户的角色。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Tobias Montagna-Hay
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.scheduling.annotation.EnableAsync
 * @see org.springframework.scheduling.annotation.SchedulingConfigurer
 */
public class ScheduledTaskRegistrar implements ScheduledTaskHolder, InitializingBean, DisposableBean {


	/**
	 * 一个特殊的 cron 表达式值，表示已禁用的触发器：{@value}。
	 * <p>这主要用于通过 {@link #addCronTask(Runnable, String)} 使用时，
	 * 当提供的 {@code expression} 值从外部来源获取时——例如从
	 * {@link org.springframework.core.env.Environment Environment} 中的属性获取。
	 * @since 5.2
	 * @see org.springframework.scheduling.annotation.Scheduled#CRON_DISABLED
	 */
	public static final String CRON_DISABLED = "-";


	@Nullable
	private TaskScheduler taskScheduler;

	@Nullable
	private ScheduledExecutorService localExecutor;

	@Nullable
	private List<TriggerTask> triggerTasks;

	@Nullable
	private List<CronTask> cronTasks;

	@Nullable
	private List<IntervalTask> fixedRateTasks;

	@Nullable
	private List<IntervalTask> fixedDelayTasks;

	private final Map<Task, ScheduledTask> unresolvedTasks = new HashMap<>(16);

	private final Set<ScheduledTask> scheduledTasks = new LinkedHashSet<>(16);


	/**
	 * 设置用于注册计划任务的 {@link TaskScheduler}。
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	/**
	 * 设置用于注册计划任务的 {@link TaskScheduler}，或一个
	 * 将被包装为 {@code TaskScheduler} 的
	 * {@link java.util.concurrent.ScheduledExecutorService}。
	 */
	public void setScheduler(@Nullable Object scheduler) {
		if (scheduler == null) {
			this.taskScheduler = null;
		}
		else if (scheduler instanceof TaskScheduler) {
			this.taskScheduler = (TaskScheduler) scheduler;
		}
		else if (scheduler instanceof ScheduledExecutorService) {
			this.taskScheduler = new ConcurrentTaskScheduler(((ScheduledExecutorService) scheduler));
		}
		else {
			throw new IllegalArgumentException("Unsupported scheduler type: " + scheduler.getClass());
		}
	}

	/**
	 * 返回此注册器的 {@link TaskScheduler} 实例（可能为 {@code null}）。
	 */
	@Nullable
	public TaskScheduler getScheduler() {
		return this.taskScheduler;
	}


	/**
	 * 以 Runnable（任务）和 Trigger 对象（通常是 {@link Trigger} 接口的自定义实现）的 Map 形式指定触发任务。
	 */
	public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
		this.triggerTasks = new ArrayList<>();
		triggerTasks.forEach((task, trigger) -> addTriggerTask(new TriggerTask(task, trigger)));
	}

	/**
	 * 以 {@link TriggerTask} 对象列表的形式指定触发任务。主要由
	 * {@code <task:*>} 命名空间解析使用。
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setTriggerTasksList(List<TriggerTask> triggerTasks) {
		this.triggerTasks = triggerTasks;
	}

	/**
	 * 以 {@link TriggerTask} 对象的不可修改列表形式获取触发任务。
	 * @return 任务列表（永不为 {@code null}）
	 * @since 4.2
	 */
	public List<TriggerTask> getTriggerTaskList() {
		return (this.triggerTasks != null? Collections.unmodifiableList(this.triggerTasks) :
				Collections.emptyList());
	}

	/**
	 * 以 Runnable（任务）和 cron 表达式的 Map 形式指定触发任务。
	 * @see CronTrigger
	 */
	public void setCronTasks(Map<Runnable, String> cronTasks) {
		this.cronTasks = new ArrayList<>();
		cronTasks.forEach(this::addCronTask);
	}

	/**
	 * 以 {@link CronTask} 对象列表的形式指定触发任务。主要由
	 * {@code <task:*>} 命名空间解析使用。
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setCronTasksList(List<CronTask> cronTasks) {
		this.cronTasks = cronTasks;
	}

	/**
	 * 以 {@link CronTask} 对象的不可修改列表形式获取 cron 任务。
	 * @return 任务列表（永不为 {@code null}）
	 * @since 4.2
	 */
	public List<CronTask> getCronTaskList() {
		return (this.cronTasks != null ? Collections.unmodifiableList(this.cronTasks) :
				Collections.emptyList());
	}

	/**
	 * 以 Runnable（任务）和固定频率值的 Map 形式指定触发任务。
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = new ArrayList<>();
		fixedRateTasks.forEach(this::addFixedRateTask);
	}

	/**
	 * 以 {@link IntervalTask} 对象列表的形式指定固定频率任务。主要由
	 * {@code <task:*>} 命名空间解析使用。
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setFixedRateTasksList(List<IntervalTask> fixedRateTasks) {
		this.fixedRateTasks = fixedRateTasks;
	}

	/**
	 * 以 {@link IntervalTask} 对象的不可修改列表形式获取固定频率任务。
	 * @return 任务列表（永不为 {@code null}）
	 * @since 4.2
	 */
	public List<IntervalTask> getFixedRateTaskList() {
		return (this.fixedRateTasks != null ? Collections.unmodifiableList(this.fixedRateTasks) :
				Collections.emptyList());
	}

	/**
	 * 以 Runnable（任务）和固定延迟值的 Map 形式指定触发任务。
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = new ArrayList<>();
		fixedDelayTasks.forEach(this::addFixedDelayTask);
	}

	/**
	 * 以 {@link IntervalTask} 对象列表的形式指定固定延迟任务。主要由
	 * {@code <task:*>} 命名空间解析使用。
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setFixedDelayTasksList(List<IntervalTask> fixedDelayTasks) {
		this.fixedDelayTasks = fixedDelayTasks;
	}

	/**
	 * 以 {@link IntervalTask} 对象的不可修改列表形式获取固定延迟任务。
	 * @return 任务列表（永不为 {@code null}）
	 * @since 4.2
	 */
	public List<IntervalTask> getFixedDelayTaskList() {
		return (this.fixedDelayTasks != null ? Collections.unmodifiableList(this.fixedDelayTasks) :
				Collections.emptyList());
	}


	/**
	 * 添加一个 Runnable 任务，按给定的 {@link Trigger} 触发。
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addTriggerTask(Runnable task, Trigger trigger) {
		addTriggerTask(new TriggerTask(task, trigger));
	}

	/**
	 * 添加一个 {@code TriggerTask}。
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addTriggerTask(TriggerTask task) {
		if (this.triggerTasks == null) {
			this.triggerTasks = new ArrayList<>();
		}
		this.triggerTasks.add(task);
	}

	/**
	 * 添加一个 {@link Runnable} 任务，按给定的 cron {@code expression} 触发。
	 * <p>从 Spring Framework 5.2 开始，如果 {@code expression}
	 * 等于 {@link #CRON_DISABLED}，此方法将不会注册该任务。
	 */
	public void addCronTask(Runnable task, String expression) {
		if (!CRON_DISABLED.equals(expression)) {
			addCronTask(new CronTask(task, expression));
		}
	}

	/**
	 * 添加一个 {@link CronTask}。
	 * @since 3.2
	 */
	public void addCronTask(CronTask task) {
		if (this.cronTasks == null) {
			this.cronTasks = new ArrayList<>();
		}
		this.cronTasks.add(task);
	}

	/**
	 * 添加一个 {@code Runnable} 任务，按给定的固定频率间隔触发。
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addFixedRateTask(Runnable task, long interval) {
		addFixedRateTask(new IntervalTask(task, interval, 0));
	}

	/**
	 * 添加一个固定频率的 {@link IntervalTask}。
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addFixedRateTask(IntervalTask task) {
		if (this.fixedRateTasks == null) {
			this.fixedRateTasks = new ArrayList<>();
		}
		this.fixedRateTasks.add(task);
	}

	/**
	 * 添加一个 Runnable 任务，按给定的固定延迟触发。
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void addFixedDelayTask(Runnable task, long delay) {
		addFixedDelayTask(new IntervalTask(task, delay, 0));
	}

	/**
	 * 添加一个固定延迟的 {@link IntervalTask}。
	 * @since 3.2
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void addFixedDelayTask(IntervalTask task) {
		if (this.fixedDelayTasks == null) {
			this.fixedDelayTasks = new ArrayList<>();
		}
		this.fixedDelayTasks.add(task);
	}


	/**
	 * 返回此 {@code ScheduledTaskRegistrar} 是否已注册任何任务。
	 * @since 3.2
	 */
	public boolean hasTasks() {
		return (!CollectionUtils.isEmpty(this.triggerTasks) ||
				!CollectionUtils.isEmpty(this.cronTasks) ||
				!CollectionUtils.isEmpty(this.fixedRateTasks) ||
				!CollectionUtils.isEmpty(this.fixedDelayTasks));
	}


	/**
	 * 在 bean 构造时调用 {@link #scheduleTasks()}。
	 */
	@Override
	public void afterPropertiesSet() {
		scheduleTasks();
	}

	/**
	 * 针对底层的 {@linkplain #setTaskScheduler(TaskScheduler) task scheduler}
	 * 调度所有已注册的任务。
	 */
	@SuppressWarnings("deprecation")
	protected void scheduleTasks() {
		if (this.taskScheduler == null) {
			this.localExecutor = Executors.newSingleThreadScheduledExecutor();
			this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
		}
		if (this.triggerTasks != null) {
			for (TriggerTask task : this.triggerTasks) {
				addScheduledTask(scheduleTriggerTask(task));
			}
		}
		if (this.cronTasks != null) {
			for (CronTask task : this.cronTasks) {
				addScheduledTask(scheduleCronTask(task));
			}
		}
		if (this.fixedRateTasks != null) {
			for (IntervalTask task : this.fixedRateTasks) {
				addScheduledTask(scheduleFixedRateTask(task));
			}
		}
		if (this.fixedDelayTasks != null) {
			for (IntervalTask task : this.fixedDelayTasks) {
				addScheduledTask(scheduleFixedDelayTask(task));
			}
		}
	}

	private void addScheduledTask(@Nullable ScheduledTask task) {
		if (task != null) {
			this.scheduledTasks.add(task);
		}
	}


	/**
	 * 调度指定的触发任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * @since 4.3
	 */
	@Nullable
	public ScheduledTask scheduleTriggerTask(TriggerTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addTriggerTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 调度指定的 cron 任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * （如果正在处理之前注册的任务则返回 {@code null}）
	 * @since 4.3
	 */
	@Nullable
	public ScheduledTask scheduleCronTask(CronTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addCronTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 调度指定的固定频率任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * （如果正在处理之前注册的任务则返回 {@code null}）
	 * @since 4.3
	 * @deprecated 从 5.0.2 起已弃用，建议使用 {@link #scheduleFixedRateTask(FixedRateTask)}
	 */
	@Deprecated
	@Nullable
	public ScheduledTask scheduleFixedRateTask(IntervalTask task) {
		FixedRateTask taskToUse = (task instanceof FixedRateTask ? (FixedRateTask) task :
				new FixedRateTask(task.getRunnable(), task.getInterval(), task.getInitialDelay()));
		return scheduleFixedRateTask(taskToUse);
	}

	/**
	 * 调度指定的固定频率任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * （如果正在处理之前注册的任务则返回 {@code null}）
	 * @since 5.0.2
	 */
	@Nullable
	public ScheduledTask scheduleFixedRateTask(FixedRateTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(this.taskScheduler.getClock().millis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedRateTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 调度指定的固定延迟任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * （如果正在处理之前注册的任务则返回 {@code null}）
	 * @since 4.3
	 * @deprecated 从 5.0.2 起已弃用，建议使用 {@link #scheduleFixedDelayTask(FixedDelayTask)}
	 */
	@Deprecated
	@Nullable
	public ScheduledTask scheduleFixedDelayTask(IntervalTask task) {
		FixedDelayTask taskToUse = (task instanceof FixedDelayTask ? (FixedDelayTask) task :
				new FixedDelayTask(task.getRunnable(), task.getInterval(), task.getInitialDelay()));
		return scheduleFixedDelayTask(taskToUse);
	}

	/**
	 * 调度指定的固定延迟任务，如果可能则立即调度，否则在调度器初始化时调度。
	 * @return 计划任务的句柄，允许取消该任务
	 * （如果正在处理之前注册的任务则返回 {@code null}）
	 * @since 5.0.2
	 */
	@Nullable
	public ScheduledTask scheduleFixedDelayTask(FixedDelayTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(this.taskScheduler.getClock().millis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedDelayTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}


	/**
	 * 返回此注册器已调度的所有本地注册任务。
	 * @since 5.0.2
	 * @see #addTriggerTask
	 * @see #addCronTask
	 * @see #addFixedRateTask
	 * @see #addFixedDelayTask
	 */
	@Override
	public Set<ScheduledTask> getScheduledTasks() {
		return Collections.unmodifiableSet(this.scheduledTasks);
	}

	@Override
	public void destroy() {
		for (ScheduledTask task : this.scheduledTasks) {
			task.cancel();
		}
		if (this.localExecutor != null) {
			this.localExecutor.shutdownNow();
		}
	}

}
