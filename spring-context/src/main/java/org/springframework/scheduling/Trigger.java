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

package org.springframework.scheduling;

import java.util.Date;

import org.springframework.lang.Nullable;

/**
 * 触发器对象的通用接口，用于确定与其关联的任务的下一次执行时间。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see TaskScheduler#schedule(Runnable, Trigger)
 * @see org.springframework.scheduling.support.CronTrigger
 */
public interface Trigger {

	/**
	 * 根据给定的触发器上下文确定下一次执行时间。
	 * @param triggerContext 封装了上次执行时间和上次完成时间的上下文对象
	 * @return 触发器定义的下一次执行时间，如果触发器不再触发则返回 {@code null}
	 */
	@Nullable
	Date nextExecutionTime(TriggerContext triggerContext);

}
