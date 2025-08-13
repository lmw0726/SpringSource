/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 异步 {@link TaskExecutor} 的扩展接口，
 * 支持 {@link java.util.concurrent.Callable} 的执行。
 *
 * <p>注意：{@link java.util.concurrent.Executors} 类提供了一组方法，
 * 可以将一些常见的闭包式对象（例如 {@link java.security.PrivilegedAction}）转换成 {@link Callable} 后执行。
 *
 * <p>实现该接口的类表明 {@link #execute(Runnable)} 方法不会在调用线程执行任务，
 * 而是会在其他线程异步执行。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.Executors
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/**
	 * 立即执行的常量标识。
	 * @deprecated 自 5.3.16 版本起，连同 {@link #execute(Runnable, long)} 一起废弃
	 */
	@Deprecated
	long TIMEOUT_IMMEDIATE = 0;

	/**
	 * 无时间限制的常量标识。
	 * @deprecated 自 5.3.16 版本起，连同 {@link #execute(Runnable, long)} 一起废弃
	 */
	@Deprecated
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * 执行给定的任务。
	 * @param task 要执行的 {@code Runnable}（不允许为 {@code null}）
	 * @param startTimeout 任务预期开始执行的超时时间（毫秒）。
	 *                     这是给执行器的提示，优先处理“立即执行”的任务。
	 *                     典型值为 {@link #TIMEOUT_IMMEDIATE} 或 {@link #TIMEOUT_INDEFINITE}（默认，与 {@link #execute(Runnable)} 一致）。
	 * @throws TaskTimeoutException 任务因超时而被拒绝执行时抛出（即无法及时启动）
	 * @throws TaskRejectedException 任务未被接受时抛出
	 * @see #execute(Runnable)
	 * @deprecated 自 5.3.16 版本起废弃，因为大多数执行器不支持启动超时
	 */
	@Deprecated
	void execute(Runnable task, long startTimeout);

	/**
	 * 提交一个 Runnable 任务执行，返回一个表示任务的 Future。
	 * 该 Future 在任务完成时返回 {@code null} 结果。
	 * @param task 要执行的 {@code Runnable}（不允许为 {@code null}）
	 * @return 表示任务未完成的 Future
	 * @throws TaskRejectedException 任务未被接受时抛出
	 * @since 3.0
	 */
	Future<?> submit(Runnable task);

	/**
	 * 提交一个 Callable 任务执行，返回一个表示任务的 Future。
	 * 该 Future 在任务完成时返回 Callable 的执行结果。
	 * @param task 要执行的 {@code Callable}（不允许为 {@code null}）
	 * @return 表示任务未完成的 Future
	 * @throws TaskRejectedException 任务未被接受时抛出
	 * @since 3.0
	 */
	<T> Future<T> submit(Callable<T> task);

}
