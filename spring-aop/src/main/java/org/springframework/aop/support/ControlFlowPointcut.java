/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于简单 <b>cflow</b> 风格切点的切点和方法匹配器。
 * 注意，评估此类切点比评估普通切点慢 10 到 15 倍，
 * 但在某些情况下它们很有用。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

	private final Class<?> clazz;

	@Nullable
	private final String methodName;

	private final AtomicInteger evaluations = new AtomicInteger();


	/**
	 * 构造一个新的切点，用于匹配该类之下的所有控制流。
	 * @param clazz 类
	 */
	public ControlFlowPointcut(Class<?> clazz) {
		this(clazz, null);
	}

	/**
	 * 构造一个新的切点，用于匹配给定类中给定方法之下的所有调用。
	 * 如果未给出方法名，则匹配给定类之下的所有控制流。
	 * @param clazz 类
	 * @param methodName 方法名称（可以为 {@code null}）
	 */
	public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
		this.methodName = methodName;
	}


	/**
	 * 子类可以重写此方法以获得更强的过滤能力（以及性能）。
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * 如果可以过滤掉某些候选类，子类可以重写此方法。
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean isRuntime() {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		this.evaluations.incrementAndGet();

		for (StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.getClassName().equals(this.clazz.getName()) &&
					(this.methodName == null || element.getMethodName().equals(this.methodName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 了解已经触发了多少次对优化很有用。
	 */
	public int getEvaluations() {
		return this.evaluations.get();
	}


	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControlFlowPointcut)) {
			return false;
		}
		ControlFlowPointcut that = (ControlFlowPointcut) other;
		return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(this.methodName, that.methodName);
	}

	@Override
	public int hashCode() {
		int code = this.clazz.hashCode();
		if (this.methodName != null) {
			code = 37 * code + this.methodName.hashCode();
		}
		return code;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": class = " + this.clazz.getName() + "; methodName = " + this.methodName;
	}

}
