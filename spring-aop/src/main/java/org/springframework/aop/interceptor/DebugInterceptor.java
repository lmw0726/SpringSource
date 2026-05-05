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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * 可以在链中引入的 AOP Alliance {@code MethodInterceptor}，
 * 用于向记录器显示有关被拦截调用的详细信息。
 *
 * <p>在方法进入和方法退出时记录完整的调用详细信息，
 * 包括调用参数和调用次数。这仅用于调试目的；
 * 对于纯跟踪目的，请使用 {@code SimpleTraceInterceptor}
 * 或 {@code CustomizableTraceInterceptor}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleTraceInterceptor
 * @see CustomizableTraceInterceptor
 */
@SuppressWarnings("serial")
public class DebugInterceptor extends SimpleTraceInterceptor {

	private volatile long count;


	/**
	 * 使用静态记录器创建新的 DebugInterceptor。
	 */
	public DebugInterceptor() {
	}

	/**
	 * 根据给定标志使用动态或静态记录器创建新的 DebugInterceptor。
	 * @param useDynamicLogger 是否使用动态记录器或静态记录器
	 * @see #setUseDynamicLogger
	 */
	public DebugInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		synchronized (this) {
			// 计数器+1
			this.count++;
		}
		// 调用父类的处理方法
		return super.invoke(invocation);
	}

	@Override
	protected String getInvocationDescription(MethodInvocation invocation) {
		return invocation + "; count=" + this.count;
	}


	/**
	 * 返回此拦截器被调用的次数。
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * 将调用计数重置为零。
	 */
	public synchronized void resetCount() {
		this.count = 0;
	}

}
