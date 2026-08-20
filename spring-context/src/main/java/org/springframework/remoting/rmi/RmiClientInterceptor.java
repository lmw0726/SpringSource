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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationUtils;

/**
 * 用于访问传统 RMI 服务或 RMI 调用器的 {@link org.aopalliance.intercept.MethodInterceptor}。
 * 服务 URL 必须是有效的 RMI URL（例如 "rmi://localhost:1099/myservice"）。
 *
 * <p>RMI 调用器在 RmiInvocationHandler 层面工作，任何服务只需一个存根。
 * 服务接口不必继承 {@code java.rmi.Remote} 或抛出 {@code java.rmi.RemoteException}。
 * 远程调用失败时会抛出 Spring 的非受检异常 RemoteAccessException。
 * 当然，入参和出参必须是可序列化的。
 *
 * <p>对于传统 RMI 服务，此调用器通常与 RMI 服务接口一起使用。
 * 或者，此调用器也可以用匹配的非 RMI 业务接口来代理远程 RMI 服务，
 * 即镜像了 RMI 服务方法但不声明 RemoteException 的接口。
 * 在后一种情况下，RMI 存根抛出的 RemoteException 会自动转换为
 * Spring 的非受检异常 RemoteAccessException。
 *
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see RmiServiceExporter
 * @see RmiProxyFactoryBean
 * @see RmiInvocationHandler
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 * @see java.rmi.Remote
 * @deprecated 从 5.3 开始（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class RmiClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor {

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private RMIClientSocketFactory registryClientSocketFactory;

	private Remote cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置是否在启动时查找 RMI 存根。默认为 "true"。
	 * <p>可以关闭以允许 RMI 服务器延迟启动。
	 * 在这种情况下，RMI 存根将在首次访问时获取。
	 * @see #setCacheStub
	 */
	public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
		this.lookupStubOnStartup = lookupStubOnStartup;
	}

	/**
	 * 设置是否在定位 RMI 存根后对其进行缓存。默认为 "true"。
	 * <p>可以关闭以允许 RMI 服务器热重启。
	 * 在这种情况下，每次调用都会获取 RMI 存根。
	 * @see #setLookupStubOnStartup
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新 RMI 存根。默认为 "false"。
	 * <p>可以开启以允许 RMI 服务器热重启。
	 * 如果缓存的 RMI 存根抛出指示远程连接失败的 RMI 异常，
	 * 将获取一个新的代理并重试调用。
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置用于访问 RMI 注册中心的自定义 RMI 客户端套接字工厂。
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 在启动时获取 RMI 存根（如果需要）。
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 * @see #setLookupStubOnStartup
	 * @see #lookupStub
	 */
	public void prepare() throws RemoteLookupFailureException {
		// 在初始化时缓存 RMI 存根？
		if (this.lookupStubOnStartup) {
			Remote remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("RMI stub [" + getServiceUrl() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
						"] for RMI stub [" + getServiceUrl() + "] - " +
						(!isImpl ? "not " : "") + "directly implemented");
				}
			}
			if (this.cacheStub) {
				this.cachedStub = remoteObj;
			}
		}
	}

	/**
	 * 创建 RMI 存根，通常通过查找的方式。
	 * <p>如果 "cacheStub" 为 "true"，则在拦截器初始化时调用；
	 * 否则由 {@link #getStub()} 在每次调用时调用。
	 * <p>默认实现通过 {@code java.rmi.Naming} 查找服务 URL。
	 * 子类可以覆盖此方法。
	 * @return 要存储在此拦截器中的 RMI 存根
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 * @see #setCacheStub
	 * @see java.rmi.Naming#lookup
	 */
	protected Remote lookupStub() throws RemoteLookupFailureException {
		try {
			Remote stub = null;
			if (this.registryClientSocketFactory != null) {
				// 为注册中心访问指定了 RMIClientSocketFactory。
				// 不幸的是，由于 RMI API 的限制，这意味着
				// 我们需要自行解析 RMI URL 并直接执行
				// LocateRegistry.getRegistry/Registry.lookup 调用。
				URL url = new URL(null, getServiceUrl(), new DummyURLStreamHandler());
				String protocol = url.getProtocol();
				if (protocol != null && !"rmi".equals(protocol)) {
					throw new MalformedURLException("Invalid URL scheme '" + protocol + "'");
				}
				String host = url.getHost();
				int port = url.getPort();
				String name = url.getPath();
				if (name != null && name.startsWith("/")) {
					name = name.substring(1);
				}
				Registry registry = LocateRegistry.getRegistry(host, port, this.registryClientSocketFactory);
				stub = registry.lookup(name);
			}
			else {
				// 可以使用标准 RMI 查找 API...
				stub = Naming.lookup(getServiceUrl());
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Located RMI stub with URL [" + getServiceUrl() + "]");
			}
			return stub;
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
		catch (NotBoundException ex) {
			throw new RemoteLookupFailureException(
					"Could not find RMI service [" + getServiceUrl() + "] in RMI registry", ex);
		}
		catch (RemoteException ex) {
			throw new RemoteLookupFailureException("Lookup of RMI stub failed", ex);
		}
	}

	/**
	 * 返回要使用的 RMI 存根。每次调用时都会调用。
	 * <p>默认实现返回初始化时创建的存根（如果有的话）。
	 * 否则，它会调用 {@link #lookupStub} 为每次调用获取新的存根。
	 * 子类可以覆盖此方法，例如在重新创建之前缓存存根一段时间，
	 * 或者测试存根是否仍然存活。
	 * @return 用于调用的 RMI 存根
	 * @throws RemoteLookupFailureException 如果 RMI 存根创建失败
	 * @see #lookupStub
	 */
	protected Remote getStub() throws RemoteLookupFailureException {
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
	 * 获取 RMI 存根并委托给 {@code doInvoke}。
	 * 如果配置了连接失败时刷新，将在遇到相应的 RMI 异常时
	 * 调用 {@link #refreshAndRetry}。
	 * @see #getStub
	 * @see #doInvoke(MethodInvocation, Remote)
	 * @see #refreshAndRetry
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Remote stub = getStub();
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
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>默认实现委托给 {@link RmiClientInterceptorUtils#isConnectFailure}。
	 * @param ex 要检查的 RMI 异常
	 * @return 该异常是否应被视为连接失败
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	/**
	 * 刷新存根并在必要时重试远程调用。
	 * <p>如果未配置连接失败时刷新，此方法将直接重新抛出原始异常。
	 * @param invocation 失败的调用
	 * @param ex 远程调用时引发的异常
	 * @return 新调用的结果值（如果成功）
	 * @throws Throwable 新调用引发的异常（如果也失败了）
	 * @see #setRefreshStubOnConnectFailure
	 * @see #doInvoke
	 */
	@Nullable
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			String msg = "Could not connect to RMI service [" + getServiceUrl() + "] - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
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
		Remote freshStub = null;
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
	protected Object doInvoke(MethodInvocation invocation, Remote stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI 调用器
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw RmiClientInterceptorUtils.convertRmiAccessException(
					invocation.getMethod(), ex, isConnectFailure(ex), getServiceUrl());
			}
			catch (InvocationTargetException ex) {
				Throwable exToThrow = ex.getTargetException();
				RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
				throw exToThrow;
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getServiceUrl() + "]", ex);
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
					RemoteException rex = (RemoteException) targetEx;
					throw RmiClientInterceptorUtils.convertRmiAccessException(
							invocation.getMethod(), rex, isConnectFailure(rex), getServiceUrl());
				}
				else {
					throw targetEx;
				}
			}
		}
	}

	/**
	 * 将给定的 AOP 方法调用应用于给定的 {@link RmiInvocationHandler}。
	 * <p>默认实现委托给 {@link #createRemoteInvocation}。
	 * @param methodInvocation 当前的 AOP 方法调用
	 * @param invocationHandler 要应用调用的 RmiInvocationHandler
	 * @return 调用结果
	 * @throws RemoteException 如果发生通信错误
	 * @throws NoSuchMethodException 如果无法解析方法名
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致了异常
	 * @see org.springframework.remoting.support.RemoteInvocation
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "RMI invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}


	/**
	 * 伪 URLStreamHandler，仅用于抑制标准 {@code java.net.URL}
	 * URLStreamHandler 的查找，以便能够使用标准 URL 类来解析 "rmi:..." URL。
	 */
	private static class DummyURLStreamHandler extends URLStreamHandler {

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}
