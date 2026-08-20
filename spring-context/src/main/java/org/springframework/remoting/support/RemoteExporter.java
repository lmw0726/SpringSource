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

package org.springframework.remoting.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 导出远程服务的类的抽象基类。
 * 提供 "service" 和 "serviceInterface" Bean 属性。
 *
 * <p>请注意，使用的服务接口会表现出一些远程调用的特征，
 * 比如其提供的方法调用粒度。此外，该接口的参数等必须是可序列化的。
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 */
public abstract class RemoteExporter extends RemotingSupport {

	private Object service;

	private Class<?> serviceInterface;

	private Boolean registerTraceInterceptor;

	private Object[] interceptors;


	/**
	 * 设置要导出的服务。
	 * 通常通过 Bean 引用进行填充。
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * 返回要导出的服务。
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * 设置要导出的服务接口。
	 * 该接口必须适合特定的服务和远程调用策略。
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要导出的服务接口。
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置是否为导出的服务注册 RemoteInvocationTraceInterceptor。
	 * 仅在子类使用 {@code getProxyForService} 创建要暴露的代理时生效。
	 * <p>默认值为 "true"。RemoteInvocationTraceInterceptor 最重要的作用是在将异常传播到客户端之前，
	 * 在服务器端记录异常堆栈跟踪。请注意，如果已指定 "interceptors" 属性，
	 * 则默认情况下 <i>不会</i> 注册 RemoteInvocationTraceInterceptor。
	 * @see #setInterceptors
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 */
	public void setRegisterTraceInterceptor(boolean registerTraceInterceptor) {
		this.registerTraceInterceptor = registerTraceInterceptor;
	}

	/**
	 * 设置在远程端点之前应用的额外拦截器（或通知器），
	 * 例如 PerformanceMonitorInterceptor。
	 * <p>您可以指定任何 AOP Alliance MethodInterceptor 或其他 Spring AOP Advice，
	 * 以及 Spring AOP Advisor。
	 * @see #getProxyForService
	 * @see org.springframework.aop.interceptor.PerformanceMonitorInterceptor
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}


	/**
	 * 检查服务引用是否已设置。
	 * @see #setService
	 */
	protected void checkService() throws IllegalArgumentException {
		Assert.notNull(getService(), "Property 'service' is required");
	}

	/**
	 * 检查服务引用是否已设置，以及是否与指定的服务匹配。
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class<?> serviceInterface = getServiceInterface();
		Assert.notNull(serviceInterface, "Property 'serviceInterface' is required");

		Object service = getService();
		if (service instanceof String) {
			throw new IllegalArgumentException("Service [" + service + "] is a String " +
					"rather than an actual service reference: Have you accidentally specified " +
					"the service bean name as value instead of as reference?");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("Service interface [" + serviceInterface.getName() +
					"] needs to be implemented by service [" + service + "] of class [" +
					service.getClass().getName() + "]");
		}
	}

	/**
	 * 获取给定服务对象的代理，该代理实现指定的服务接口。
	 * <p>用于导出一个不暴露任何内部细节、仅暴露专用于远程访问的特定接口的代理。
	 * 此外，默认情况下会注册一个 {@link RemoteInvocationTraceInterceptor}。
	 * @return 代理对象
	 * @see #setServiceInterface
	 * @see #setRegisterTraceInterceptor
	 * @see RemoteInvocationTraceInterceptor
	 */
	protected Object getProxyForService() {
		checkService();
		checkServiceInterface();

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addInterface(getServiceInterface());

		if (this.registerTraceInterceptor != null ? this.registerTraceInterceptor : this.interceptors == null) {
			proxyFactory.addAdvice(new RemoteInvocationTraceInterceptor(getExporterName()));
		}
		if (this.interceptors != null) {
			AdvisorAdapterRegistry adapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
			for (Object interceptor : this.interceptors) {
				proxyFactory.addAdvisor(adapterRegistry.wrap(interceptor));
			}
		}

		proxyFactory.setTarget(getService());
		proxyFactory.setOpaque(true);

		return proxyFactory.getProxy(getBeanClassLoader());
	}

	/**
	 * 返回此导出器的简短名称。
	 * 用于远程调用的跟踪。
	 * <p>默认为非限定类名（不含包名）。可在子类中重写。
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 * @see org.springframework.util.ClassUtils#getShortName
	 */
	protected String getExporterName() {
		return ClassUtils.getShortName(getClass());
	}

}
