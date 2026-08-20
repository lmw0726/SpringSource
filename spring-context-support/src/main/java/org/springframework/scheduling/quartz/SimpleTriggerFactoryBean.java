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

import java.util.Date;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于创建 Quartz {@link org.quartz.SimpleTrigger} 实例的 Spring {@link FactoryBean}，
 * 支持以 Bean 风格配置触发器。
 *
 * <p>{@code SimpleTrigger(Impl)} 本身已经是一个 JavaBean，但缺少合理的默认值。
 * 本类使用 Spring Bean 名称作为任务名称，Quartz 默认分组（"DEFAULT"）作为任务分组，
 * 当前时间作为开始时间，并在未指定时采用无限重复。
 *
 * <p>本类还会使用给定 {@link org.quartz.JobDetail} 的任务名称和分组来注册触发器。
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
public class SimpleTriggerFactoryBean implements FactoryBean<SimpleTrigger>, BeanNameAware, InitializingBean {

	/** SimpleTrigger 类的常量。 */
	private static final Constants constants = new Constants(SimpleTrigger.class);


	@Nullable
	private String name;

	@Nullable
	private String group;

	@Nullable
	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	@Nullable
	private Date startTime;

	private long startDelay;

	private long repeatInterval;

	private int repeatCount = -1;

	private int priority;

	private int misfireInstruction;

	@Nullable
	private String description;

	@Nullable
	private String beanName;

	@Nullable
	private SimpleTrigger simpleTrigger;


	/**
	 * 指定触发器的名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定触发器的分组。
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
	 * 通过给定的 Map 将对象注册到 JobDataMap 中。
	 * <p>这些对象仅对此 Trigger 可用，
	 * 与 JobDetail 数据映射中的对象不同。
	 * @param jobDataAsMap 包含 String 键和任意对象值的 Map
	 * （例如 Spring 管理的 Bean）
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
	 * 设置以毫秒为单位的启动延迟。
	 * <p>启动延迟会加到当前系统时间（Bean 启动时），
	 * 以控制触发器的开始时间。
	 * @see #setStartTime
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * 指定此触发器的执行时间间隔。
	 */
	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	/**
	 * 指定此触发器应触发的次数。
	 * <p>默认为无限重复。
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * 指定此触发器的优先级。
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 指定此触发器的失火指令。
	 */
	public void setMisfireInstruction(int misfireInstruction) {
		this.misfireInstruction = misfireInstruction;
	}

	/**
	 * 通过 {@link org.quartz.SimpleTrigger} 类中对应常量的名称
	 * 设置失火指令。
	 * 默认为 {@code MISFIRE_INSTRUCTION_SMART_POLICY}。
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
	 * @see org.quartz.Trigger#MISFIRE_INSTRUCTION_SMART_POLICY
	 */
	public void setMisfireInstructionName(String constantName) {
		this.misfireInstruction = constants.asNumber(constantName).intValue();
	}

	/**
	 * 为关联此触发器的文本描述。
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() {
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

		SimpleTriggerImpl sti = new SimpleTriggerImpl();
		sti.setName(this.name != null ? this.name : toString());
		sti.setGroup(this.group);
		if (this.jobDetail != null) {
			sti.setJobKey(this.jobDetail.getKey());
		}
		sti.setJobDataMap(this.jobDataMap);
		sti.setStartTime(this.startTime);
		sti.setRepeatInterval(this.repeatInterval);
		sti.setRepeatCount(this.repeatCount);
		sti.setPriority(this.priority);
		sti.setMisfireInstruction(this.misfireInstruction);
		sti.setDescription(this.description);
		this.simpleTrigger = sti;
	}


	@Override
	@Nullable
	public SimpleTrigger getObject() {
		return this.simpleTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return SimpleTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
