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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Quartz {@link ThreadPool} 适配器，委托给 Spring 管理的
 * {@link Executor} 实例，该实例在 {@link SchedulerFactoryBean} 上指定。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulerFactoryBean#setTaskExecutor
 */
public class LocalTaskExecutorThreadPool implements ThreadPool {


	/** 供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Executor taskExecutor;


	@Override
	public void setInstanceId(String schedInstId) {
	}

	@Override
	public void setInstanceName(String schedName) {
	}


	@Override
	public void initialize() throws SchedulerConfigException {
		// 初始化时必须使用线程绑定的 Executor。
		this.taskExecutor = SchedulerFactoryBean.getConfigTimeTaskExecutor();
		if (this.taskExecutor == null) {
			throw new SchedulerConfigException("No local Executor found for configuration - " +
					"'taskExecutor' property must be set on SchedulerFactoryBean");
		}
	}

	@Override
	public void shutdown(boolean waitForJobsToComplete) {
	}

	@Override
	public int getPoolSize() {
		return -1;
	}


	@Override
	public boolean runInThread(Runnable runnable) {
		Assert.state(this.taskExecutor != null, "No TaskExecutor available");
		try {
			this.taskExecutor.execute(runnable);
			return true;
		}
		catch (RejectedExecutionException ex) {
			logger.error("Task has been rejected by TaskExecutor", ex);
			return false;
		}
	}

	@Override
	public int blockForAvailableThreads() {
		// 当前实现始终返回 1，使 Quartz
		// 可以调度它想要调度的任何任务。
		// 对于特定的 TaskExecutors，可以做出更智能的处理，
		// 例如在 {@code java.util.concurrent.ThreadPoolExecutor} 上
		// 调用 {@code getMaximumPoolSize() - getActiveCount()}。
		return 1;
	}

}
