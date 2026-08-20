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

package org.springframework.ejb.access;

import javax.naming.NamingException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 用于远程 SLSB（Stateless Session Bean）代理的便捷 {@link FactoryBean}。
 * 专为 EJB 2.x 设计，但也适用于 EJB 3 Session Bean。
 *
 * <p>有关如何指定目标 EJB 的 JNDI 位置的信息，请参阅
 * {@link org.springframework.jndi.JndiObjectLocator}。
 *
 * <p>如果需要控制拦截器链，请使用带有 SimpleRemoteSlsbInvokerInterceptor 的
 * AOP ProxyFactoryBean，而不是依赖此类。
 *
 * <p>在 Bean 容器中，此类通常最好作为单例使用。但是，如果该 Bean 容器预实例化单例
 * （如 XML ApplicationContext 变体所示），则在 Bean 容器在 EJB 容器加载目标 EJB 之前
 * 加载时，可能会出现问题。这是因为默认情况下，JNDI 查找将在本类的 init 方法中执行并
 * 缓存，但此时 EJB 尚未绑定到目标位置。最佳解决方案是将 lookupHomeOnStartup 属性
 * 设置为 false，这样将在首次访问 EJB 时获取 Home 对象。
 * （此标志默认为 true 仅为向后兼容的原因）。
 *
 * <p>此代理工厂通常与 RMI 业务接口一起使用，该接口作为 EJB 组件接口的超接口。
 * 或者，此工厂也可以代理具有匹配的非 RMI 业务接口的远程 SLSB，即镜像 EJB 业务方法
 * 但不声明 RemoteException 的接口。在后一种情况下，EJB 存根抛出的 RemoteException
 * 将自动转换为 Spring 的未检查异常 RemoteAccessException。
 *
 * @author Rod Johnson
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 09.05.2003
 * @see org.springframework.remoting.RemoteAccessException
 * @see AbstractSlsbInvokerInterceptor#setLookupHomeOnStartup
 * @see AbstractSlsbInvokerInterceptor#setCacheHome
 * @see AbstractRemoteSlsbInvokerInterceptor#setRefreshHomeOnConnectFailure
 */
public class SimpleRemoteStatelessSessionProxyFactoryBean extends SimpleRemoteSlsbInvokerInterceptor
	implements FactoryBean<Object>, BeanClassLoaderAware {

	/** 我们正在代理的 EJB 的业务接口。 */
	@Nullable
	private Class<?> businessInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** EJB 对象。 */
	@Nullable
	private Object proxy;


	/**
	 * 设置我们正在代理的 EJB 的业务接口。
	 * 这通常是 EJB 远程组件接口的超接口。
	 * 在实现 EJB 时，使用业务方法接口是最佳实践。
	 * <p>也可以指定匹配的非 RMI 业务接口，即镜像 EJB 业务方法但不声明
	 * RemoteException 的接口。在这种情况下，EJB 存根抛出的 RemoteException
	 * 将自动转换为 Spring 的通用 RemoteAccessException。
	 * @param businessInterface EJB 的业务接口
	 */
	public void setBusinessInterface(@Nullable Class<?> businessInterface) {
		this.businessInterface = businessInterface;
	}

	/**
	 * 返回我们正在代理的 EJB 的业务接口。
	 */
	@Nullable
	public Class<?> getBusinessInterface() {
		return this.businessInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.businessInterface == null) {
			throw new IllegalArgumentException("businessInterface is required");
		}
		this.proxy = new ProxyFactory(this.businessInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.businessInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
