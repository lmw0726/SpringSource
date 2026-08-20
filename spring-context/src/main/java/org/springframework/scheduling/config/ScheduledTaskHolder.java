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

import java.util.Set;

/**
 * 用于暴露本地计划任务的通用接口。
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 * @see ScheduledTaskRegistrar
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public interface ScheduledTaskHolder {

	/**
	 * 返回此实例已计划执行的任务概览。
	 */
	Set<ScheduledTask> getScheduledTasks();

}
