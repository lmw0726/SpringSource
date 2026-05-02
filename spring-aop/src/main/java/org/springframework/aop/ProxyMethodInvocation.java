/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * AOP Alliance {@link org.aopalliance.intercept.MethodInvocation} 接口的扩展，
 * 允许访问本次方法调用所经过的代理。
 *
 * <p>在必要时可用于将返回值替换为代理，例如调用目标返回自身时。
 *
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 1.1.3
 * @see org.springframework.aop.framework.ReflectiveMethodInvocation
 * @see org.springframework.aop.support.DelegatingIntroductionInterceptor
 */
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * 返回本次方法调用所经过的代理。
	 * @return 原始代理对象
	 */
	Object getProxy();

	/**
	 * 创建此对象的克隆。如果在此对象上调用 {@code proceed()} 之前完成克隆，
	 * 则可以在每个克隆上调用一次 {@code proceed()}，
	 * 从而多次调用连接点（以及通知链的其余部分）。
	 * @return 此调用的可调用克隆。
	 * 每个克隆可以调用一次 {@code proceed()}。
	 */
	MethodInvocation invocableClone();

	/**
	 * 创建此对象的克隆。如果在此对象上调用 {@code proceed()} 之前完成克隆，
	 * 则可以在每个克隆上调用一次 {@code proceed()}，
	 * 从而多次调用连接点（以及通知链的其余部分）。
	 * @param arguments 克隆后的调用应使用的参数，
	 * 覆盖原始参数
	 * @return 此调用的可调用克隆。
	 * 每个克隆可以调用一次 {@code proceed()}。
	 */
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * 设置此链中任意通知在后续调用中要使用的参数。
	 * @param arguments 参数数组
	 */
	void setArguments(Object... arguments);

	/**
	 * 将指定用户属性及其给定值添加到此调用中。
	 * <p>这些属性不会在 AOP 框架自身内部使用。它们只是作为调用对象的一部分保留，
	 * 供特殊拦截器使用。
	 * @param key 属性名称
	 * @param value 属性值，或 {@code null} 表示重置该属性
	 */
	void setUserAttribute(String key, @Nullable Object value);

	/**
	 * 返回指定用户属性的值。
	 * @param key 属性名称
	 * @return 属性值；如果未设置，则返回 {@code null}
	 * @see #setUserAttribute
	 */
	@Nullable
	Object getUserAttribute(String key);

}
