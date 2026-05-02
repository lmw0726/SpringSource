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
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 当没有目标对象（或仅知道目标类），且行为仅由接口和通知器提供时的
 * 规范 {@code TargetSource}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public final class EmptyTargetSource implements TargetSource, Serializable {

	/** 使用 Spring 1.2 的 serialVersionUID 以保证互操作性。 */
	private static final long serialVersionUID = 3680494563553489691L;


	//---------------------------------------------------------------------
	// 静态工厂方法
	//---------------------------------------------------------------------

	/**
	 * 此 {@link EmptyTargetSource} 的规范（单例）实例。
	 */
	public static final EmptyTargetSource INSTANCE = new EmptyTargetSource(null, true);


	/**
	 * 返回给定目标类的 EmptyTargetSource。
	 * @param targetClass 目标类（可以为 {@code null}）
	 * @see #getTargetClass()
	 */
	public static EmptyTargetSource forClass(@Nullable Class<?> targetClass) {
		return forClass(targetClass, true);
	}

	/**
	 * 返回给定目标类的 EmptyTargetSource。
	 * @param targetClass 目标类（可以为 {@code null}）
	 * @param isStatic TargetSource 是否应标记为静态
	 * @see #getTargetClass()
	 */
	public static EmptyTargetSource forClass(@Nullable Class<?> targetClass, boolean isStatic) {
		return (targetClass == null && isStatic ? INSTANCE : new EmptyTargetSource(targetClass, isStatic));
	}


	//---------------------------------------------------------------------
	// 实例实现
	//---------------------------------------------------------------------

	private final Class<?> targetClass;

	private final boolean isStatic;


	/**
	 * 创建 {@link EmptyTargetSource} 类的新实例。
	 * <p>此构造函数是 {@code private} 的，以强制使用单例模式/工厂方法模式。
	 * @param targetClass 要暴露的目标类（可以为 {@code null}）
	 * @param isStatic TargetSource 是否标记为静态
	 */
	private EmptyTargetSource(@Nullable Class<?> targetClass, boolean isStatic) {
		this.targetClass = targetClass;
		this.isStatic = isStatic;
	}


	/**
	 * 始终返回指定的目标类，如果没有则返回 {@code null}。
	 */
	@Override
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 始终返回 {@code true}。
	 */
	@Override
	public boolean isStatic() {
		return this.isStatic;
	}

	/**
	 * 始终返回 {@code null}。
	 */
	@Override
	@Nullable
	public Object getTarget() {
		return null;
	}

	/**
	 * 无需释放。
	 */
	@Override
	public void releaseTarget(Object target) {
	}


	/**
	 * 在没有目标类的情况下，反序列化时返回规范实例，从而保护单例模式。
	 */
	private Object readResolve() {
		return (this.targetClass == null && this.isStatic ? INSTANCE : this);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EmptyTargetSource)) {
			return false;
		}
		EmptyTargetSource otherTs = (EmptyTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.targetClass, otherTs.targetClass) && this.isStatic == otherTs.isStatic);
	}

	@Override
	public int hashCode() {
		return EmptyTargetSource.class.hashCode() * 13 + ObjectUtils.nullSafeHashCode(this.targetClass);
	}

	@Override
	public String toString() {
		return "EmptyTargetSource: " +
				(this.targetClass != null ? "target class [" + this.targetClass.getName() + "]" : "no target class") +
				", " + (this.isStatic ? "static" : "dynamic");
	}

}
