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

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * {@link org.springframework.aop.TargetSource} 的实现，
 * 缓存本地的目标对象，但允许在应用程序运行期间替换目标对象。
 *
 * <p>如果在 Spring IoC 容器中配置此类的对象，请使用构造函数注入。
 *
 * <p>如果在序列化时目标对象是可序列化的，则此 TargetSource 也是可序列化的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class HotSwappableTargetSource implements TargetSource, Serializable {

	/** 使用 Spring 1.2 的 serialVersionUID 以保证互操作性。 */
	private static final long serialVersionUID = 7497929212653839187L;


	/** 当前的目标对象。 */
	private Object target;


	/**
	 * 使用给定的初始目标对象创建新的 HotSwappableTargetSource。
	 * @param initialTarget 初始目标对象
	 */
	public HotSwappableTargetSource(Object initialTarget) {
		Assert.notNull(initialTarget, "Target object must not be null");
		this.target = initialTarget;
	}


	/**
	 * 返回当前目标对象的类型。
	 * <p>返回的类型通常在所有目标对象间保持不变。
	 */
	@Override
	public synchronized Class<?> getTargetClass() {
		return this.target.getClass();
	}

	@Override
	public final boolean isStatic() {
		return false;
	}

	@Override
	public synchronized Object getTarget() {
		return this.target;
	}

	@Override
	public void releaseTarget(Object target) {
		//没什么可做的
	}


	/**
	 * 替换目标对象，返回旧的目标对象。
	 * @param newTarget 新的目标对象
	 * @return 旧的目标对象
	 * @throws IllegalArgumentException 如果新的目标对象无效
	 */
	public synchronized Object swap(Object newTarget) throws IllegalArgumentException {
		Assert.notNull(newTarget, "Target object must not be null");
		Object old = this.target;
		this.target = newTarget;
		return old;
	}


	/**
	 * 如果当前目标对象相等，则两个 HotSwappableTargetSource 相等。
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof HotSwappableTargetSource &&
				this.target.equals(((HotSwappableTargetSource) other).target)));
	}

	@Override
	public int hashCode() {
		return HotSwappableTargetSource.class.hashCode();
	}

	@Override
	public String toString() {
		return "HotSwappableTargetSource for target: " + this.target;
	}

}
