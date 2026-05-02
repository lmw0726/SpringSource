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

package org.springframework.aop.target.dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * 抽象的 {@link org.springframework.aop.TargetSource} 实现，
 * 包装一个可刷新的目标对象。子类可以确定是否需要刷新，
 * 并需要提供新的目标对象。
 *
 * <p>实现了 {@link Refreshable} 接口，以便允许对刷新状态进行显式控制。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see #requiresRefresh()
 * @see #freshTarget()
 */
public abstract class AbstractRefreshableTargetSource implements TargetSource, Refreshable {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	protected Object targetObject;

	private long refreshCheckDelay = -1;

	private long lastRefreshCheck = -1;

	private long lastRefreshTime = -1;

	private long refreshCount = 0;


	/**
	 * 设置刷新检查之间的延迟时间（毫秒）。
	 * 默认值为 -1，表示完全不进行刷新检查。
	 * <p>注意，只有当 {@link #requiresRefresh()} 返回 {@code true} 时，
	 * 才会实际执行刷新。
	 */
	public void setRefreshCheckDelay(long refreshCheckDelay) {
		this.refreshCheckDelay = refreshCheckDelay;
	}


	@Override
	public synchronized Class<?> getTargetClass() {
		if (this.targetObject == null) {
			refresh();
		}
		return this.targetObject.getClass();
	}

	/**
	 * 非静态。
	 */
	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	@Nullable
	public final synchronized Object getTarget() {
		if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
			refresh();
		}
		return this.targetObject;
	}

	/**
	 * 无需释放目标对象。
	 */
	@Override
	public void releaseTarget(Object object) {
	}


	@Override
	public final synchronized void refresh() {
		logger.debug("Attempting to refresh target");

		this.targetObject = freshTarget();
		this.refreshCount++;
		this.lastRefreshTime = System.currentTimeMillis();

		logger.debug("Target refreshed successfully");
	}

	@Override
	public synchronized long getRefreshCount() {
		return this.refreshCount;
	}

	@Override
	public synchronized long getLastRefreshTime() {
		return this.lastRefreshTime;
	}


	private boolean refreshCheckDelayElapsed() {
		if (this.refreshCheckDelay < 0) {
			return false;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (this.lastRefreshCheck < 0 || currentTimeMillis - this.lastRefreshCheck > this.refreshCheckDelay) {
			// 即将执行刷新检查 - 更新时间戳。
			this.lastRefreshCheck = currentTimeMillis;
			logger.debug("Refresh check delay elapsed - checking whether refresh is required");
			return true;
		}

		return false;
	}


	/**
	 * 确定是否需要刷新。
	 * 在每次刷新检查时调用，前提是刷新检查延迟已经过去。
	 * <p>默认实现始终返回 {@code true}，意味着每次延迟过后都会触发刷新。
	 * 子类应重写此方法，以便对底层目标资源进行适当的检查。
	 * @return 是否需要刷新
	 */
	protected boolean requiresRefresh() {
		return true;
	}

	/**
	 * 获取一个全新的目标对象。
	 * <p>仅在刷新检查发现需要刷新时调用（即 {@link #requiresRefresh()} 返回 {@code true}）。
	 * @return 全新的目标对象
	 */
	protected abstract Object freshTarget();

}
