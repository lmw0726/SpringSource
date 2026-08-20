/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * RMI 代理的 {@link FactoryBean}，支持传统 RMI 服务和 RMI 调用器（invoker）。
 * 将代理后的服务暴露为 bean 引用，使用指定的服务接口。
 * 代理在远程调用失败时将抛出 Spring 的非受检异常 RemoteAccessException，
 * 而非 RMI 的 RemoteException。
 *
 * <p>服务 URL 必须是有效的 RMI URL，例如 "rmi://localhost:1099/myservice"。
 * RMI 调用器在 RmiInvocationHandler 层级工作，对任何服务使用相同的调用器存根（stub）。
 * 服务接口不必继承 {@code java.rmi.Remote} 或抛出 {@code java.rmi.RemoteException}。
 * 当然，入参和出参必须是可序列化的。
 *
 * <p>对于传统 RMI 服务，此代理工厂通常与 RMI 服务接口配合使用。
 * 此外，此工厂也可以使用匹配的非 RMI 业务接口来代理远程 RMI 服务，
 * 即一个镜像了 RMI 服务方法但不声明 RemoteException 的接口。
 * 在后一种情况下，RMI 存根抛出的 RemoteException 将自动转换为
 * Spring 的非受检异常 RemoteAccessException。
 *
 * <p>与 Hessian 相比，RMI 的主要优势在于序列化。
 * 实际上，任何可序列化的 Java 对象都可以毫无障碍地传输。
 * Hessian 有自己的（反）序列化机制，但基于 HTTP，因此比 RMI 更容易设置。
 * 或者，可以考虑使用 Spring 的 HTTP 调用器，将 Java 序列化与基于 HTTP 的传输相结合。
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see RmiClientInterceptor
 * @see RmiServiceExporter
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see org.springframework.remoting.RemoteAccessException
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @see org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean
 * @deprecated 从 5.3 版本起（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class RmiProxyFactoryBean extends RmiClientInterceptor implements FactoryBean<Object>, BeanClassLoaderAware {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");
		this.serviceProxy = new ProxyFactory(ifc, this).getProxy(getBeanClassLoader());
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
