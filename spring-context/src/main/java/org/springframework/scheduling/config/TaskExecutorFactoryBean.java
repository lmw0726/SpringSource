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

package org.springframework.scheduling.config;

import java.util.concurrent.RejectedExecutionHandler;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * 用于创建 {@link ThreadPoolTaskExecutor} 实例的 {@link FactoryBean}，
 * 主要在 XML task 命名空间背后使用。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TaskExecutorFactoryBean implements
		FactoryBean<TaskExecutor>, BeanNameAware, InitializingBean, DisposableBean {

	@Nullable
	private String poolSize;

	@Nullable
	private Integer queueCapacity;

	@Nullable
	private RejectedExecutionHandler rejectedExecutionHandler;

	@Nullable
	private Integer keepAliveSeconds;

	@Nullable
	private String beanName;

	@Nullable
	private ThreadPoolTaskExecutor target;


	public void setPoolSize(String poolSize) {
		this.poolSize = poolSize;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler = rejectedExecutionHandler;
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		determinePoolSizeRange(executor);
		if (this.queueCapacity != null) {
			executor.setQueueCapacity(this.queueCapacity);
		}
		if (this.keepAliveSeconds != null) {
			executor.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.rejectedExecutionHandler != null) {
			executor.setRejectedExecutionHandler(this.rejectedExecutionHandler);
		}
		if (this.beanName != null) {
			executor.setThreadNamePrefix(this.beanName + "-");
		}
		executor.afterPropertiesSet();
		this.target = executor;
	}

	private void determinePoolSizeRange(ThreadPoolTaskExecutor executor) {
		if (StringUtils.hasText(this.poolSize)) {
			try {
				int corePoolSize;
				int maxPoolSize;
				int separatorIndex = this.poolSize.indexOf('-');
				if (separatorIndex != -1) {
					corePoolSize = Integer.parseInt(this.poolSize.substring(0, separatorIndex));
					maxPoolSize = Integer.parseInt(this.poolSize.substring(separatorIndex + 1));
					if (corePoolSize > maxPoolSize) {
						throw new IllegalArgumentException(
								"Lower bound of pool-size range must not exceed the upper bound");
					}
					if (this.queueCapacity == null) {
						// 未提供 queue-capacity，因此是无界的
						if (corePoolSize == 0) {
							// 实际将 'corePoolSize' 设置为范围的上限
							// 但允许核心线程超时...
							executor.setAllowCoreThreadTimeOut(true);
							corePoolSize = maxPoolSize;
						}
						else {
							// 非零下限意味着存在核心-最大大小范围...
							throw new IllegalArgumentException(
									"A non-zero lower bound for the size range requires a queue-capacity value");
						}
					}
				}
				else {
					int value = Integer.parseInt(this.poolSize);
					corePoolSize = value;
					maxPoolSize = value;
				}
				executor.setCorePoolSize(corePoolSize);
				executor.setMaxPoolSize(maxPoolSize);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid pool-size value [" + this.poolSize + "]: only single " +
						"maximum integer (e.g. \"5\") and minimum-maximum range (e.g. \"3-5\") are supported", ex);
			}
		}
	}


	@Override
	@Nullable
	public TaskExecutor getObject() {
		return this.target;
	}

	@Override
	public Class<? extends TaskExecutor> getObjectType() {
		return (this.target != null ? this.target.getClass() : ThreadPoolTaskExecutor.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.target != null) {
			this.target.destroy();
		}
	}

}
