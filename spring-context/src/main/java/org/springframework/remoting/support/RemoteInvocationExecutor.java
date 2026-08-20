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

import java.lang.reflect.InvocationTargetException;

/**
 * 在目标对象上执行 {@link RemoteInvocation} 的策略接口。
 *
 * <p>由 {@link org.springframework.remoting.rmi.RmiServiceExporter}（用于 RMI 调用器）
 * 和 {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter} 使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultRemoteInvocationFactory
 * @see org.springframework.remoting.rmi.RmiServiceExporter#setRemoteInvocationExecutor
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter#setRemoteInvocationExecutor
 */
public interface RemoteInvocationExecutor {

	/**
	 * 在给定的目标对象上执行此调用。
	 * 通常在服务器端收到 RemoteInvocation 时被调用。
	 * @param invocation RemoteInvocation 实例
	 * @param targetObject 要对其应用调用的目标对象
	 * @return 调用结果
	 * @throws NoSuchMethodException 如果无法解析方法名称
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致了异常
	 * @see java.lang.reflect.Method#invoke
	 */
	Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

}
