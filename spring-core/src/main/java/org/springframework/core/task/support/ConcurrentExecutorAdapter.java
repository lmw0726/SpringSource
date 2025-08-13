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

package org.springframework.core.task.support;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.Executor;

/**
 * 适配器，将任何 Spring 的 {@link org.springframework.core.task.TaskExecutor} 暴露为
 * {@link java.util.concurrent.Executor} 接口。
 *
 * <p>自 Spring 3.0 起，TaskExecutor 本身已继承 Executor 接口，
 * 该适配器主要用于 <em>隐藏</em> 给定对象的 TaskExecutor 特性，
 * 仅向客户端暴露标准的 Executor 接口。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.util.concurrent.Executor
 * @see org.springframework.core.task.TaskExecutor
 */
public class ConcurrentExecutorAdapter implements Executor {

	private final TaskExecutor taskExecutor;


	/**
	 * 为指定的 Spring TaskExecutor 创建新的 ConcurrentExecutorAdapter。
	 * @param taskExecutor 要包装的 Spring TaskExecutor，不能为空
	 */
	public ConcurrentExecutorAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable command) {
		this.taskExecutor.execute(command);
	}

}
