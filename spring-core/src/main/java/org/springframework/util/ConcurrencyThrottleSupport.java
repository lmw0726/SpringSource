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

package org.springframework.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * 支持对特定资源的并发访问进行节流控制的支持类。
 *
 * <p>设计用作基类，子类在其工作流程的适当点调用
 * {@link #beforeAccess()} 和 {@link #afterAccess()} 方法。
 * 注意 {@code afterAccess} 通常应该在 finally 块中调用！
 *
 * <p>此支持类的默认并发限制为 -1（"无限并发"）。
 * 子类可以重写此默认值；请查看您正在使用的具体类的 javadoc。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #setConcurrencyLimit
 * @see #beforeAccess()
 * @see #afterAccess()
 * @see org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see java.io.Serializable
 */
@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

	/**
	 * 允许任意数量的并发调用：即，不对并发进行节流控制。
	 */
	public static final int UNBOUNDED_CONCURRENCY = -1;

	/**
	 * 关闭并发：即，不允许任何并发调用。
	 */
	public static final int NO_CONCURRENCY = 0;


	/** 使用 transient 以优化序列化。 */
	protected transient Log logger = LogFactory.getLog(getClass());

	private transient Object monitor = new Object();

	private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

	private int concurrencyCount = 0;


	/**
	 * 设置允许的最大并发访问尝试次数。
	 * -1 表示无限并发。
	 * <p>原则上，此限制可以在运行时更改，
	 * 尽管它通常被设计为配置时设置。
	 * <p>注意：不要在运行时在 -1 和任何具体限制之间切换，
	 * 因为这会导致不一致的并发计数：限制为 -1 实际上会完全关闭并发计数。
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyLimit = concurrencyLimit;
	}

	/**
	 * 返回允许的最大并发访问尝试次数。
	 */
	public int getConcurrencyLimit() {
		return this.concurrencyLimit;
	}

	/**
	 * 返回此节流器当前是否处于活动状态。
	 * @return 如果此实例的并发限制处于活动状态则返回 {@code true}
	 * @see #getConcurrencyLimit()
	 */
	public boolean isThrottleActive() {
		return (this.concurrencyLimit >= 0);
	}


	/**
	 * 在具体子类的主要执行逻辑之前调用。
	 * <p>此实现应用并发节流控制。
	 * @see #afterAccess()
	 */
	protected void beforeAccess() {
		// 如果并发限制被设置为“完全不允许并发”
		if (this.concurrencyLimit == NO_CONCURRENCY) {
			// 直接拒绝调用
			throw new IllegalStateException(
					"Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
		}

		// 如果设置了并发限制（>0 表示有限并发）
		if (this.concurrencyLimit > 0) {
			// 是否开启 debug 日志
			boolean debug = logger.isDebugEnabled();

			// 使用 monitor 对象作为锁，保证并发安全
			synchronized (this.monitor) {

				// 标记当前线程是否被中断过
				boolean interrupted = false;

				// 当当前并发数已经达到上限时
				while (this.concurrencyCount >= this.concurrencyLimit) {

					// 如果线程在等待过程中被中断过
					if (interrupted) {
						// 抛出异常，不再继续等待
						throw new IllegalStateException("Thread was interrupted while waiting for invocation access, " +
								"but concurrency limit still does not allow for entering");
					}

					// 打印调试日志：当前并发已达到上限，线程将被阻塞
					if (debug) {
						logger.debug("Concurrency count " + this.concurrencyCount +
								" has reached limit " + this.concurrencyLimit + " - blocking");
					}
					try {
						// 当前线程进入等待状态，释放锁，等待被唤醒
						this.monitor.wait();
					}
					catch (InterruptedException ex) {
						// 如果线程被中断：
						// 重新中断当前线程，以允许其他线程响应。
						Thread.currentThread().interrupt();

						// 标记为已中断（但继续走 while 流程）
						interrupted = true;
					}
				}

				// 如果开启 debug，记录进入限流区域
				if (debug) {
					logger.debug("Entering throttle at concurrency count " + this.concurrencyCount);
				}

				// 并发计数 +1（表示当前线程成功进入执行区）
				this.concurrencyCount++;
			}
		}
	}

	/**
	 * 在具体子类的主要执行逻辑之后调用。
	 * @see #beforeAccess()
	 */
	protected void afterAccess() {
		// 只有在设置了并发限制（>=0）时才需要做释放逻辑
		if (this.concurrencyLimit >= 0) {

			// 使用同一个 monitor 对象加锁，保证并发安全
			synchronized (this.monitor) {

				// 当前并发计数 -1（释放一个“执行名额”）
				this.concurrencyCount--;

				// 如果开启 debug 日志，打印当前并发数
				if (logger.isDebugEnabled()) {
					logger.debug("Returning from throttle at concurrency count " + this.concurrencyCount);
				}

				// 唤醒一个正在 wait() 的线程
				// 👉 让其有机会重新竞争进入执行区
				this.monitor.notify();
			}
		}
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化，仅在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化瞬态字段。
		this.logger = LogFactory.getLog(getClass());
		this.monitor = new Object();
	}

}
