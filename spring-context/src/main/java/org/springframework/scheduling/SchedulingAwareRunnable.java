/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Runnable 接口的扩展，为长时间运行的操作添加了特殊回调。
 *
 * <p>此接口与 CommonJ Work 接口紧密对应，但保持独立以避免对 CommonJ 的强制依赖。
 *
 * <p>建议具有调度能力的 TaskExecutor 检查提交的 Runnable，
 * 检测是否实现了此接口，并根据自身能力做出适当反应。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see commonj.work.Work
 * @see org.springframework.core.task.TaskExecutor
 * @see SchedulingTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingAwareRunnable extends Runnable {

	/**
	 * 返回 Runnable 的操作是否为长时间运行（{@code true}）而非短时间运行（{@code false}）。
	 * <p>在前一种情况下，任务将不会从线程池（如果有的话）中分配线程，
	 * 而是被视为长时间运行的后台线程。
	 * <p>这应被视为一个提示。当然，TaskExecutor 的实现可以自由忽略此标志以及整个 SchedulingAwareRunnable 接口。
	 */
	boolean isLongLived();

}
