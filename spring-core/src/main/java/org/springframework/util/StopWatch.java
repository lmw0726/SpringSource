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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 简单秒表，允许对多个任务计时，提供总运行时间和每个命名任务的运行时间。
 *
 * <p>封装了 {@link System#nanoTime()} 的使用，提高了应用代码的可读性，
 * 并减少计算错误的可能性。
 *
 * <p>注意，此对象设计上不是线程安全的，也未使用同步机制。
 *
 * <p>此类通常用于概念验证和开发期间的性能验证，而不是生产环境应用的一部分。
 *
 * <p>从 Spring Framework 5.2 开始，运行时间以纳秒为单位进行跟踪和报告。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2001年5月2日
 */
public class StopWatch {

	/**
	 * 此 {@code StopWatch} 的标识符。
	 * <p>当有多个秒表输出并需要在日志或控制台输出中区分它们时非常有用。
	 */
	private final String id;

	private boolean keepTaskList = true;

	private final List<TaskInfo> taskList = new ArrayList<>(1);

	/** 当前任务的开始时间（纳秒）。 */
	private long startTimeNanos;

	/** 当前任务的名称。 */
	@Nullable
	private String currentTaskName;

	@Nullable
	private TaskInfo lastTaskInfo;

	private int taskCount;

	/** 总运行时间（纳秒）。 */
	private long totalTimeNanos;


	/**
	 * 构造一个新的 {@code StopWatch}。
	 * <p>不启动任何任务。
	 */
	public StopWatch() {
		this("");
	}

	/**
	 * 使用给定的ID构造一个新的 {@code StopWatch}。
	 * <p>当有多个秒表输出并需要区分时，ID非常有用。
	 * <p>不会启动任何任务。
	 * @param id 此秒表的标识符
	 */
	public StopWatch(String id) {
		this.id = id;
	}


	/**
	 * 获取此 {@code StopWatch} 的ID，即构造时指定的ID。
	 * @return ID（默认是空字符串）
	 * @since 4.2.2
	 * @see #StopWatch(String)
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 配置是否随着时间构建 {@link TaskInfo} 数组。
	 * <p>当使用 {@code StopWatch} 记录数百万次区间时，将此设置为 {@code false}；
	 * 否则，{@code TaskInfo} 结构会消耗过多内存。
	 * <p>默认值为 {@code true}。
	 */
	public void setKeepTaskList(boolean keepTaskList) {
		this.keepTaskList = keepTaskList;
	}


	/**
	 * 启动一个无名任务。
	 * <p>如果未先调用此方法而直接调用 {@link #stop()} 或计时方法，结果未定义。
	 * @see #start(String)
	 * @see #stop()
	 */
	public void start() throws IllegalStateException {
		start("");
	}

	/**
	 * 开始一个命名任务。
	 * <p>如果未先调用此方法而直接调用 {@link #stop()} 或计时方法，结果未定义。
	 * @param taskName 要启动的任务名称
	 * @see #start()
	 * @see #stop()
	 */
	public void start(String taskName) throws IllegalStateException {
		if (this.currentTaskName != null) {
			throw new IllegalStateException("Can't start StopWatch: it's already running");
		}
		this.currentTaskName = taskName;
		this.startTimeNanos = System.nanoTime();
	}

	/**
	 * 停止当前任务。
	 * <p>如果未调用至少一对 {@code start()} / {@code stop()} 方法，调用计时方法结果未定义。
	 * @see #start()
	 * @see #start(String)
	 */
	public void stop() throws IllegalStateException {
		if (this.currentTaskName == null) {
			throw new IllegalStateException("Can't stop StopWatch: it's not running");
		}
		long lastTime = System.nanoTime() - this.startTimeNanos;
		this.totalTimeNanos += lastTime;
		this.lastTaskInfo = new TaskInfo(this.currentTaskName, lastTime);
		if (this.keepTaskList) {
			this.taskList.add(this.lastTaskInfo);
		}
		++this.taskCount;
		this.currentTaskName = null;
	}

	/**
	 * 判断此 {@code StopWatch} 是否正在运行。
	 * @see #currentTaskName()
	 */
	public boolean isRunning() {
		return (this.currentTaskName != null);
	}

	/**
	 * 获取当前正在运行任务的名称（如果有）。
	 * @since 4.2.2
	 * @see #isRunning()
	 */
	@Nullable
	public String currentTaskName() {
		return this.currentTaskName;
	}

	/**
	 * 获取最后一个任务所用的纳秒时间。
	 * @since 5.2
	 * @see #getLastTaskTimeMillis()
	 */
	public long getLastTaskTimeNanos() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task interval");
		}
		return this.lastTaskInfo.getTimeNanos();
	}

	/**
	 * 获取最后一个任务所用的毫秒时间。
	 * @see #getLastTaskTimeNanos()
	 */
	public long getLastTaskTimeMillis() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task interval");
		}
		return this.lastTaskInfo.getTimeMillis();
	}

	/**
	 * 获取最后一个任务的名称。
	 */
	public String getLastTaskName() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task name");
		}
		return this.lastTaskInfo.getTaskName();
	}

	/**
	 * 获取最后一个任务的 {@link TaskInfo} 对象。
	 */
	public TaskInfo getLastTaskInfo() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task info");
		}
		return this.lastTaskInfo;
	}


	/**
	 * 获取所有任务的总纳秒时间。
	 * @since 5.2
	 * @see #getTotalTimeMillis()
	 * @see #getTotalTimeSeconds()
	 */
	public long getTotalTimeNanos() {
		return this.totalTimeNanos;
	}

	/**
	 * 获取所有任务的总毫秒时间。
	 * @see #getTotalTimeNanos()
	 * @see #getTotalTimeSeconds()
	 */
	public long getTotalTimeMillis() {
		return nanosToMillis(this.totalTimeNanos);
	}

	/**
	 * 获取所有任务的总秒数。
	 * @see #getTotalTimeNanos()
	 * @see #getTotalTimeMillis()
	 */
	public double getTotalTimeSeconds() {
		return nanosToSeconds(this.totalTimeNanos);
	}

	/**
	 * 获取计时的任务数量。
	 */
	public int getTaskCount() {
		return this.taskCount;
	}

	/**
	 * 获取执行任务的数据数组。
	 */
	public TaskInfo[] getTaskInfo() {
		if (!this.keepTaskList) {
			throw new UnsupportedOperationException("Task info is not being kept!");
		}
		return this.taskList.toArray(new TaskInfo[0]);
	}


	/**
	 * 获取总运行时间的简短描述。
	 */
	public String shortSummary() {
		return "StopWatch '" + getId() + "': running time = " + getTotalTimeNanos() + " ns";
	}

	/**
	 * 生成一个字符串表格，描述所有执行的任务。
	 * <p>如需自定义报告，请调用 {@link #getTaskInfo()} 并直接使用任务信息。
	 */
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder(shortSummary());
		sb.append('\n');
		if (!this.keepTaskList) {
			sb.append("No task info kept");
		}
		else {
			sb.append("---------------------------------------------\n");
			sb.append("ns         %     Task name\n");
			sb.append("---------------------------------------------\n");
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setMinimumIntegerDigits(9);
			nf.setGroupingUsed(false);
			NumberFormat pf = NumberFormat.getPercentInstance();
			pf.setMinimumIntegerDigits(3);
			pf.setGroupingUsed(false);
			for (TaskInfo task : getTaskInfo()) {
				sb.append(nf.format(task.getTimeNanos())).append("  ");
				sb.append(pf.format((double) task.getTimeNanos() / getTotalTimeNanos())).append("  ");
				sb.append(task.getTaskName()).append('\n');
			}
		}
		return sb.toString();
	}

	/**
	 * 生成一个描述所有执行任务的详细字符串
	 * <p>如需自定义报告，请调用 {@link #getTaskInfo()} 并直接使用任务信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(shortSummary());
		if (this.keepTaskList) {
			for (TaskInfo task : getTaskInfo()) {
				sb.append("; [").append(task.getTaskName()).append("] took ").append(task.getTimeNanos()).append(" ns");
				long percent = Math.round(100.0 * task.getTimeNanos() / getTotalTimeNanos());
				sb.append(" = ").append(percent).append('%');
			}
		}
		else {
			sb.append("; no task info kept");
		}
		return sb.toString();
	}


	private static long nanosToMillis(long duration) {
		return TimeUnit.NANOSECONDS.toMillis(duration);
	}

	private static double nanosToSeconds(long duration) {
		return duration / 1_000_000_000.0;
	}


	/**
	 * 嵌套类，用于保存 {@code StopWatch} 中执行的某个任务的数据。
	 */
	public static final class TaskInfo {

		private final String taskName;

		private final long timeNanos;

		TaskInfo(String taskName, long timeNanos) {
			this.taskName = taskName;
			this.timeNanos = timeNanos;
		}

		/**
		 * 获取该任务的名称。
		 */
		public String getTaskName() {
			return this.taskName;
		}

		/**
		 * 获取该任务耗时的纳秒数。
		 * @since 5.2
		 * @see #getTimeMillis()
		 * @see #getTimeSeconds()
		 */
		public long getTimeNanos() {
			return this.timeNanos;
		}

		/**
		 * 获取该任务耗时的毫秒数。
		 * @see #getTimeNanos()
		 * @see #getTimeSeconds()
		 */
		public long getTimeMillis() {
			return nanosToMillis(this.timeNanos);
		}

		/**
		 * 获取该任务耗时的秒数。
		 * @see #getTimeMillis()
		 * @see #getTimeNanos()
		 */
		public double getTimeSeconds() {
			return nanosToSeconds(this.timeNanos);
		}

	}

}
