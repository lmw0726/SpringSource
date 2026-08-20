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

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.triggers.CronTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于创建 Quartz {@link org.quartz.CronTrigger} 实例的 Spring {@link FactoryBean}，
 * 支持以 bean 风格配置触发器。
 *
 * <p>{@code CronTrigger(Impl)} 本身已经是一个 JavaBean，但缺乏合理的默认值。
 * 本类使用 Spring bean 名称作为任务名称，使用 Quartz 默认组（"DEFAULT"）
 * 作为任务组，使用当前时间作为开始时间，如果未指定则无限重复执行。
 *
 * <p>本类还将使用给定 {@link org.quartz.JobDetail} 的任务名称和组来注册触发器。
 * 这使得 {@link SchedulerFactoryBean} 能够自动为对应的 JobDetail 注册触发器，
 * 而无需单独注册 JobDetail。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setName
 * @see #setGroup
 * @see #setStartDelay
 * @see #setJobDetail
 * @see SchedulerFactoryBean#setTriggers
 * @see SchedulerFactoryBean#setJobDetails
 */
public class CronTriggerFactoryBean implements FactoryBean<CronTrigger>, BeanNameAware, InitializingBean {

	/** CronTrigger 类的常量。 */
	private static final Constants constants = new Constants(CronTrigger.class);


	@Nullable
	private String name;

	@Nullable
	private String group;

	@Nullable
	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	@Nullable
	private Date startTime;

	private long startDelay = 0;

	@Nullable
	private String cronExpression;

	@Nullable
	private TimeZone timeZone;

	@Nullable
	private String calendarName;

	private int priority;

	private int misfireInstruction;

	@Nullable
	private String description;

	@Nullable
	private String beanName;

	@Nullable
	private CronTrigger cronTrigger;


	/**
	 * 设置触发器的名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置触发器的分组。
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 设置此触发器应关联的 JobDetail。
	 */
	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	/**
	 * 设置触发器的 JobDataMap。
	 * @see #setJobDataAsMap
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * 返回触发器的 JobDataMap。
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * 通过给定的 Map 在 JobDataMap 中注册对象。
	 * <p>这些对象将仅对此 Trigger 可用，
	 * 而 JobDetail 的数据映射中的对象则不同。
	 * @param jobDataAsMap 包含 String 键和任意对象值的 Map
	 *（例如 Spring 管理的 bean）
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		this.jobDataMap.putAll(jobDataAsMap);
	}

	/**
	 * 设置触发器的特定开始时间。
	 * <p>请注意，动态计算的 {@link #setStartDelay} 规范
	 * 会覆盖此处设置的静态时间戳。
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * 设置启动延迟时间（以毫秒为单位）。
	 * <p>启动延迟时间会加到当前系统时间上（当 bean 启动时），
	 * 以控制触发器的开始时间。
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * 设置此触发器的 cron 表达式。
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * 设置此触发器 cron 表达式的时区。
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 将特定日历与此 cron 触发器关联。
	 */
	public void setCalendarName(String calendarName) {
		this.calendarName = calendarName;
	}

	/**
	 * 设置此触发器的优先级。
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 设置此触发器的 misfire 指令。
	 */
	public void setMisfireInstruction(int misfireInstruction) {
		this.misfireInstruction = misfireInstruction;
	}

	/**
	 * 通过 {@link org.quartz.CronTrigger} 类中对应常量的名称
	 * 设置 misfire 指令。
	 * 默认值为 {@code MISFIRE_INSTRUCTION_SMART_POLICY}。
	 * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
	 * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
	 * @see org.quartz.Trigger#MISFIRE_INSTRUCTION_SMART_POLICY
	 */
	public void setMisfireInstructionName(String constantName) {
		this.misfireInstruction = constants.asNumber(constantName).intValue();
	}

	/**
	 * 将文本描述与此触发器关联。
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() throws ParseException {
		Assert.notNull(this.cronExpression, "Property 'cronExpression' is required");

		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.jobDetail != null) {
			this.jobDataMap.put("jobDetail", this.jobDetail);
		}
		if (this.startDelay > 0 || this.startTime == null) {
			this.startTime = new Date(System.currentTimeMillis() + this.startDelay);
		}
		if (this.timeZone == null) {
			this.timeZone = TimeZone.getDefault();
		}

		CronTriggerImpl cti = new CronTriggerImpl();
		cti.setName(this.name != null ? this.name : toString());
		cti.setGroup(this.group);
		if (this.jobDetail != null) {
			cti.setJobKey(this.jobDetail.getKey());
		}
		cti.setJobDataMap(this.jobDataMap);
		cti.setStartTime(this.startTime);
		cti.setCronExpression(this.cronExpression);
		cti.setTimeZone(this.timeZone);
		cti.setCalendarName(this.calendarName);
		cti.setPriority(this.priority);
		cti.setMisfireInstruction(this.misfireInstruction);
		cti.setDescription(this.description);
		this.cronTrigger = cti;
	}


	@Override
	@Nullable
	public CronTrigger getObject() {
		return this.cronTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return CronTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
