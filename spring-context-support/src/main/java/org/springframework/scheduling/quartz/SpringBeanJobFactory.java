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

import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

/**
 * {@link AdaptableJobFactory} 的子类，同时支持 Spring 风格的
 * bean 属性依赖注入。本质上是 Spring 的 {@link QuartzJobBean}
 * 以 Quartz {@link org.quartz.spi.JobFactory} 形式的直接等价实现。
 *
 * <p>将调度器上下文（scheduler context）、任务数据映射（job data map）
 * 和触发器数据映射（trigger data map）中的条目作为 bean 属性值进行应用。
 * 如果未找到匹配的 bean 属性，默认情况下将直接忽略该条目。
 * 这与 QuartzJobBean 的行为类似。
 *
 * <p>自 Spring 4.1 起，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulerFactoryBean#setJobFactory
 * @see QuartzJobBean
 */
public class SpringBeanJobFactory extends AdaptableJobFactory
		implements ApplicationContextAware, SchedulerContextAware {

	@Nullable
	private String[] ignoredUnknownProperties;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private SchedulerContext schedulerContext;


	/**
	 * 指定应忽略的未知属性（在 bean 中未找到的属性）。
	 * <p>默认值为 {@code null}，表示应忽略所有未知属性。
	 * 指定一个空数组可在遇到任何未知属性时抛出异常，
	 * 或指定一个属性名列表，当特定任务类上未找到相应属性时忽略这些属性
	 * （所有其他未知属性仍会触发异常）。
	 */
	public void setIgnoredUnknownProperties(String... ignoredUnknownProperties) {
		this.ignoredUnknownProperties = ignoredUnknownProperties;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext) {
		this.schedulerContext = schedulerContext;
	}


	/**
	 * 创建任务实例，使用从调度器上下文、任务数据映射和触发器数据映射中
	 * 获取的属性值来填充该实例。
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Object job = (this.applicationContext != null ?
				this.applicationContext.getAutowireCapableBeanFactory().createBean(
						bundle.getJobDetail().getJobClass(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false) :
				super.createJobInstance(bundle));

		if (isEligibleForPropertyPopulation(job)) {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
			MutablePropertyValues pvs = new MutablePropertyValues();
			if (this.schedulerContext != null) {
				pvs.addPropertyValues(this.schedulerContext);
			}
			pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
			pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
			if (this.ignoredUnknownProperties != null) {
				for (String propName : this.ignoredUnknownProperties) {
					if (pvs.contains(propName) && !bw.isWritableProperty(propName)) {
						pvs.removePropertyValue(propName);
					}
				}
				bw.setPropertyValues(pvs);
			}
			else {
				bw.setPropertyValues(pvs, true);
			}
		}

		return job;
	}

	/**
	 * 返回给定的任务对象是否适合填充其 bean 属性。
	 * <p>默认实现会忽略 {@link QuartzJobBean} 实例，
	 * 因为它们会自行注入 bean 属性。
	 * @param jobObject 要检查的任务对象
	 * @see QuartzJobBean
	 */
	protected boolean isEligibleForPropertyPopulation(Object jobObject) {
		return (!(jobObject instanceof QuartzJobBean));
	}

}
