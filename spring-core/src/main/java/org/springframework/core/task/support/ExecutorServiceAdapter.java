/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 适配器，将 Spring 的 {@link org.springframework.core.task.TaskExecutor} 包装成完整的 {@code java.util.concurrent.ExecutorService}。
 *
 * <p>主要用于适配那些基于 {@code java.util.concurrent.ExecutorService} API 进行通信的客户端组件。
 * 也可用作本地 Spring {@code TaskExecutor} 后端与 Java EE 7 环境中通过 JNDI 查找的 {@code ManagedExecutorService} 之间的桥梁。
 *
 * <p><b>注意：</b>此 ExecutorService 适配器不支持 {@code java.util.concurrent.ExecutorService} API 中的生命周期方法（如 shutdown() 等），
 * 这类似于 Java EE 7 环境中的服务器范围 {@code ManagedExecutorService}。
 * 生命周期由后端线程池管理，本适配器仅作为该线程池的访问代理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 */
public class ExecutorServiceAdapter extends AbstractExecutorService {

	private final TaskExecutor taskExecutor;


	/**
	 * 使用指定的 TaskExecutor 创建新的 ExecutorServiceAdapter。
	 * @param taskExecutor 目标 TaskExecutor，不能为空
	 */
	public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable task) {
		this.taskExecutor.execute(task);
	}

	@Override
	public void shutdown() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

}
