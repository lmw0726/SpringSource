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

package org.springframework.core.task;

import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * {@link TaskExecutor} 实现，将每个任务 <i>同步地</i> 在调用线程中执行。
 *
 * <p>主要用于测试场景。
 *
 * <p>在调用线程中执行的优点是能够参与该线程的上下文，
 * 例如线程上下文类加载器或线程当前的事务关联。
 * 但在许多情况下，更推荐使用异步执行：对于这类场景，应选择异步的 {@code TaskExecutor}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAsyncTaskExecutor
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor implements TaskExecutor, Serializable {

	/**
	 * 同步执行给定的 {@code task}，通过直接调用其 {@link Runnable#run()} 方法实现。
	 * @throws IllegalArgumentException 如果给定的 {@code task} 为 {@code null}
	 */
	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Runnable must not be null");
		task.run();
	}

}
