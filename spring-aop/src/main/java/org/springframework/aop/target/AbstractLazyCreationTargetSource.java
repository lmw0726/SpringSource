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

package org.springframework.aop.target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.aop.TargetSource} 的实现，
 * 将延迟创建用户管理的对象。
 *
 * <p>延迟目标对象的创建由用户通过实现 {@link #createObject()} 方法来控制。
 * 此 {@code TargetSource} 将在代理首次被访问时调用该方法。
 *
 * <p>适用于需要将对某个依赖的引用传递给一个对象，
 * 但实际上不希望该依赖在首次使用之前被创建的场景。
 * 一个典型的使用场景是连接到远程资源。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see #isInitialized()
 * @see #createObject()
 */
public abstract class AbstractLazyCreationTargetSource implements TargetSource {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 延迟初始化的目标对象。 */
	private Object lazyTarget;


	/**
	 * 返回此 TargetSource 的延迟目标对象是否已经被获取过。
	 */
	public synchronized boolean isInitialized() {
		return (this.lazyTarget != null);
	}

	/**
	 * 此默认实现在目标对象为 {@code null}（尚未初始化）时返回 {@code null}，
	 * 或者在目标对象已经初始化后返回目标对象的类。
	 * <p>子类可能希望重写此方法，以便在目标对象仍然为 {@code null} 时提供一个有意义的值。
	 * @see #isInitialized()
	 */
	@Override
	@Nullable
	public synchronized Class<?> getTargetClass() {
		return (this.lazyTarget != null ? this.lazyTarget.getClass() : null);
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	/**
	 * 返回延迟初始化的目标对象，如果尚未存在则即时创建。
	 * @see #createObject()
	 */
	@Override
	public synchronized Object getTarget() throws Exception {
		if (this.lazyTarget == null) {
			logger.debug("Initializing lazy target object");
			this.lazyTarget = createObject();
		}
		return this.lazyTarget;
	}

	@Override
	public void releaseTarget(Object target) throws Exception {
		// 无需执行任何操作
	}


	/**
	 * 子类应实现此方法以返回延迟初始化的对象。
	 * 在代理首次被调用时调用。
	 * @return 创建的对象
	 * @throws Exception 如果创建失败
	 */
	protected abstract Object createObject() throws Exception;

}
