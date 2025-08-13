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

package org.springframework.core.task;

/**
 * 用于装饰即将执行的任何 {@link Runnable} 的回调接口。
 *
 * <p>注意，这样的装饰器不一定直接应用于用户提供的 {@code Runnable}/{@code Callable}，
 * 而是应用于实际的执行回调（可能是用户任务的包装）。
 *
 * <p>主要用途是在任务调用前设置一些执行上下文，或为任务执行提供监控/统计。
 *
 * <p><b>注意：</b> {@code TaskDecorator} 实现中的异常处理可能有限。
 * 特别是在基于 {@code Future} 的操作中，暴露的 {@code Runnable} 是一个包装器，
 * 不会传播其 {@code run} 方法中的任何异常。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see TaskExecutor#execute(Runnable)
 * @see SimpleAsyncTaskExecutor#setTaskDecorator
 * @see org.springframework.core.task.support.TaskExecutorAdapter#setTaskDecorator
 */
@FunctionalInterface
public interface TaskDecorator {

	/**
	 * 装饰给定的 {@code Runnable}，返回一个可能被包装过的
	 * {@code Runnable} 用于实际执行，内部委托给原始的 {@link Runnable#run()} 实现。
	 * @param runnable 原始的 {@code Runnable}
	 * @return 装饰后的 {@code Runnable}
	 */
	Runnable decorate(Runnable runnable);

}
