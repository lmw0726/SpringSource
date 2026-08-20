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

package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerListener;

import org.springframework.util.Assert;

/**
 * 一个简单的 TimerListener 适配器，将执行委托给给定的 Runnable。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see commonj.timers.TimerListener
 * @see java.lang.Runnable
 * @deprecated 从 5.1 版本起已弃用，建议使用 EE 7 的
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public class DelegatingTimerListener implements TimerListener {

	private final Runnable runnable;


	/**
	 * 创建一个新的 DelegatingTimerListener。
	 * @param runnable 要委托的 Runnable 实现
	 */
	public DelegatingTimerListener(Runnable runnable) {
		Assert.notNull(runnable, "Runnable is required");
		this.runnable = runnable;
	}


	/**
	 * 将执行委托给底层的 Runnable。
	 */
	@Override
	public void timerExpired(Timer timer) {
		this.runnable.run();
	}

}
