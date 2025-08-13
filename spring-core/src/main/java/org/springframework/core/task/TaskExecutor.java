/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.concurrent.Executor;

/**
 * 简单的任务执行器接口，抽象了 {@link Runnable} 的执行。
 *
 * <p>实现类可以使用各种不同的执行策略，
 * 例如：同步执行、异步执行、使用线程池等。
 *
 * <p>等同于 JDK 1.5 的 {@link java.util.concurrent.Executor} 接口；
 * 在 Spring 3.0 中扩展该接口，使客户端可以声明依赖 Executor 并接收任何 TaskExecutor 实现。
 * 该接口与标准的 Executor 接口保持分离，
 * 主要是为了兼容 Spring 2.x 中对 JDK 1.4 的支持。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.Executor
 */
@FunctionalInterface
public interface TaskExecutor extends Executor {

	/**
	 * 执行给定的 {@code task}。
	 * <p>如果实现采用异步执行策略，则调用可能立即返回；
	 * 如果是同步执行，则可能阻塞直到执行完成。
	 * @param task 要执行的 {@code Runnable}（不能为空）
	 * @throws TaskRejectedException 如果任务未被接受执行
	 */
	@Override
	void execute(Runnable task);

}
