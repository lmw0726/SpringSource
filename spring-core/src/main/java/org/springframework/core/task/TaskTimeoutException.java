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

/**
 * 当 {@link AsyncTaskExecutor} 因指定的超时时间拒绝接受某个任务执行时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see AsyncTaskExecutor#execute(Runnable, long)
 * @deprecated 自 5.3.16 版本起废弃，因为常用的执行器不支持启动超时。
 */
@Deprecated
@SuppressWarnings("serial")
public class TaskTimeoutException extends TaskRejectedException {

	/**
	 * 使用指定的详细消息创建一个新的 {@code TaskTimeoutException}，
	 * 无根因。
	 * @param msg 详细消息
	 */
	public TaskTimeoutException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和给定的根因创建一个新的 {@code TaskTimeoutException}。
	 * @param msg 详细消息
	 * @param cause 根因（通常来自于使用诸如 {@code java.util.concurrent} 包的底层 API）
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
