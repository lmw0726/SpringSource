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

import javax.naming.NamingException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于从 JNDI 获取 RMI 代理的 {@link FactoryBean}。
 *
 * <p>通常用于 RMI-IIOP（CORBA），但也可用于 EJB home 对象（例如有状态 Session Bean 的 home）。
 * 与简单的 JNDI 查找相比，此访问器还会通过 {@link javax.rmi.PortableRemoteObject} 执行 narrowing 操作。
 *
 * <p>对于传统的 RMI 服务，此调用器通常与 RMI 服务接口一起使用。或者，此调用器也可以用匹配的
 * 非 RMI 业务接口来代理远程 RMI 服务，即一个镜像了 RMI 服务方法但不声明 RemoteException 的接口。
 * 在后一种情况下，RMI 存根抛出的 RemoteException 将自动转换为
 * Spring 的非受检异常 RemoteAccessException。
 *
 * <p>JNDI 环境可以通过 "jndiEnvironment" 属性指定，
 * 也可以在 {@code jndi.properties} 文件或系统属性中配置。
 * 例如：
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props&gt;
 *		&lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		&lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceInterface
 * @see #setJndiName
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setJndiName
 * @see JndiRmiServiceExporter
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 * @see java.rmi.Remote
 * @see javax.rmi.PortableRemoteObject#narrow
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class JndiRmiProxyFactoryBean extends JndiRmiClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");
		this.serviceProxy = new ProxyFactory(ifc, this).getProxy(this.beanClassLoader);
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
