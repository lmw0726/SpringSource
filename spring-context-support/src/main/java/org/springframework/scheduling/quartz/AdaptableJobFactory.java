/*
 * Copyright 2002-2018 the original author or authors.
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

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.util.ReflectionUtils;

/**
 * 支持 {@link java.lang.Runnable} 对象和标准 Quartz {@link org.quartz.Job} 实例的
 * {@link JobFactory} 实现。
 *
 * <p>从 Spring 4.1 开始兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DelegatingJob
 * @see #adaptJob(Object)
 */
public class AdaptableJobFactory implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		try {
			Object jobObject = createJobInstance(bundle);
			return adaptJob(jobObject);
		}
		catch (Throwable ex) {
			throw new SchedulerException("Job instantiation failed", ex);
		}
	}

	/**
	 * 创建指定任务类的实例。
	 * <p>可被重写以对任务实例进行后处理。
	 * @param bundle 从中获取 JobDetail 和触发器触发相关其他信息的 TriggerFiredBundle
	 * @return 任务实例
	 * @throws Exception 如果任务实例化失败
	 */
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Class<?> jobClass = bundle.getJobDetail().getJobClass();
		return ReflectionUtils.accessibleConstructor(jobClass).newInstance();
	}

	/**
	 * 将给定的任务对象适配为 Quartz Job 接口。
	 * <p>默认实现支持直接的 Quartz Job 和 Runnable，
	 * 其中 Runnable 会被包装为 DelegatingJob。
	 * @param jobObject 指定任务类的原始实例
	 * @return 适配后的 Quartz Job 实例
	 * @throws Exception 如果给定的任务无法被适配
	 * @see DelegatingJob
	 */
	protected Job adaptJob(Object jobObject) throws Exception {
		if (jobObject instanceof Job) {
			return (Job) jobObject;
		}
		else if (jobObject instanceof Runnable) {
			return new DelegatingJob((Runnable) jobObject);
		}
		else {
			throw new IllegalArgumentException(
					"Unable to execute job class [" + jobObject.getClass().getName() +
					"]: only [org.quartz.Job] and [java.lang.Runnable] supported.");
		}
	}

}
