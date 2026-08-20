/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.config;

import org.springframework.util.Assert;

/**
 * 定义 {@code Runnable} 作为任务执行的持有者类，通常在指定的时间或间隔执行。
 * 有关各种调度方式，请参阅子类层次结构。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public class Task {

	private final Runnable runnable;


	/**
	 * 创建一个新的 {@code Task}。
	 * @param runnable 要执行的底层任务
	 */
	public Task(Runnable runnable) {
		Assert.notNull(runnable, "Runnable must not be null");
		this.runnable = runnable;
	}


	/**
	 * 返回底层任务。
	 */
	public Runnable getRunnable() {
		return this.runnable;
	}


	@Override
	public String toString() {
		return this.runnable.toString();
	}

}
