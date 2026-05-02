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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * 监控拦截器的基类，例如性能监控器。
 * 提供可配置的 "prefix" 和 "suffix" 属性，用于帮助
 * 分类/分组性能监控结果。
 *
 * <p>在它们的 {@link #invokeUnderTrace} 实现中，子类应该调用
 * {@link #createInvocationTraceName} 方法来为给定的跟踪创建名称，
 * 包括关于方法调用的信息以及前缀/后缀。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see #setPrefix
 * @see #setSuffix
 * @see #createInvocationTraceName
 */
@SuppressWarnings("serial")
public abstract class AbstractMonitoringInterceptor extends AbstractTraceInterceptor {

	private String prefix = "";

	private String suffix = "";

	private boolean logTargetClassInvocation = false;


	/**
	 * 设置将附加到跟踪数据的文本。
	 * <p>默认为无。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回将附加到跟踪数据的文本。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置将添加到跟踪数据前面的文本。
	 * <p>默认为无。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回将添加到跟踪数据前面的文本。
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置是否在目标类上记录调用（如果适用）。
	 * （即如果方法实际上委托给目标类）。
	 * <p>默认为 "false"，基于代理接口/类名称记录调用。
	 */
	public void setLogTargetClassInvocation(boolean logTargetClassInvocation) {
		this.logTargetClassInvocation = logTargetClassInvocation;
	}


	/**
	 * 为给定的 {@code MethodInvocation} 创建一个 {@code String} 名称，
	 * 该名称可用于跟踪/日志记录目的。此名称由配置的前缀、
	 * 后跟被调用方法的完全限定名称、再后跟配置的后缀组成。
	 * @see #setPrefix
	 * @see #setSuffix
	 */
	protected String createInvocationTraceName(MethodInvocation invocation) {
		Method method = invocation.getMethod();
		Class<?> clazz = method.getDeclaringClass();
		if (this.logTargetClassInvocation && clazz.isInstance(invocation.getThis())) {
			clazz = invocation.getThis().getClass();
		}
		String className = clazz.getName();
		return getPrefix() + className + '.' + method.getName() + getSuffix();
	}

}
