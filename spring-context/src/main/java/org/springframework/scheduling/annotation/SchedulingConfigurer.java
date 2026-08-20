/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.scheduling.annotation;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 可选接口，由使用了 {@link EnableScheduling @EnableScheduling} 注解的
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类来实现。通常用于设置特定的
 * {@link org.springframework.scheduling.TaskScheduler TaskScheduler} Bean，
 * 以便在执行定时任务时使用，或者用于以<em>编程式</em>方式注册定时任务，
 * 与使用 {@link Scheduled @Scheduled} 注解的<em>声明式</em>方法相对应。
 * 例如，当实现基于 {@link org.springframework.scheduling.Trigger Trigger} 的任务时
 * （该任务不受 {@code @Scheduled} 注解支持），可能需要使用此接口。
 *
 * <p>详细用法示例请参阅 {@link EnableScheduling @EnableScheduling}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableScheduling
 * @see ScheduledTaskRegistrar
 */
@FunctionalInterface
public interface SchedulingConfigurer {

	/**
	 * 回调方法，允许将 {@link org.springframework.scheduling.TaskScheduler TaskScheduler}
	 * 和特定的 {@link org.springframework.scheduling.config.Task Task} 实例
	 * 注册到给定的 {@link ScheduledTaskRegistrar} 中。
	 * @param taskRegistrar 要配置的注册器。
	 */
	void configureTasks(ScheduledTaskRegistrar taskRegistrar);

}
