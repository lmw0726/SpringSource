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

package org.springframework.scheduling.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.SchedulerRepository;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于访问 Quartz Scheduler 的 Spring bean 风格类，即用于在给定的
 * {@link org.quartz.Scheduler} 实例上注册作业、触发器和监听器。
 *
 * <p>从 Spring 4.1 起，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see #setScheduler
 * @see #setSchedulerName
 */
public class SchedulerAccessorBean extends SchedulerAccessor implements BeanFactoryAware, InitializingBean {

	@Nullable
	private String schedulerName;

	@Nullable
	private Scheduler scheduler;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 通过 Spring 应用上下文或 Quartz {@link org.quartz.impl.SchedulerRepository}
	 * 中的调度器名称，指定要操作的 Quartz {@link Scheduler}。
	 * <p>调度器可以通过自定义引导注册到仓库中，
	 * 例如通过 {@link org.quartz.impl.StdSchedulerFactory} 或
	 * {@link org.quartz.impl.DirectSchedulerFactory} 工厂类。
	 * 但通常更推荐使用 Spring 的 {@link SchedulerFactoryBean}，
	 * 它包含了此访问器的作业/触发器/监听器功能。
	 * <p>如果未指定，此访问器将尝试从包含的应用上下文中获取默认的 {@link Scheduler} bean。
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * 指定要操作的 Quartz {@link Scheduler} 实例。
	 * <p>如果未指定，此访问器将尝试从包含的应用上下文中获取默认的 {@link Scheduler} bean。
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * 返回此访问器所操作的 Quartz Scheduler 实例。
	 */
	@Override
	public Scheduler getScheduler() {
		Assert.state(this.scheduler != null, "No Scheduler set");
		return this.scheduler;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterPropertiesSet() throws SchedulerException {
		if (this.scheduler == null) {
			this.scheduler = (this.schedulerName != null ? findScheduler(this.schedulerName) : findDefaultScheduler());
		}
		registerListeners();
		registerJobsAndTriggers();
	}

	protected Scheduler findScheduler(String schedulerName) throws SchedulerException {
		if (this.beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory lbf = (ListableBeanFactory) this.beanFactory;
			String[] beanNames = lbf.getBeanNamesForType(Scheduler.class);
			for (String beanName : beanNames) {
				Scheduler schedulerBean = (Scheduler) lbf.getBean(beanName);
				if (schedulerName.equals(schedulerBean.getSchedulerName())) {
					return schedulerBean;
				}
			}
		}
		Scheduler schedulerInRepo = SchedulerRepository.getInstance().lookup(schedulerName);
		if (schedulerInRepo == null) {
			throw new IllegalStateException("No Scheduler named '" + schedulerName + "' found");
		}
		return schedulerInRepo;
	}

	protected Scheduler findDefaultScheduler() {
		if (this.beanFactory != null) {
			return this.beanFactory.getBean(Scheduler.class);
		}
		else {
			throw new IllegalStateException(
					"No Scheduler specified, and cannot find a default Scheduler without a BeanFactory");
		}
	}

}
