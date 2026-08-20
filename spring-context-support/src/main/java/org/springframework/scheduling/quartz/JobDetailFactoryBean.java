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

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于创建 Quartz {@link org.quartz.JobDetail} 实例的 Spring {@link FactoryBean}，
 * 支持以 Bean 风格配置 JobDetail。
 *
 * <p>{@code JobDetail(Impl)} 本身已经是一个 JavaBean，但缺少合理的默认值。
 * 本类使用 Spring Bean 名称作为任务名称，如果未指定任务组，则使用 Quartz 默认组（"DEFAULT"）。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setName
 * @see #setGroup
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.quartz.Scheduler#DEFAULT_GROUP
 */
public class JobDetailFactoryBean
		implements FactoryBean<JobDetail>, BeanNameAware, ApplicationContextAware, InitializingBean {

	@Nullable
	private String name;

	@Nullable
	private String group;

	@Nullable
	private Class<? extends Job> jobClass;

	private JobDataMap jobDataMap = new JobDataMap();

	private boolean durability = false;

	private boolean requestsRecovery = false;

	@Nullable
	private String description;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private String applicationContextJobDataKey;

	@Nullable
	private JobDetail jobDetail;


	/**
	 * 指定任务的名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定任务的分组。
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 指定任务的实现类。
	 */
	public void setJobClass(Class<? extends Job> jobClass) {
		this.jobClass = jobClass;
	}

	/**
	 * 设置任务的 JobDataMap。
	 * @see #setJobDataAsMap
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * 返回任务的 JobDataMap。
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * 通过给定的 Map 将对象注册到 JobDataMap 中。
	 * <p>这些对象仅对当前任务可用，
	 * 这与 SchedulerContext 中的对象不同。
	 * <p>注意：当使用持久化任务（其 JobDetail 将保存在数据库中）时，
	 * 不要将 Spring 管理的 Bean 或 ApplicationContext 引用放入 JobDataMap，
	 * 而应放入 SchedulerContext 中。
	 * @param jobDataAsMap 包含 String 类型键和任意对象值的 Map
	 * （例如 Spring 管理的 Bean）
	 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean#setSchedulerContextAsMap
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		getJobDataMap().putAll(jobDataAsMap);
	}

	/**
	 * 指定任务的持久性，即即使没有触发器指向该任务，
	 * 该任务是否仍应保留在任务存储中。
	 */
	public void setDurability(boolean durability) {
		this.durability = durability;
	}

	/**
	 * 设置此任务的恢复标志，即当遇到"恢复"或"故障转移"情况时，
	 * 是否应重新执行该任务。
	 */
	public void setRequestsRecovery(boolean requestsRecovery) {
		this.requestsRecovery = requestsRecovery;
	}

	/**
	 * 设置此任务的文本描述。
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 设置要在 JobDataMap 中暴露的 ApplicationContext 引用的键，
	 * 例如 "applicationContext"。默认为无。
	 * 仅适用于在 Spring ApplicationContext 中运行的情况。
	 * <p>对于 QuartzJobBean，该引用将作为 Bean 属性应用到 Job 实例。
	 * 在这种情况下，"applicationContext" 属性将对应于
	 * "setApplicationContext" 方法。
	 * <p>注意，像 ApplicationContextAware 这样的 BeanFactory 回调接口
	 * 不会自动应用到 Quartz Job 实例上，因为 Quartz 本身负责其 Job 的生命周期。
	 * <p><b>注意：当使用持久化任务存储（JobDetail 内容将保存在数据库中）时，
	 * 不要将 ApplicationContext 引用放入 JobDataMap，
	 * 而应放入 SchedulerContext 中。</b>
	 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean#setApplicationContextSchedulerContextKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextJobDataKey(String applicationContextJobDataKey) {
		this.applicationContextJobDataKey = applicationContextJobDataKey;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.jobClass, "Property 'jobClass' is required");

		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.applicationContextJobDataKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
						"JobDetailBean needs to be set up in an ApplicationContext " +
						"to be able to handle an 'applicationContextJobDataKey'");
			}
			getJobDataMap().put(this.applicationContextJobDataKey, this.applicationContext);
		}

		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(this.name != null ? this.name : toString());
		jdi.setGroup(this.group);
		jdi.setJobClass(this.jobClass);
		jdi.setJobDataMap(this.jobDataMap);
		jdi.setDurability(this.durability);
		jdi.setRequestsRecovery(this.requestsRecovery);
		jdi.setDescription(this.description);
		this.jobDetail = jdi;
	}


	@Override
	@Nullable
	public JobDetail getObject() {
		return this.jobDetail;
	}

	@Override
	public Class<?> getObjectType() {
		return JobDetail.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
