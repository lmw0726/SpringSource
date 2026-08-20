/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.scheduling.quartz;

import org.quartz.SchedulerContext;

import org.springframework.beans.factory.Aware;

/**
 * 需要访问 SchedulerContext（但无法自然访问）的
 * Spring 管理的 Quartz 工件需要实现的回调接口。
 *
 * <p>目前仅支持通过 Spring 的 SchedulerFactoryBean
 * 传入的自定义 JobFactory 实现。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see org.quartz.spi.JobFactory
 * @see SchedulerFactoryBean#setJobFactory
 */
public interface SchedulerContextAware extends Aware {

	/**
	 * 设置当前 Quartz 调度器的 SchedulerContext。
	 * @see org.quartz.Scheduler#getContext()
	 */
	void setSchedulerContext(SchedulerContext schedulerContext);

}
