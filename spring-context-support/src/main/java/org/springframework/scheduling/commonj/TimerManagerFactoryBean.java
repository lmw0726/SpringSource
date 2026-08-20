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

package org.springframework.scheduling.commonj;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import commonj.timers.Timer;
import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.FactoryBean} 用于获取
 * CommonJ {@link commonj.timers.TimerManager} 并将其暴露为 Bean 引用。
 *
 * <p><b>这是在 Spring 上下文中设置
 * CommonJ TimerManager 的核心便捷类。</b>
 *
 * <p>允许注册 ScheduledTimerListener。这是该类的主要目的；
 * TimerManager 本身也可以通过
 * {@link org.springframework.jndi.JndiObjectFactoryBean} 从 JNDI 获取。
 * 在仅需要在启动时静态注册任务的场景中，
 * 应用代码无需直接访问 TimerManager 本身。
 *
 * <p>请注意，TimerManager 使用的 TimerListener 实例
 * 在重复执行之间是共享的，这与 Quartz 为每次执行
 * 创建新 Job 实例的方式不同。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ScheduledTimerListener
 * @see commonj.timers.TimerManager
 * @see commonj.timers.TimerListener
 * @deprecated 从 5.1 版本起已弃用，建议使用 EE 7 的
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public class TimerManagerFactoryBean extends TimerManagerAccessor
		implements FactoryBean<TimerManager>, InitializingBean, DisposableBean, Lifecycle {

	@Nullable
	private ScheduledTimerListener[] scheduledTimerListeners;

	@Nullable
	private List<Timer> timers;


	/**
	 * 向该 FactoryBean 创建的 TimerManager 注册 ScheduledTimerListener 对象列表。
	 * 根据每个 ScheduledTimerListener 的设置，
	 * 将通过 TimerManager 的某个 schedule 方法进行注册。
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long)
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
	 */
	public void setScheduledTimerListeners(ScheduledTimerListener[] scheduledTimerListeners) {
		this.scheduledTimerListeners = scheduledTimerListeners;
	}


	//---------------------------------------------------------------------
	// InitializingBean 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();

		if (this.scheduledTimerListeners != null) {
			this.timers = new ArrayList<>(this.scheduledTimerListeners.length);
			TimerManager timerManager = obtainTimerManager();
			for (ScheduledTimerListener scheduledTask : this.scheduledTimerListeners) {
				Timer timer;
				if (scheduledTask.isOneTimeTask()) {
					timer = timerManager.schedule(scheduledTask.getTimerListener(), scheduledTask.getDelay());
				}
				else {
					if (scheduledTask.isFixedRate()) {
						timer = timerManager.scheduleAtFixedRate(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
					else {
						timer = timerManager.schedule(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
				}
				this.timers.add(timer);
			}
		}
	}


	//---------------------------------------------------------------------
	// FactoryBean 接口的实现
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public TimerManager getObject() {
		return getTimerManager();
	}

	@Override
	public Class<? extends TimerManager> getObjectType() {
		TimerManager timerManager = getTimerManager();
		return (timerManager != null ? timerManager.getClass() : TimerManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// DisposableBean 接口的实现
	//---------------------------------------------------------------------

	/**
	 * 在关闭时取消所有静态注册的 Timer，
	 * 并停止底层的 TimerManager（如果未被共享）。
	 * @see commonj.timers.Timer#cancel()
	 * @see commonj.timers.TimerManager#stop()
	 */
	@Override
	public void destroy() {
		// 取消所有已注册的定时器。
		if (this.timers != null) {
			for (Timer timer : this.timers) {
				try {
					timer.cancel();
				}
				catch (Throwable ex) {
					logger.debug("Could not cancel CommonJ Timer", ex);
				}
			}
			this.timers.clear();
		}

		// 停止 TimerManager 本身。
		super.destroy();
	}

}
