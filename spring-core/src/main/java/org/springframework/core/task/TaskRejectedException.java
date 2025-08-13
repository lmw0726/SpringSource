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

import java.util.concurrent.RejectedExecutionException;

/**
 * 当 {@link TaskExecutor} 拒绝接受某个任务执行时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see TaskExecutor#execute(Runnable)
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

	/**
	 * 使用指定的详细消息创建一个新的 {@code TaskRejectedException}，
	 * 无根因。
	 * @param msg 详细消息
	 */
	public TaskRejectedException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和给定的根因创建一个新的 {@code TaskRejectedException}。
	 * @param msg 详细消息
	 * @param cause 根因（通常来自于使用诸如 {@code java.util.concurrent} 包的底层 API）
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskRejectedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
