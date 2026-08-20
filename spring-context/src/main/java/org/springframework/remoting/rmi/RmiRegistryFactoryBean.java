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

package org.springframework.remoting.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean}，用于定位 {@link java.rmi.registry.Registry} 并将其暴露为 bean 引用。
 * 如果尚不存在 RMI 注册表，也可以在运行时动态创建一个本地 RMI 注册表。
 *
 * <p>可用于设置并将实际的 Registry 对象传递给需要使用 RMI 的应用程序对象。
 * 一个需要使用 RMI 的对象示例是 Spring 的 {@link RmiServiceExporter}，
 * 它要么使用传入的 Registry 引用，要么回退到其本地属性和默认值指定的注册表。
 *
 * <p>也可用于在指定端口强制创建本地 RMI 注册表，例如用于 JMX 连接器。
 * 如果与 {@link org.springframework.jmx.support.ConnectorServerFactoryBean} 配合使用，
 * 建议将连接器定义（ConnectorServerFactoryBean）标记为
 * "depends-on" 注册表定义（RmiRegistryFactoryBean），
 * 以保证注册表先启动。
 *
 * <p>注意：本类的实现与 {@link RmiServiceExporter} 中的相应逻辑一致，
 * 并提供了相同的自定义扩展点。RmiServiceExporter 为了方便，
 * 自己实现了注册表查找：通常直接依赖注册表默认值即可。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see RmiServiceExporter#setRegistry
 * @see org.springframework.jmx.support.ConnectorServerFactoryBean
 * @see java.rmi.registry.Registry
 * @see java.rmi.registry.LocateRegistry
 * @deprecated 自 5.3 起（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class RmiRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private String host;

	private int port = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private boolean alwaysCreate = false;

	private boolean created = false;


	/**
	 * 设置导出 RMI 服务的注册表主机，
	 * 即 {@code rmi://HOST:port/name}
	 * <p>默认为 localhost。
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 返回导出 RMI 服务的注册表主机。
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 设置导出 RMI 服务的注册表端口，
	 * 即 {@code rmi://host:PORT/name}
	 * <p>默认为 {@code Registry.REGISTRY_PORT}（1099）。
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 返回导出 RMI 服务的注册表端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 客户端套接字工厂。
	 * <p>如果给定对象同时实现了 {@code java.rmi.server.RMIServerSocketFactory}，
	 * 它将自动注册为服务端套接字工厂。
	 * @see #setServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, java.rmi.server.RMIClientSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 服务端套接字工厂。
	 * <p>仅当客户端套接字工厂未实现 {@code java.rmi.server.RMIServerSocketFactory} 时才需要指定。
	 * @see #setClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#createRegistry(int, RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * 设置是否始终在进程内创建注册表，而不尝试定位指定端口上的现有注册表。
	 * <p>默认为 "false"。当你始终打算创建新注册表时，
	 * 将此标志切换为 "true" 可以避免定位现有注册表的开销。
	 */
	public void setAlwaysCreate(boolean alwaysCreate) {
		this.alwaysCreate = alwaysCreate;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// 检查注册表的套接字工厂。
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// 获取要暴露的 RMI 注册表。
		this.registry = getRegistry(this.host, this.port, this.clientSocketFactory, this.serverSocketFactory);
	}


	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryHost 要使用的注册表主机（如果指定了此参数，
	 * 则不会隐式创建 RMI 注册表）
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 注册表的 RMI 客户端套接字工厂（可选）
	 * @param serverSocketFactory 注册表的 RMI 服务端套接字工厂（可选）
	 * @return RMI 注册表
	 * @throws java.rmi.RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// 已显式指定主机：只能进行查找。
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
			}
			Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
			testRegistry(reg);
			return reg;
		}

		else {
			return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
		}
	}

	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 注册表的 RMI 客户端套接字工厂（可选）
	 * @param serverSocketFactory 注册表的 RMI 服务端套接字工厂（可选）
	 * @return RMI 注册表
	 * @throws RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreate) {
				logger.debug("Creating new RMI registry");
				this.created = true;
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// 检索现有注册表。
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.trace("RMI registry access threw exception", ex);
					logger.debug("Could not detect RMI registry - creating new one");
					// 未找到注册表 -> 创建新注册表。
					this.created = true;
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryPort 要使用的注册表端口
	 * @return RMI 注册表
	 * @throws RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreate) {
			logger.debug("Creating new RMI registry");
			this.created = true;
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// 检索现有注册表。
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.trace("RMI registry access threw exception", ex);
				logger.debug("Could not detect RMI registry - creating new one");
				// 未找到注册表 -> 创建新注册表。
				this.created = true;
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * 测试给定的 RMI 注册表，对其调用某个操作以检查其是否仍然活跃。
	 * <p>默认实现调用 {@code Registry.list()}。
	 * @param registry 要测试的 RMI 注册表
	 * @throws RemoteException 如果注册表方法抛出异常
	 * @see java.rmi.registry.Registry#list()
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	@Override
	public Registry getObject() throws Exception {
		return this.registry;
	}

	@Override
	public Class<? extends Registry> getObjectType() {
		return (this.registry != null ? this.registry.getClass() : Registry.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 在 bean 工厂关闭时取消导出 RMI 注册表，
	 * 前提是本 bean 实际创建了注册表。
	 */
	@Override
	public void destroy() throws RemoteException {
		if (this.created) {
			logger.debug("Unexporting RMI registry");
			UnicastRemoteObject.unexportObject(this.registry, true);
		}
	}

}
