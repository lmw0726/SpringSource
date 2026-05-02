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

import java.io.Serializable;

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.aop.TargetSource} 接口的实现，
 * 持有给定的对象。这是 TargetSource 接口的默认实现，
 * 由 Spring AOP 框架使用。通常不需要在应用程序代码中
 * 创建此类的对象。
 *
 * <p>此类是可序列化的。然而，SingletonTargetSource 的实际可序列化性
 * 取决于其目标对象是否可序列化。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AdvisedSupport#setTarget(Object)
 */
public class SingletonTargetSource implements TargetSource, Serializable {

	/** 使用 Spring 1.2 的 serialVersionUID 以保证互操作性。 */
	private static final long serialVersionUID = 9031246629662423738L;


	/** 被缓存并通过反射调用的目标对象。 */
	private final Object target;


	/**
	 * 为给定的目标对象创建一个新的 SingletonTargetSource。
	 * @param target 目标对象
	 */
	public SingletonTargetSource(Object target) {
		Assert.notNull(target, "Target object must not be null");
		this.target = target;
	}


	@Override
	public Class<?> getTargetClass() {
		return this.target.getClass();
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public void releaseTarget(Object target) {
		// nothing to do
	}

	@Override
	public boolean isStatic() {
		return true;
	}


	/**
	 * 如果两个调用拦截器具有相同的目标对象，或者目标对象本身相等，则它们相等。
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SingletonTargetSource)) {
			return false;
		}
		SingletonTargetSource otherTargetSource = (SingletonTargetSource) other;
		return this.target.equals(otherTargetSource.target);
	}

	/**
	 * SingletonTargetSource 使用目标对象的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.target.hashCode();
	}

	@Override
	public String toString() {
		return "SingletonTargetSource for target object [" + ObjectUtils.identityToString(this.target) + "]";
	}

}
