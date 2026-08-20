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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiObjectLocator;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.util.Assert;

/**
 * 用于从 JNDI 访问 RMI 服务的 {@link org.aopalliance.intercept.MethodInterceptor}。
 * 通常用于 RMI-IIOP，但也可用于 EJB Home 对象（例如有状态会话 Bean 的 Home）。
 * 与简单的 JNDI 查找不同，此访问器还通过 PortableRemoteObject 执行窄化操作。
 *
 * <p>对于常规 RMI 服务，此调用器通常与 RMI 服务接口一起使用。
 * 另外，此调用器也可以使用匹配的非 RMI 业务接口来代理远程 RMI 服务，
 * 即镜像 RMI 服务方法但不声明 RemoteException 的接口。
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
 * 	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setJndiName
 * @see JndiRmiServiceExporter
 * @see JndiRmiProxyFactoryBean
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 * @see java.rmi.Remote
 * @deprecated 从 5.3 开始（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class JndiRmiClientInterceptor extends JndiObjectLocator implements MethodInterceptor, InitializingBean {

	private Class<?> serviceInterface;

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private boolean exposeAccessContext = false;

	private Object cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置要访问的服务接口。
	 * 该接口必须适用于特定的服务和远程调用工具。
	 * <p>通常需要设置此属性才能创建合适的服务代理，
	 * 但如果查找返回的是带类型的存根，则可以不设置。
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要访问的服务接口。
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置此访问器使用的 RemoteInvocationFactory。
	 * 默认为 {@link DefaultRemoteInvocationFactory}。
	 * <p>自定义的调用工厂可以向调用中添加额外的上下文信息，
	 * 例如用户凭证。
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory = remoteInvocationFactory;
	}

	/**
	 * 返回此访问器使用的 RemoteInvocationFactory。
	 */
	public RemoteInvocationFactory getRemoteInvocationFactory() {
		return this.remoteInvocationFactory;
	}

	/**
	 * 设置是否在启动时查找 RMI 存根。默认为 "true"。
	 * <p>可以关闭此选项以允许 RMI 服务器延迟启动。
	 * 在这种情况下，RMI 存根将在首次访问时获取。
	 * @see #setCacheStub
	 */
	public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
		this.lookupStubOnStartup = lookupStubOnStartup;
	}

	/**
	 * 设置是否缓存已定位的 RMI 存根。默认为 "true"。
	 * <p>可以关闭此选项以允许 RMI 服务器热重启。
	 * 在这种情况下，每次调用都会重新获取 RMI 存根。
	 * @see #setLookupStubOnStartup
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新 RMI 存根。默认为 "false"。
	 * <p>可以开启此选项以允许 RMI 服务器热重启。
	 * 如果缓存的 RMI 存根抛出指示远程连接失败的 RMI 异常，
	 * 将获取新的代理并重试调用。
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置是否为所有对目标 RMI 存根的访问暴露 JNDI 环境上下文，
	 * 即为暴露对象引用上的所有方法调用暴露 JNDI 上下文。
	 * <p>默认为 "false"，即仅为对象查找暴露 JNDI 上下文。
	 * 将此标志切换为 "true" 以在每次 RMI 调用时暴露 JNDI 环境
	 * （包括授权上下文），这是 WebLogic 对具有授权要求的 RMI 存根所需要的。
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 在必要时于启动时获取 RMI 存根。
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 * @see #setLookupStubOnStartup
	 * @see #lookupStub
	 */
	public void prepare() throws RemoteLookupFailureException {
		// 启动时缓存 RMI 存根？
		if (this.lookupStubOnStartup) {
			Object remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("JNDI RMI object [" + getJndiName() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
							"] for JNDI RMI object [" + getJndiName() + "] - " +
							(!isImpl ? "not " : "") + "directly implemented");
				}
			}
			if (this.cacheStub) {
				this.cachedStub = remoteObj;
			}
		}
	}

	/**
	 * 创建 RMI 存根，通常通过查找获取。
	 * <p>如果 "cacheStub" 为 "true"，则在拦截器初始化时调用；
	 * 否则由 {@link #getStub()} 在每次调用时调用。
	 * <p>默认实现从 JNDI 环境获取服务。可以在子类中重写此方法。
	 * @return 要存储在此拦截器中的 RMI 存根
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 * @see #setCacheStub
	 * @see #lookup
	 */
	protected Object lookupStub() throws RemoteLookupFailureException {
		try {
			return lookup();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}
	}

	/**
	 * 返回要使用的 RMI 存根。每次调用时调用。
	 * <p>默认实现返回初始化时创建的存根（如果有）。
	 * 否则，它会调用 {@link #lookupStub} 为每次调用获取新的存根。
	 * 可以在子类中重写此方法，例如在重新创建存根之前缓存一段固定时间，
	 * 或者测试存根是否仍然存活。
	 * @return 用于调用的 RMI 存根
	 * @throws NamingException 如果存根创建失败
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 */
	protected Object getStub() throws NamingException, RemoteLookupFailureException {
		if (!this.cacheStub || (this.lookupStubOnStartup && !this.refreshStubOnConnectFailure)) {
			return (this.cachedStub != null ? this.cachedStub : lookupStub());
		}
		else {
			synchronized (this.stubMonitor) {
				if (this.cachedStub == null) {
					this.cachedStub = lookupStub();
				}
				return this.cachedStub;
			}
		}
	}


	/**
	 * 获取 RMI 存根并委托给 {@link #doInvoke}。
	 * 如果配置了连接失败时刷新，它将在遇到相应的 RMI 异常时
	 * 调用 {@link #refreshAndRetry}。
	 * @see #getStub
	 * @see #doInvoke
	 * @see #refreshAndRetry
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object stub;
		try {
			stub = getStub();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}

		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
		try {
			return doInvoke(invocation, stub);
		}
		catch (RemoteConnectFailureException ex) {
			return handleRemoteConnectFailure(invocation, ex);
		}
		catch (RemoteException ex) {
			if (isConnectFailure(ex)) {
				return handleRemoteConnectFailure(invocation, ex);
			}
			else {
				throw ex;
			}
		}
		finally {
			getJndiTemplate().releaseContext(ctx);
		}
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>默认实现委托给
	 * {@link RmiClientInterceptorUtils#isConnectFailure}。
	 * @param ex 要检查的 RMI 异常
	 * @return 该异常是否应被视为连接失败
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	/**
	 * 如果有必要，刷新存根并重试远程调用。
	 * <p>如果未配置连接失败时刷新，此方法
	 * 只是重新抛出原始异常。
	 * @param invocation 失败的调用
	 * @param ex 远程调用时引发的异常
	 * @return 新调用的结果值（如果成功）
	 * @throws Throwable 如果新调用也失败，则抛出异常
	 */
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not connect to RMI service [" + getJndiName() + "] - retrying", ex);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Could not connect to RMI service [" + getJndiName() + "] - retrying");
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新 RMI 存根并重试给定的调用。
	 * 在连接失败时由 invoke 调用。
	 * @param invocation AOP 方法调用
	 * @return 调用结果（如果有）
	 * @throws Throwable 如果调用失败
	 * @see #invoke
	 */
	@Nullable
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		Object freshStub;
		synchronized (this.stubMonitor) {
			this.cachedStub = null;
			freshStub = lookupStub();
			if (this.cacheStub) {
				this.cachedStub = freshStub;
			}
		}
		return doInvoke(invocation, freshStub);
	}


	/**
	 * 在给定的 RMI 存根上执行给定的调用。
	 * @param invocation AOP 方法调用
	 * @param stub 要调用的 RMI 存根
	 * @return 调用结果（如果有）
	 * @throws Throwable 如果调用失败
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation, Object stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI 调用器
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw convertRmiAccessException(ex, invocation.getMethod());
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getJndiName() + "]", ex);
			}
		}
		else {
			// 传统 RMI 存根
			try {
				return RmiClientInterceptorUtils.invokeRemoteMethod(invocation, stub);
			}
			catch (InvocationTargetException ex) {
				Throwable targetEx = ex.getTargetException();
				if (targetEx instanceof RemoteException) {
					throw convertRmiAccessException((RemoteException) targetEx, invocation.getMethod());
				}
				else {
					throw targetEx;
				}
			}
		}
	}

	/**
	 * 将给定的 AOP 方法调用应用到给定的 {@link RmiInvocationHandler}。
	 * <p>默认实现委托给 {@link #createRemoteInvocation}。
	 * @param methodInvocation 当前的 AOP 方法调用
	 * @param invocationHandler 要应用调用的 RmiInvocationHandler
	 * @return 调用结果
	 * @throws RemoteException 如果发生通信错误
	 * @throws NoSuchMethodException 如果无法解析方法名
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 * @see org.springframework.remoting.support.RemoteInvocation
	 */
	protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
			throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "RMI invoker proxy for service URL [" + getJndiName() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}

	/**
	 * 为给定的 AOP 方法调用创建新的 RemoteInvocation 对象。
	 * <p>默认实现委托给已配置的
	 * {@link #setRemoteInvocationFactory RemoteInvocationFactory}。
	 * 可以在子类中重写此方法以提供自定义的 RemoteInvocation 子类，
	 * 包含额外的调用参数（例如用户凭证）。
	 * <p>注意，建议构建自定义的 RemoteInvocationFactory 作为可重用的策略，
	 * 而不是重写此方法。
	 * @param methodInvocation 当前的 AOP 方法调用
	 * @return RemoteInvocation 对象
	 * @see RemoteInvocationFactory#createRemoteInvocation
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return getRemoteInvocationFactory().createRemoteInvocation(methodInvocation);
	}

	/**
	 * 将远程访问期间发生的给定 RMI RemoteException 转换为
	 * Spring 的 RemoteAccessException，前提是方法签名未声明
	 * RemoteException。否则返回原始的 RemoteException。
	 * @param method 被调用的方法
	 * @param ex 发生的 RemoteException
	 * @return 要抛出给调用者的异常
	 */
	private Exception convertRmiAccessException(RemoteException ex, Method method) {
		return RmiClientInterceptorUtils.convertRmiAccessException(method, ex, isConnectFailure(ex), getJndiName());
	}

}
