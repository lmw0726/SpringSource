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

package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Quartz Job 接口的简单实现，将传入的 JobDataMap 和 SchedulerContext 作为 bean 属性值进行设置。
 * 这样做是合理的，因为每次执行都会创建一个新的 Job 实例。
 * JobDataMap 中的条目会覆盖 SchedulerContext 中相同键的条目。
 *
 * <p>例如，假设 JobDataMap 包含一个键 "myParam"，其值为 "5"：
 * Job 实现可以暴露一个类型为 int 的 bean 属性 "myParam" 来接收该值，
 * 即通过 "setMyParam(int)" 方法。这对复杂类型（如业务对象等）同样适用。
 *
 * <p><b>请注意，向 Job 实例应用依赖注入的首选方式是通过 JobFactory：</b>
 * 即指定 {@link SpringBeanJobFactory} 作为 Quartz JobFactory
 * （通常通过 {@link SchedulerFactoryBean#setJobFactory} SchedulerFactoryBean 的 "jobFactory" 属性）。
 * 这样可以实现依赖注入的 Quartz Job，而无需依赖 Spring 基类。
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see org.quartz.JobExecutionContext#getMergedJobDataMap()
 * @see org.quartz.Scheduler#getContext()
 * @see SchedulerFactoryBean#setSchedulerContextAsMap
 * @see SpringBeanJobFactory
 * @see SchedulerFactoryBean#setJobFactory
 */
public abstract class QuartzJobBean implements Job {

	/**
	 * 此实现将传入的作业数据映射（job data map）作为 bean 属性值进行设置，
	 * 然后委托给 {@code executeInternal} 执行。
	 * @see #executeInternal
	 */
	@Override
	public final void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValues(context.getScheduler().getContext());
			pvs.addPropertyValues(context.getMergedJobDataMap());
			bw.setPropertyValues(pvs, true);
		}
		catch (SchedulerException ex) {
			throw new JobExecutionException(ex);
		}
		executeInternal(context);
	}

	/**
	 * 执行实际的作业。作业数据映射已通过 execute 方法作为 bean 属性值进行了设置。
	 * 该方法的契约与标准 Quartz execute 方法完全相同。
	 * @see #execute
	 */
	protected abstract void executeInternal(JobExecutionContext context) throws JobExecutionException;

}
