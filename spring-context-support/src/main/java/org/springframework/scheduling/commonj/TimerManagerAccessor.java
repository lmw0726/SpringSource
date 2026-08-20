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

package org.springframework.scheduling.commonj;

import javax.naming.NamingException;

import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 访问 CommonJ {@link commonj.timers.TimerManager} 的类的基类。
 * 定义通用的配置设置和通用的生命周期处理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see commonj.timers.TimerManager
 * @deprecated 从 5.1 版本开始，推荐使用 EE 7 的
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public abstract class TimerManagerAccessor extends JndiLocatorSupport
		implements InitializingBean, DisposableBean, Lifecycle {

	@Nullable
	private TimerManager timerManager;

	@Nullable
	private String timerManagerName;

	private boolean shared = false;


	/**
	 * 指定要委托的 CommonJ TimerManager。
	 * <p>请注意，给定的 TimerManager 的生命周期将由这个 FactoryBean 管理。
	 * <p>或者（通常是这样），您可以指定目标 TimerManager 的 JNDI 名称。
	 * @see #setTimerManagerName
	 */
	public void setTimerManager(TimerManager timerManager) {
		this.timerManager = timerManager;
	}

	/**
	 * 设置 CommonJ TimerManager 的 JNDI 名称。
	 * <p>这可以是完全限定的 JNDI 名称，或者如果 "resourceRef" 设置为 "true"，则是相对于当前环境命名上下文的 JNDI 名称。
	 * @see #setTimerManager
	 * @see #setResourceRef
	 */
	public void setTimerManagerName(String timerManagerName) {
		this.timerManagerName = timerManagerName;
	}

	/**
	 * 指定通过此 FactoryBean 获取的 TimerManager 是共享实例（"true"）还是独立实例（"false"）。
	 * 前者的生命周期应该由应用服务器管理，而后者的生命周期则由应用程序决定。
	 * <p>默认值为 "false"，即管理一个独立的 TimerManager 实例。
	 * 这是 CommonJ 规范建议应用服务器应该通过 JNDI 查找提供的功能，
	 * 通常在 {@code web.xml} 中声明为类型为 {@code commonj.timers.TimerManager} 的 {@code resource-ref}，
	 * 并将 {@code res-sharing-scope} 设置为 'Unshareable'。
	 * <p>如果您获取的是共享的 TimerManager，请将此标志切换为 "true"，
	 * 通常是通过指定已明确声明为 'Shareable' 的 TimerManager 的 JNDI 位置。
	 * 请注意，WebLogic 的集群感知作业调度器也是一个共享的 TimerManager。
	 * <p>此 FactoryBean 在共享模式和非共享模式之间的唯一区别是，
	 * 它只会尝试在独立（非共享）实例的情况下挂起/恢复/停止底层 TimerManager。
	 * 这仅影响 {@link org.springframework.context.Lifecycle} 支持以及应用程序上下文关闭。
	 * @see #stop()
	 * @see #start()
	 * @see #destroy()
	 * @see commonj.timers.TimerManager
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.timerManager == null) {
			if (this.timerManagerName == null) {
				throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
			}
			this.timerManager = lookup(this.timerManagerName, TimerManager.class);
		}
	}

	/**
	 * 返回配置的 TimerManager（如果有的话）。
	 * @return TimerManager，如果不可用则返回 {@code null}
	 */
	@Nullable
	protected final TimerManager getTimerManager() {
		return this.timerManager;
	}

	/**
	 * 获取 TimerManager 以供实际使用。
	 * @return TimerManager（永远不会为 {@code null}）
	 * @throws IllegalStateException 如果未设置 TimerManager
	 * @since 5.0
	 */
	protected TimerManager obtainTimerManager() {
		Assert.notNull(this.timerManager, "No TimerManager set");
		return this.timerManager;
	}


	//---------------------------------------------------------------------
	// Lifecycle 接口的实现
	//---------------------------------------------------------------------

	/**
	 * 恢复底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#resume()
	 */
	@Override
	public void start() {
		if (!this.shared) {
			obtainTimerManager().resume();
		}
	}

	/**
	 * 挂起底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#suspend()
	 */
	@Override
	public void stop() {
		if (!this.shared) {
			obtainTimerManager().suspend();
		}
	}

	/**
	 * 如果底层的 TimerManager 既没有挂起也没有停止，则认为其正在运行。
	 * @see commonj.timers.TimerManager#isSuspending()
	 * @see commonj.timers.TimerManager#isStopping()
	 */
	@Override
	public boolean isRunning() {
		TimerManager tm = obtainTimerManager();
		return (!tm.isSuspending() && !tm.isStopping());
	}


	//---------------------------------------------------------------------
	// DisposableBean 接口的实现
	//---------------------------------------------------------------------

	/**
	 * 停止底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#stop()
	 */
	@Override
	public void destroy() {
		// 必要时停止整个 TimerManager。
		if (this.timerManager != null && !this.shared) {
			// 可能会提前返回，但至少我们已经取消了所有已知的定时器。
			this.timerManager.stop();
		}
	}

}
