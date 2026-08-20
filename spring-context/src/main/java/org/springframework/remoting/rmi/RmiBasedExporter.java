/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;

/**
 * 基于 RMI 的远程导出器的便捷超类。提供了将给定的普通 Java 服务对象
 * 自动包装为 RmiInvocationWrapper 的功能，从而暴露 {@link RmiInvocationHandler} 远程接口。
 *
 * <p>使用 RMI 调用器机制，RMI 通信在 {@link RmiInvocationHandler} 层面进行，
 * 所有服务共享同一个通用的调用器存根。服务接口<i>不需要</i>继承
 * {@code java.rmi.Remote}，也不需要在所有服务方法上声明
 * {@code java.rmi.RemoteException}。但输入和输出参数仍然必须是可序列化的。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see RmiServiceExporter
 * @see JndiRmiServiceExporter
 * @deprecated 自 5.3 版本起已弃用（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public abstract class RmiBasedExporter extends RemoteInvocationBasedExporter {

	/**
	 * 确定要导出的对象：如果服务对象本身是 RMI 服务，则返回服务对象本身；
	 * 如果是普通的非 RMI 服务对象，则返回一个 RmiInvocationWrapper。
	 * @return 要导出的 RMI 对象
	 * @see #setService
	 * @see #setServiceInterface
	 */
	protected Remote getObjectToExport() {
		// 确定远程对象
		if (getService() instanceof Remote &&
				(getServiceInterface() == null || Remote.class.isAssignableFrom(getServiceInterface()))) {
			// 传统的 RMI 服务
			return (Remote) getService();
		}
		else {
			// RMI 调用器
			if (logger.isDebugEnabled()) {
				logger.debug("RMI service [" + getService() + "] is an RMI invoker");
			}
			return new RmiInvocationWrapper(getProxyForService(), this);
		}
	}

	/**
	 * 在此重新定义以使 RmiInvocationWrapper 可以访问。
	 * 只是委托给相应的父类方法。
	 */
	@Override
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return super.invoke(invocation, targetObject);
	}

}
