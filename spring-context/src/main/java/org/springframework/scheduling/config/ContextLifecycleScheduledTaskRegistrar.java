/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * {@link ScheduledTaskRegistrar} 的子类，将任务的实际调度重定向到
 * {@link #afterSingletonsInstantiated()} 回调（自 4.1.2 起）。
 *
 * @author Juergen Hoeller
 * @since 3.2.1
 */
public class ContextLifecycleScheduledTaskRegistrar extends ScheduledTaskRegistrar implements SmartInitializingSingleton {

	@Override
	public void afterPropertiesSet() {
		// 空操作
	}

	@Override
	public void afterSingletonsInstantiated() {
		scheduleTasks();
	}

}
