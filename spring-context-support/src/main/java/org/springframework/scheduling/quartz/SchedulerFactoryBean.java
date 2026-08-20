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

package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 用于创建和配置 Quartz {@link org.quartz.Scheduler} 的 {@link FactoryBean}，
 * 作为 Spring 应用上下文的一部分管理其生命周期，并将 Scheduler 暴露为
 * 用于依赖注入的 bean 引用。
 *
 * <p>允许注册 JobDetails、Calendars 和 Triggers，在初始化时自动启动调度器，
 * 在销毁时关闭调度器。对于仅需要在启动时静态注册任务的场景，不需要在
 * 应用代码中访问 Scheduler 实例本身。
 *
 * <p>如需在运行时动态注册任务，请使用对本 SchedulerFactoryBean 的 bean 引用
 * 来直接访问 Quartz Scheduler（{@code org.quartz.Scheduler}）。这允许您创建
 * 新的任务和触发器，以及控制和监控整个 Scheduler。
 *
 * <p>请注意，Quartz 会为每次执行实例化一个新的 Job，这与 Timer 不同，
 * Timer 使用在重复执行之间共享的 TimerTask 实例。只有 JobDetail 描述符是共享的。
 *
 * <p>使用持久化任务时，强烈建议在 Spring 管理（或纯 JTA）的事务中执行
 * Scheduler 上的所有操作。否则，数据库锁可能无法正常工作，甚至可能失败。
 * （详见 {@link #setDataSource setDataSource} javadoc。）
 *
 * <p>实现事务执行的首选方式是在业务外观层声明式地划定事务边界，
 * 这将自动应用于在这些范围内执行的 Scheduler 操作。
 * 或者，您也可以为 Scheduler 本身添加事务通知。
 *
 * <p>自 Spring 4.1 起，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see #setDataSource
 * @see org.quartz.Scheduler
 * @see org.quartz.SchedulerFactory
 * @see org.quartz.impl.StdSchedulerFactory
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean
 */
public class SchedulerFactoryBean extends SchedulerAccessor implements FactoryBean<Scheduler>,
		BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle {


	/**
	 * 线程数属性。
	 */
	public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";

	/**
	 * 默认线程数。
	 */
	public static final int DEFAULT_THREAD_COUNT = 10;


	private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder = new ThreadLocal<>();

	private static final ThreadLocal<Executor> configTimeTaskExecutorHolder = new ThreadLocal<>();

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal<>();

	private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder = new ThreadLocal<>();


	/**
	 * 返回当前已配置的 Quartz Scheduler 的 {@link ResourceLoader}，
	 * 供 {@link ResourceLoaderClassLoadHelper} 使用。
	 * <p>此实例将在相应 Scheduler 初始化之前设置，并在之后立即重置。
	 * 因此仅在配置期间可用。
	 * @see #setApplicationContext
	 * @see ResourceLoaderClassLoadHelper
	 */
	@Nullable
	public static ResourceLoader getConfigTimeResourceLoader() {
		return configTimeResourceLoaderHolder.get();
	}

	/**
	 * 返回当前已配置的 Quartz Scheduler 的 {@link Executor}，
	 * 供 {@link LocalTaskExecutorThreadPool} 使用。
	 * <p>此实例将在相应 Scheduler 初始化之前设置，并在之后立即重置。
	 * 因此仅在配置期间可用。
	 * @since 2.0
	 * @see #setTaskExecutor
	 * @see LocalTaskExecutorThreadPool
	 */
	@Nullable
	public static Executor getConfigTimeTaskExecutor() {
		return configTimeTaskExecutorHolder.get();
	}

	/**
	 * 返回当前已配置的 Quartz Scheduler 的 {@link DataSource}，
	 * 供 {@link LocalDataSourceJobStore} 使用。
	 * <p>此实例将在相应 Scheduler 初始化之前设置，并在之后立即重置。
	 * 因此仅在配置期间可用。
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	@Nullable
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * 返回当前已配置的 Quartz Scheduler 的非事务性 {@link DataSource}，
	 * 供 {@link LocalDataSourceJobStore} 使用。
	 * <p>此实例将在相应 Scheduler 初始化之前设置，并在之后立即重置。
	 * 因此仅在配置期间可用。
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see LocalDataSourceJobStore
	 */
	@Nullable
	public static DataSource getConfigTimeNonTransactionalDataSource() {
		return configTimeNonTransactionalDataSourceHolder.get();
	}


	@Nullable
	private SchedulerFactory schedulerFactory;

	private Class<? extends SchedulerFactory> schedulerFactoryClass = StdSchedulerFactory.class;

	@Nullable
	private String schedulerName;

	@Nullable
	private Resource configLocation;

	@Nullable
	private Properties quartzProperties;

	@Nullable
	private Executor taskExecutor;

	@Nullable
	private DataSource dataSource;

	@Nullable
	private DataSource nonTransactionalDataSource;

	@Nullable
	private Map<String, ?> schedulerContextMap;

	@Nullable
	private String applicationContextSchedulerContextKey;

	@Nullable
	private JobFactory jobFactory;

	private boolean jobFactorySet = false;

	private boolean autoStartup = true;

	private int startupDelay = 0;

	private int phase = DEFAULT_PHASE;

	private boolean exposeSchedulerInRepository = false;

	private boolean waitForJobsToCompleteOnShutdown = false;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private Scheduler scheduler;


	/**
	 * 设置要使用的外部 Quartz {@link SchedulerFactory} 实例。
	 * <p>默认使用内部的 {@link StdSchedulerFactory} 实例。如果调用此方法，
	 * 它将覆盖通过 {@link #setSchedulerFactoryClass} 指定的任何类，
	 * 以及通过 {@link #setConfigLocation}、{@link #setQuartzProperties}、
	 * {@link #setTaskExecutor} 或 {@link #setDataSource} 指定的任何设置。
	 * <p><b>注意：</b>使用外部提供的 {@code SchedulerFactory} 实例时，
	 * {@code SchedulerFactoryBean} 中的本地设置（如 {@link #setConfigLocation}
	 * 或 {@link #setQuartzProperties}）将被忽略，因为外部 {@code SchedulerFactory}
	 * 实例需要自行初始化。
	 * @since 4.3.15
	 * @see #setSchedulerFactoryClass
	 */
	public void setSchedulerFactory(SchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
	}

	/**
	 * 设置要使用的 Quartz {@link SchedulerFactory} 实现类。
	 * <p>默认使用 {@link StdSchedulerFactory} 类，从 {@code quartz.jar}
	 * 中读取标准的 {@code quartz.properties}。要应用自定义 Quartz 属性，
	 * 请在此本地 {@code SchedulerFactoryBean} 实例上指定
	 * {@link #setConfigLocation "configLocation"} 和/或
	 * {@link #setQuartzProperties "quartzProperties"} 等。
	 * @see org.quartz.impl.StdSchedulerFactory
	 * @see #setConfigLocation
	 * @see #setQuartzProperties
	 * @see #setTaskExecutor
	 * @see #setDataSource
	 */
	public void setSchedulerFactoryClass(Class<? extends SchedulerFactory> schedulerFactoryClass) {
		this.schedulerFactoryClass = schedulerFactoryClass;
	}

	/**
	 * 设置通过 SchedulerFactory 创建的 Scheduler 名称，
	 * 作为 {@code org.quartz.scheduler.instanceName} 属性的替代方式。
	 * <p>如果未指定，名称将从 Quartz 属性
	 * ({@code org.quartz.scheduler.instanceName}) 中获取，
	 * 或回退到声明的 {@code SchedulerFactoryBean} bean 名称。
	 * @see #setBeanName
	 * @see StdSchedulerFactory#PROP_SCHED_INSTANCE_NAME
	 * @see org.quartz.SchedulerFactory#getScheduler()
	 * @see org.quartz.SchedulerFactory#getScheduler(String)
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * 设置 Quartz 属性配置文件的位置，例如类路径资源
	 * "classpath:quartz.properties"。
	 * <p>注意：当所有必要的属性已通过此 bean 本地指定，
	 * 或依赖 Quartz 的默认配置时，可以省略此设置。
	 * @see #setQuartzProperties
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置 Quartz 属性，如 "org.quartz.threadPool.class"。
	 * <p>可用于覆盖 Quartz 属性配置文件中的值，
	 * 或在本地指定所有必要的属性。
	 * @see #setConfigLocation
	 */
	public void setQuartzProperties(Properties quartzProperties) {
		this.quartzProperties = quartzProperties;
	}

	/**
	 * 设置用作 Quartz 后端的 Spring 管理的 {@link Executor}，
	 * 通过 Quartz SPI 暴露为线程池。
	 * <p>可用于将本地 JDK ThreadPoolExecutor 或 CommonJ
	 * WorkManager 分配为 Quartz 后端，以避免 Quartz 的手动线程创建。
	 * <p>默认情况下，将使用 Quartz SimpleThreadPool，
	 * 通过相应的 Quartz 属性进行配置。
	 * @since 2.0
	 * @see #setQuartzProperties
	 * @see LocalTaskExecutorThreadPool
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
	 * @see org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 设置 Scheduler 使用的默认 {@link DataSource}。
	 * <p>注意：如果设置了此属性，Quartz 设置中不应定义
	 * 任务存储的 "dataSource"，以避免无意义的双重配置。
	 * 同时，不要定义 "org.quartz.jobStore.class" 属性。
	 * （您可以显式定义 Spring 的 {@link LocalDataSourceJobStore}，
	 * 但使用此方法时它已经是默认值。）
	 * <p>将使用 Quartz JobStoreCMT 的 Spring 特定子类。
	 * 因此强烈建议在 Spring 管理（或纯 JTA）的事务中执行
	 * Scheduler 上的所有操作。否则，数据库锁可能无法正常工作，
	 * 甚至可能失败（例如，在没有事务的情况下尝试在 Oracle 上获取锁）。
	 * <p>支持事务性和非事务性 DataSource 访问。
	 * 对于非 XA DataSource 和本地 Spring 事务，单个 DataSource
	 * 参数就足够了。对于 XA DataSource 和全局 JTA 事务，
	 * 应设置 SchedulerFactoryBean 的 "nonTransactionalDataSource" 属性，
	 * 传入不参与全局事务的非 XA DataSource。
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see #setQuartzProperties
	 * @see #setTransactionManager
	 * @see LocalDataSourceJobStore
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置用于<i>非事务性访问</i>的 {@link DataSource}。
	 * <p>仅当默认 DataSource 是始终参与事务的 XA DataSource 时才需要此设置：
	 * 在这种情况下，应将该 DataSource 的非 XA 版本指定为 "nonTransactionalDataSource"。
	 * <p>对于本地 DataSource 实例和 Spring 事务，此设置无关紧要。
	 * 在这种情况下，只需指定单个默认 DataSource 作为 "dataSource" 即可。
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource) {
		this.nonTransactionalDataSource = nonTransactionalDataSource;
	}

	/**
	 * 通过给定的 Map 注册对象到 Scheduler 上下文中。
	 * 这些对象将可供在此 Scheduler 中运行的任何 Job 使用。
	 * <p>注意：使用持久化 Job（其 JobDetail 将保存在数据库中）时，
	 * 不要将 Spring 管理的 bean 或 ApplicationContext 引用
	 * 放入 JobDataMap 中，而应放入 SchedulerContext 中。
	 * @param schedulerContextAsMap 一个以 String 为键、任意对象为值的 Map
	 * （例如 Spring 管理的 bean）
	 * @see JobDetailFactoryBean#setJobDataAsMap
	 */
	public void setSchedulerContextAsMap(Map<String, ?> schedulerContextAsMap) {
		this.schedulerContextMap = schedulerContextAsMap;
	}

	/**
	 * 设置要在 SchedulerContext 中暴露的 {@link ApplicationContext}
	 * 引用的键，例如 "applicationContext"。默认为无。
	 * 仅在 Spring ApplicationContext 中运行时适用。
	 * <p>注意：使用持久化 Job（其 JobDetail 将保存在数据库中）时，
	 * 不要将 ApplicationContext 引用放入 JobDataMap 中，
	 * 而应放入 SchedulerContext 中。
	 * <p>对于 QuartzJobBean，该引用将作为 bean 属性应用于 Job 实例。
	 * 在这种情况下，"applicationContext" 属性将对应
	 * "setApplicationContext" 方法。
	 * <p>请注意，ApplicationContextAware 等 BeanFactory 回调接口
	 * 不会自动应用于 Quartz Job 实例，因为 Quartz 自身负责其 Job 的生命周期。
	 * @see JobDetailFactoryBean#setApplicationContextJobDataKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey) {
		this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
	}

	/**
	 * 设置此 Scheduler 要使用的 Quartz {@link JobFactory}。
	 * <p>默认使用 Spring 的 {@link AdaptableJobFactory}，它支持
	 * {@link java.lang.Runnable} 对象以及标准的 Quartz
	 * {@link org.quartz.Job} 实例。请注意，此默认值仅适用于
	 * <i>本地</i> Scheduler，不适用于 RemoteScheduler
	 * （Quartz 不支持在 RemoteScheduler 上设置自定义 JobFactory）。
	 * <p>在此处指定 Spring 的 {@link SpringBeanJobFactory} 实例
	 * （通常作为内部 bean 定义），可以从指定的 job data map
	 * 和 scheduler context 自动填充 job 的 bean 属性。
	 * @since 2.0
	 * @see AdaptableJobFactory
	 * @see SpringBeanJobFactory
	 */
	public void setJobFactory(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.jobFactorySet = true;
	}

	/**
	 * 设置初始化后是否自动启动调度器。
	 * <p>默认为 "true"；设置为 "false" 以允许手动启动。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回此调度器是否配置为自动启动。如果为 "true"，
	 * 调度器将在上下文刷新后以及启动延迟（如果有）后启动。
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定此调度器应启动和停止的阶段。
	 * 启动顺序从最低到最高，关闭顺序则相反。
	 * 默认值为 {@code Integer.MAX_VALUE}，
	 * 意味着此调度器尽可能晚地启动并尽快停止。
	 * @since 3.0
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此调度器将启动和停止的阶段。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * 设置初始化后异步启动调度器之前等待的秒数。
	 * 默认值为 0，表示在初始化此 bean 时立即同步启动。
	 * <p>如果在应用程序完全启动之前不应运行任何任务，
	 * 将此值设置为 10 或 20 秒是合理的。
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * 设置是否在 Quartz {@link SchedulerRepository} 中暴露 Spring 管理的
	 * {@link Scheduler} 实例。默认为 "false"，因为 Spring 管理的
	 * Scheduler 通常专门用于在 Spring 上下文中访问。
	 * <p>将此标志切换为 "true" 以全局暴露 Scheduler。
	 * 除非您有现有的 Spring 应用程序依赖此行为，否则不建议这样做。
	 * 请注意，这种全局暴露在早期的 Spring 版本中是意外的默认行为；
	 * 自 Spring 2.5.6 起已修复此问题。
	 */
	public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository) {
		this.exposeSchedulerInRepository = exposeSchedulerInRepository;
	}

	/**
	 * 设置关闭时是否等待正在运行的任务完成。
	 * <p>默认为 "false"。如果您倾向于让任务完全完成，
	 * 而不介意更长的关闭阶段，请将此值切换为 "true"。
	 * @see org.quartz.Scheduler#shutdown(boolean)
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	//---------------------------------------------------------------------
	// InitializingBean 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.dataSource == null && this.nonTransactionalDataSource != null) {
			this.dataSource = this.nonTransactionalDataSource;
		}

		if (this.applicationContext != null && this.resourceLoader == null) {
			this.resourceLoader = this.applicationContext;
		}

		// 初始化 Scheduler 实例...
		this.scheduler = prepareScheduler(prepareSchedulerFactory());
		try {
			registerListeners();
			registerJobsAndTriggers();
		}
		catch (Exception ex) {
			try {
				this.scheduler.shutdown(true);
			}
			catch (Exception ex2) {
				logger.debug("Scheduler shutdown exception after registration failure", ex2);
			}
			throw ex;
		}
	}


	/**
	 * 如有必要，创建 SchedulerFactory 并将本地定义的 Quartz 属性应用于它。
	 * @return 初始化后的 SchedulerFactory
	 */
	private SchedulerFactory prepareSchedulerFactory() throws SchedulerException, IOException {
		SchedulerFactory schedulerFactory = this.schedulerFactory;
		if (schedulerFactory == null) {
			// 创建本地 SchedulerFactory 实例（通常是 StdSchedulerFactory）
			schedulerFactory = BeanUtils.instantiateClass(this.schedulerFactoryClass);
			if (schedulerFactory instanceof StdSchedulerFactory) {
				initSchedulerFactory((StdSchedulerFactory) schedulerFactory);
			}
			else if (this.configLocation != null || this.quartzProperties != null ||
					this.taskExecutor != null || this.dataSource != null) {
				throw new IllegalArgumentException(
						"StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
			}
			// 否则，无需通过 StdSchedulerFactory.initialize(Properties) 应用本地设置
		}
		// 否则，假设外部提供的工厂已使用适当的设置进行了初始化
		return schedulerFactory;
	}

	/**
	 * 初始化给定的 SchedulerFactory，将本地定义的 Quartz 属性应用于它。
	 * @param schedulerFactory 要初始化的 SchedulerFactory
	 */
	private void initSchedulerFactory(StdSchedulerFactory schedulerFactory) throws SchedulerException, IOException {
		Properties mergedProps = new Properties();
		if (this.resourceLoader != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
					ResourceLoaderClassLoadHelper.class.getName());
		}

		if (this.taskExecutor != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
					LocalTaskExecutorThreadPool.class.getName());
		}
		else {
			// 在此处设置必要的默认属性，因为 Quartz 在显式给定属性时
			// 不会应用其默认配置。
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
			mergedProps.setProperty(PROP_THREAD_COUNT, Integer.toString(DEFAULT_THREAD_COUNT));
		}

		if (this.configLocation != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading Quartz config from [" + this.configLocation + "]");
			}
			PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
		}

		CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
		if (this.dataSource != null) {
			mergedProps.putIfAbsent(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
		}

		// 确定本地设置和 Quartz 属性中的调度器名称...
		if (this.schedulerName != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.schedulerName);
		}
		else {
			String nameProp = mergedProps.getProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME);
			if (nameProp != null) {
				this.schedulerName = nameProp;
			}
			else if (this.beanName != null) {
				mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.beanName);
				this.schedulerName = this.beanName;
			}
		}

		schedulerFactory.initialize(mergedProps);
	}

	private Scheduler prepareScheduler(SchedulerFactory schedulerFactory) throws SchedulerException {
		if (this.resourceLoader != null) {
			// 使给定的 ResourceLoader 可用于 SchedulerFactory 配置。
			configTimeResourceLoaderHolder.set(this.resourceLoader);
		}
		if (this.taskExecutor != null) {
			// 使给定的 TaskExecutor 可用于 SchedulerFactory 配置。
			configTimeTaskExecutorHolder.set(this.taskExecutor);
		}
		if (this.dataSource != null) {
			// 使给定的 DataSource 可用于 SchedulerFactory 配置。
			configTimeDataSourceHolder.set(this.dataSource);
		}
		if (this.nonTransactionalDataSource != null) {
			// 使给定的非事务性 DataSource 可用于 SchedulerFactory 配置。
			configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
		}

		// 从 SchedulerFactory 获取 Scheduler 实例。
		try {
			Scheduler scheduler = createScheduler(schedulerFactory, this.schedulerName);
			populateSchedulerContext(scheduler);

			if (!this.jobFactorySet && !(scheduler instanceof RemoteScheduler)) {
				// 对于本地 Scheduler，使用 AdaptableJobFactory 作为默认值，
				// 除非通过 "jobFactory" bean 属性显式给定 null 值。
				this.jobFactory = new AdaptableJobFactory();
			}
			if (this.jobFactory != null) {
				if (this.applicationContext != null && this.jobFactory instanceof ApplicationContextAware) {
					((ApplicationContextAware) this.jobFactory).setApplicationContext(this.applicationContext);
				}
				if (this.jobFactory instanceof SchedulerContextAware) {
					((SchedulerContextAware) this.jobFactory).setSchedulerContext(scheduler.getContext());
				}
				scheduler.setJobFactory(this.jobFactory);
			}
			return scheduler;
		}

		finally {
			if (this.resourceLoader != null) {
				configTimeResourceLoaderHolder.remove();
			}
			if (this.taskExecutor != null) {
				configTimeTaskExecutorHolder.remove();
			}
			if (this.dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.nonTransactionalDataSource != null) {
				configTimeNonTransactionalDataSourceHolder.remove();
			}
		}
	}

	/**
	 * 为给定的工厂和调度器名称创建 Scheduler 实例。
	 * 由 {@link #afterPropertiesSet} 调用。
	 * <p>默认实现调用 SchedulerFactory 的 {@code getScheduler}
	 * 方法。可以被重写以实现自定义的 Scheduler 创建。
	 * @param schedulerFactory 用于创建 Scheduler 的工厂
	 * @param schedulerName 要创建的调度器名称
	 * @return Scheduler 实例
	 * @throws SchedulerException 如果 Quartz 方法抛出异常
	 * @see #afterPropertiesSet
	 * @see org.quartz.SchedulerFactory#getScheduler
	 */
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, @Nullable String schedulerName)
			throws SchedulerException {

		// 覆盖线程上下文 ClassLoader，以解决 Quartz ClassLoadHelper 加载方式过于简单的问题。
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader = (this.resourceLoader != null &&
				this.resourceLoader.getClassLoader() != threadContextClassLoader);
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.resourceLoader.getClassLoader());
		}
		try {
			SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				Scheduler existingScheduler = (schedulerName != null ? repository.lookup(schedulerName) : null);
				Scheduler newScheduler = schedulerFactory.getScheduler();
				if (newScheduler == existingScheduler) {
					throw new IllegalStateException("Active Scheduler of name '" + schedulerName + "' already registered " +
							"in Quartz SchedulerRepository. Cannot create a new Spring-managed Scheduler of the same name!");
				}
				if (!this.exposeSchedulerInRepository) {
					// 在此情况下需要将其移除，因为 Quartz 默认共享 Scheduler 实例！
					SchedulerRepository.getInstance().remove(newScheduler.getSchedulerName());
				}
				return newScheduler;
			}
		}
		finally {
			if (overrideClassLoader) {
				// 恢复原始的线程上下文 ClassLoader。
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * 在 Quartz SchedulerContext 中暴露指定的上下文属性和/或当前
	 * ApplicationContext。
	 */
	private void populateSchedulerContext(Scheduler scheduler) throws SchedulerException {
		// 将指定的对象放入 Scheduler 上下文中。
		if (this.schedulerContextMap != null) {
			scheduler.getContext().putAll(this.schedulerContextMap);
		}

		// 在 Scheduler 上下文中注册 ApplicationContext。
		if (this.applicationContextSchedulerContextKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
						"SchedulerFactoryBean needs to be set up in an ApplicationContext " +
						"to be able to handle an 'applicationContextSchedulerContextKey'");
			}
			scheduler.getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
		}
	}


	/**
	 * 启动 Quartz Scheduler，遵循 "startupDelay" 设置。
	 * @param scheduler 要启动的 Scheduler
	 * @param startupDelay 在异步启动 Scheduler 之前等待的秒数
	 */
	protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {
		if (startupDelay <= 0) {
			logger.info("Starting Quartz Scheduler now");
			scheduler.start();
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() +
						"] in " + startupDelay + " seconds");
			}
			// 不使用 Quartz 的 startDelayed 方法，因为我们明确希望这里是守护线程，
			// 在所有其他线程结束时不会阻止 JVM 退出。
			Thread schedulerThread = new Thread() {
				@Override
				public void run() {
					try {
						TimeUnit.SECONDS.sleep(startupDelay);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						// 直接继续执行
					}
					if (logger.isInfoEnabled()) {
						logger.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
					}
					try {
						scheduler.start();
					}
					catch (SchedulerException ex) {
						throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
					}
				}
			};
			schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
			schedulerThread.setDaemon(true);
			schedulerThread.start();
		}
	}


	//---------------------------------------------------------------------
	// FactoryBean 接口的实现
	//---------------------------------------------------------------------

	@Override
	public Scheduler getScheduler() {
		Assert.state(this.scheduler != null, "No Scheduler set");
		return this.scheduler;
	}

	@Override
	@Nullable
	public Scheduler getObject() {
		return this.scheduler;
	}

	@Override
	public Class<? extends Scheduler> getObjectType() {
		return (this.scheduler != null ? this.scheduler.getClass() : Scheduler.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// SmartLifecycle 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void start() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				startScheduler(this.scheduler, this.startupDelay);
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not start Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				this.scheduler.standby();
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not stop Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public boolean isRunning() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				return !this.scheduler.isInStandbyMode();
			}
			catch (SchedulerException ex) {
				return false;
			}
		}
		return false;
	}


	//---------------------------------------------------------------------
	// DisposableBean 接口的实现
	//---------------------------------------------------------------------

	/**
	 * 在 bean 工厂关闭时关闭 Quartz 调度器，
	 * 停止所有已计划的任务。
	 */
	@Override
	public void destroy() throws SchedulerException {
		if (this.scheduler != null) {
			logger.info("Shutting down Quartz Scheduler");
			this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
		}
	}

}
