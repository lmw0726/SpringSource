/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.support;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 从 AOP Alliance {@link org.aopalliance.intercept.MethodInvocation} 创建 {@link RemoteInvocation} 的策略接口。
 *
 * <p>被 {@link org.springframework.remoting.rmi.RmiClientInterceptor}（用于 RMI 调用器）
 * 和 {@link org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor} 使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultRemoteInvocationFactory
 * @see org.springframework.remoting.rmi.RmiClientInterceptor#setRemoteInvocationFactory
 * @see org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor#setRemoteInvocationFactory
 */
public interface RemoteInvocationFactory {

	/**
	 * 从给定的 AOP MethodInvocation 创建一个可序列化的 RemoteInvocation 对象。
	 * <p>可以实现来向远程调用添加自定义上下文信息，例如用户凭证。
	 * @param methodInvocation 原始的 AOP MethodInvocation 对象
	 * @return RemoteInvocation 对象
	 */
	RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation);

}
