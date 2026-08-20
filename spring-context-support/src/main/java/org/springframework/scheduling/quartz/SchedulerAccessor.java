/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * 访问 Quartz 调度器（Scheduler）的通用基类，即用于在
 * {@link org.quartz.Scheduler} 实例上注册作业（Job）、触发器（Trigger）和监听器（Listener）。
 *
 * <p>具体用法请参阅 {@link SchedulerFactoryBean} 和
 * {@link SchedulerAccessorBean} 类。
 *
 * <p>从 Spring 4.1 起，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5.6
 */
public abstract class SchedulerAccessor implements ResourceLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean overwriteExistingJobs = false;

	@Nullable
	private String[] jobSchedulingDataLocations;

	@Nullable
	private List<JobDetail> jobDetails;

	@Nullable
	private Map<String, Calendar> calendars;

	@Nullable
	private List<Trigger> triggers;

	@Nullable
	private SchedulerListener[] schedulerListeners;

	@Nullable
	private JobListener[] globalJobListeners;

	@Nullable
	private TriggerListener[] globalTriggerListeners;

	@Nullable
	private PlatformTransactionManager transactionManager;

	@Nullable
	protected ResourceLoader resourceLoader;


	/**
	 * 设置在此 SchedulerFactoryBean 上定义的作业是否应覆盖已有的作业定义。
	 * 默认为 "false"，即不覆盖从持久化作业存储中读取的已注册作业。
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * 设置遵循 "job_scheduling_data_1_5" XSD 或更高版本的 Quartz 作业定义 XML 文件的位置。
	 * 可指定此属性以自动注册该文件中定义的作业，
	 * 也可以与直接在 SchedulerFactoryBean 上定义的作业配合使用。
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocation(String jobSchedulingDataLocation) {
		this.jobSchedulingDataLocations = new String[] {jobSchedulingDataLocation};
	}

	/**
	 * 设置多个遵循 "job_scheduling_data_1_5" XSD 或更高版本的 Quartz 作业定义 XML 文件的位置。
	 * 可指定此属性以自动注册这些文件中定义的作业，
	 * 也可以与直接在 SchedulerFactoryBean 上定义的作业配合使用。
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations) {
		this.jobSchedulingDataLocations = jobSchedulingDataLocations;
	}

	/**
	 * 向此 FactoryBean 创建的调度器注册一组 JobDetail 对象，供 Trigger 引用。
	 * <p>当 Trigger 自行确定 JobDetail 时，不需要此操作：
	 * 在这种情况下，JobDetail 将随 Trigger 一起隐式注册。
	 * @see #setTriggers
	 * @see org.quartz.JobDetail
	 */
	public void setJobDetails(JobDetail... jobDetails) {
		// 此处使用可修改的 ArrayList，以便在自动检测感知 JobDetail 的 Trigger 时
		// 能够继续添加 JobDetail 对象。
		this.jobDetails = new ArrayList<>(Arrays.asList(jobDetails));
	}

	/**
	 * 向此 FactoryBean 创建的调度器注册一组 Quartz Calendar 对象，供 Trigger 引用。
	 * @param calendars 以日历名称为键、Calendar 对象为值的 Map
	 * @see org.quartz.Calendar
	 */
	public void setCalendars(Map<String, Calendar> calendars) {
		this.calendars = calendars;
	}

	/**
	 * 向此 FactoryBean 创建的调度器注册一组 Trigger 对象。
	 * <p>如果 Trigger 自行确定对应的 JobDetail，
	 * 则该作业将自动注册到调度器。
	 * 否则，需要通过此 FactoryBean 的 "jobDetails" 属性注册相应的 JobDetail。
	 * @see #setJobDetails
	 * @see org.quartz.JobDetail
	 */
	public void setTriggers(Trigger... triggers) {
		this.triggers = Arrays.asList(triggers);
	}

	/**
	 * 指定要注册到调度器的 Quartz SchedulerListener。
	 */
	public void setSchedulerListeners(SchedulerListener... schedulerListeners) {
		this.schedulerListeners = schedulerListeners;
	}

	/**
	 * 指定要注册到调度器的全局 Quartz JobListener。
	 * 这些 JobListener 将应用于调度器中的所有 Job。
	 */
	public void setGlobalJobListeners(JobListener... globalJobListeners) {
		this.globalJobListeners = globalJobListeners;
	}

	/**
	 * 指定要注册到调度器的全局 Quartz TriggerListener。
	 * 这些 TriggerListener 将应用于调度器中的所有 Trigger。
	 */
	public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners) {
		this.globalTriggerListeners = globalTriggerListeners;
	}

	/**
	 * 设置用于注册此 SchedulerFactoryBean 定义的作业和触发器的事务管理器。
	 * 默认为无；仅在为调度器指定 DataSource 时此设置才有意义。
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * 注册作业和触发器（如果可能，在事务中执行）。
	 */
	protected void registerJobsAndTriggers() throws SchedulerException {
		TransactionStatus transactionStatus = null;
		if (this.transactionManager != null) {
			transactionStatus = this.transactionManager.getTransaction(TransactionDefinition.withDefaults());
		}

		try {
			if (this.jobSchedulingDataLocations != null) {
				ClassLoadHelper clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
				clh.initialize();
				XMLSchedulingDataProcessor dataProcessor = new XMLSchedulingDataProcessor(clh);
				for (String location : this.jobSchedulingDataLocations) {
					dataProcessor.processFileAndScheduleJobs(location, getScheduler());
				}
			}

			// 注册 JobDetail。
			if (this.jobDetails != null) {
				for (JobDetail jobDetail : this.jobDetails) {
					addJobToScheduler(jobDetail);
				}
			}
			else {
				// 创建空列表，以便在注册触发器时更方便地检查。
				this.jobDetails = new ArrayList<>();
			}

			// 注册 Calendar。
			if (this.calendars != null) {
				for (String calendarName : this.calendars.keySet()) {
					Calendar calendar = this.calendars.get(calendarName);
					getScheduler().addCalendar(calendarName, calendar, true, true);
				}
			}

			// 注册 Trigger。
			if (this.triggers != null) {
				for (Trigger trigger : this.triggers) {
					addTriggerToScheduler(trigger);
				}
			}
		}

		catch (Throwable ex) {
			if (transactionStatus != null) {
				try {
					this.transactionManager.rollback(transactionStatus);
				}
				catch (TransactionException tex) {
					logger.error("Job registration exception overridden by rollback exception", ex);
					throw tex;
				}
			}
			if (ex instanceof SchedulerException) {
				throw (SchedulerException) ex;
			}
			if (ex instanceof Exception) {
				throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
			}
			throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
		}

		if (transactionStatus != null) {
			this.transactionManager.commit(transactionStatus);
		}
	}

	/**
	 * 如果给定的作业尚不存在，则将其添加到调度器。
	 * 如果设置了 "overwriteExistingJobs"，则无论如何都会覆盖该作业。
	 * @param jobDetail 要添加的作业
	 * @return {@code true} 表示作业已被实际添加，
	 * {@code false} 表示作业之前已存在
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addJobToScheduler(JobDetail jobDetail) throws SchedulerException {
		if (this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null) {
			getScheduler().addJob(jobDetail, true);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 如果给定的触发器尚不存在，则将其添加到调度器。
	 * 如果设置了 "overwriteExistingJobs"，则无论如何都会覆盖该触发器。
	 * @param trigger 要添加的触发器
	 * @return {@code true} 表示触发器已被实际添加，
	 * {@code false} 表示触发器之前已存在
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addTriggerToScheduler(Trigger trigger) throws SchedulerException {
		boolean triggerExists = (getScheduler().getTrigger(trigger.getKey()) != null);
		if (triggerExists && !this.overwriteExistingJobs) {
			return false;
		}

		// 检查 Trigger 是否关联了对应的 JobDetail。
		JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().remove("jobDetail");
		if (triggerExists) {
			if (jobDetail != null && this.jobDetails != null &&
					!this.jobDetails.contains(jobDetail) && addJobToScheduler(jobDetail)) {
				this.jobDetails.add(jobDetail);
			}
			try {
				getScheduler().rescheduleJob(trigger.getKey(), trigger);
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on rescheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
			}
		}
		else {
			try {
				if (jobDetail != null && this.jobDetails != null && !this.jobDetails.contains(jobDetail) &&
						(this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null)) {
					getScheduler().scheduleJob(jobDetail, trigger);
					this.jobDetails.add(jobDetail);
				}
				else {
					getScheduler().scheduleJob(trigger);
				}
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on job scheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
				if (this.overwriteExistingJobs) {
					getScheduler().rescheduleJob(trigger.getKey(), trigger);
				}
			}
		}
		return true;
	}

	/**
	 * 向调度器注册所有指定的监听器。
	 */
	protected void registerListeners() throws SchedulerException {
		ListenerManager listenerManager = getScheduler().getListenerManager();
		if (this.schedulerListeners != null) {
			for (SchedulerListener listener : this.schedulerListeners) {
				listenerManager.addSchedulerListener(listener);
			}
		}
		if (this.globalJobListeners != null) {
			for (JobListener listener : this.globalJobListeners) {
				listenerManager.addJobListener(listener);
			}
		}
		if (this.globalTriggerListeners != null) {
			for (TriggerListener listener : this.globalTriggerListeners) {
				listenerManager.addTriggerListener(listener);
			}
		}
	}


	/**
	 * 模板方法，用于确定要操作的调度器。
	 * 由子类实现。
	 */
	protected abstract Scheduler getScheduler();

}
