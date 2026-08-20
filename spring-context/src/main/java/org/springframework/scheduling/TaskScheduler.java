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

package org.springframework.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * 任务调度器接口，抽象了基于不同类型的触发器来调度 {@link Runnable Runnables} 的方式。
 *
 * <p>此接口与 {@link SchedulingTaskExecutor} 分离，因为它通常代表不同类型的后端，
 * 即具有不同特性和能力的线程池。如果实现能够处理两种执行特性，则可以同时实现这两个接口。
 *
 * <p>"默认"实现是 {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler}，
 * 它包装了原生的 {@link java.util.concurrent.ScheduledExecutorService} 并添加了扩展的触发器能力。
 *
 * <p>此接口大致等同于 JSR-236 {@code ManagedScheduledExecutorService}，
 * 该服务在 Java EE 7 环境中受支持，但与 Spring 的 {@code TaskExecutor} 模型对齐。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ScheduledExecutorService
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
 */
public interface TaskScheduler {

	/**
	 * 返回用于调度目的的时钟。
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	default Clock getClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * 调度给定的 {@link Runnable}，在触发器指示下一次执行时间时调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param trigger {@link Trigger} 接口的实现，
	 * 例如包装 cron 表达式的 {@link org.springframework.scheduling.support.CronTrigger} 对象
	 * @return 表示任务待完成的 {@link ScheduledFuture}，
	 * 如果给定的 Trigger 对象从不触发（即从 {@link Trigger#nextExecutionTime} 返回 {@code null}）则返回 {@code null}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @see org.springframework.scheduling.support.CronTrigger
	 */
	@Nullable
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @since 5.0
	 * @see #schedule(Runnable, Date)
	 */
	default ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return schedule(task, Date.from(startTime));
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 */
	ScheduledFuture<?> schedule(Runnable task, Date startTime);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，然后按照给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望首次执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @param period 任务连续执行之间的时间间隔
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, Date, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return scheduleAtFixedRate(task, Date.from(startTime), period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，然后按照给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望首次执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @param period 任务连续执行之间的时间间隔（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

	/**
	 * 调度给定的 {@link Runnable}，尽快开始并按照给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param period 任务连续执行之间的时间间隔
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return scheduleAtFixedRate(task, period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快开始并按照给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param period 任务连续执行之间的时间间隔（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，然后在一次执行完成与下一次执行开始之间按照给定的延迟调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望首次执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @param delay 一次执行完成与下一次执行开始之间的延迟
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, Date, long)
	 */
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return scheduleWithFixedDelay(task, Date.from(startTime), delay.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，然后在一次执行完成与下一次执行开始之间按照给定的延迟调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务的期望首次执行时间
	 * （如果这是过去时间，任务将立即执行，即尽快执行）
	 * @param delay 一次执行完成与下一次执行开始之间的延迟（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

	/**
	 * 调度给定的 {@link Runnable}，尽快开始并在一次执行完成与下一次执行开始之间按照给定的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param delay 一次执行完成与下一次执行开始之间的延迟
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return scheduleWithFixedDelay(task, delay.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快开始并在一次执行完成与下一次执行开始之间按照给定的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param delay 一次执行完成与下一次执行开始之间的延迟（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因未被接受
	 * （例如池过载处理策略或池正在关闭）
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
