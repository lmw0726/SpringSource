/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jmx.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 创建 JSR-160 {@link JMXConnectorServer} 的 {@link FactoryBean}，
 * 可选择将其注册到 {@link MBeanServer}，然后启动它。
 *
 * <p>通过将 {@code threaded} 属性设置为 {@code true}，可以在单独的线程中启动
 * {@code JMXConnectorServer}。通过将 {@code daemon} 属性设置为 {@code true}，
 * 可以将该线程配置为守护线程。
 *
 * <p>当包含该类实例的 {@code ApplicationContext} 关闭时，此实例会被销毁，
 * 从而正确地关闭 {@code JMXConnectorServer}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see JMXConnectorServer
 * @see MBeanServer
 */
public class ConnectorServerFactoryBean extends MBeanRegistrationSupport
		implements FactoryBean<JMXConnectorServer>, InitializingBean, DisposableBean {

	/** 默认的服务 URL。 */
	public static final String DEFAULT_SERVICE_URL = "service:jmx:jmxmp://localhost:9875";


	private String serviceUrl = DEFAULT_SERVICE_URL;

	private Map<String, Object> environment = new HashMap<>();

	@Nullable
	private MBeanServerForwarder forwarder;

	@Nullable
	private ObjectName objectName;

	private boolean threaded = false;

	private boolean daemon = false;

	@Nullable
	private JMXConnectorServer connectorServer;


	/**
	 * 设置 {@code JMXConnectorServer} 的服务 URL。
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * 以 {@code java.util.Properties}（String 键/值对）的形式设置用于构造
	 * {@code JMXConnectorServer} 的环境属性。
	 */
	public void setEnvironment(@Nullable Properties environment) {
		CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
	}

	/**
	 * 以 {@code Map}（String 键和任意 Object 值）的形式设置用于构造
	 * {@code JMXConnector} 的环境属性。
	 */
	public void setEnvironmentMap(@Nullable Map<String, ?> environment) {
		if (environment != null) {
			this.environment.putAll(environment);
		}
	}

	/**
	 * 设置应用于 {@code JMXConnectorServer} 的 MBeanServerForwarder。
	 */
	public void setForwarder(MBeanServerForwarder forwarder) {
		this.forwarder = forwarder;
	}

	/**
	 * 设置用于将 {@code JMXConnectorServer} 本身注册到 {@code MBeanServer}
	 * 的 {@code ObjectName}，可以是 {@code ObjectName} 实例或 {@code String}。
	 * @throws MalformedObjectNameException 如果 {@code ObjectName} 格式不正确
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置是否应在单独的线程中启动 {@code JMXConnectorServer}。
	 */
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	/**
	 * 设置为 {@code JMXConnectorServer} 启动的线程是否应作为守护线程启动。
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}


	/**
	 * 启动连接器服务器。如果 {@code threaded} 标志设置为 {@code true}，
	 * 则 {@code JMXConnectorServer} 将在单独的线程中启动。
	 * 如果 {@code daemon} 标志设置为 {@code true}，该线程将作为守护线程启动。
	 * @throws JMException 如果在将连接器服务器注册到 {@code MBeanServer} 时出现问题
	 * @throws IOException 如果启动连接器服务器时出现问题
	 */
	@Override
	public void afterPropertiesSet() throws JMException, IOException {
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}

		// 创建 JMX 服务 URL。
		JMXServiceURL url = new JMXServiceURL(this.serviceUrl);

		// 现在创建连接器服务器。
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);

		// 设置给定的 MBeanServerForwarder（如果有的话）。
		if (this.forwarder != null) {
			this.connectorServer.setMBeanServerForwarder(this.forwarder);
		}

		// 我们是否想要将连接器注册到 MBean 服务器？
		if (this.objectName != null) {
			doRegister(this.connectorServer, this.objectName);
		}

		try {
			if (this.threaded) {
				// 异步启动连接器服务器（在单独的线程中）。
				final JMXConnectorServer serverToStart = this.connectorServer;
				Thread connectorThread = new Thread() {
					@Override
					public void run() {
						try {
							serverToStart.start();
						}
						catch (IOException ex) {
							throw new JmxException("Could not start JMX connector server after delay", ex);
						}
					}
				};

				connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
				connectorThread.setDaemon(this.daemon);
				connectorThread.start();
			}
			else {
				// 在同一个线程中启动连接器服务器。
				this.connectorServer.start();
			}

			if (logger.isInfoEnabled()) {
				logger.info("JMX connector server started: " + this.connectorServer);
			}
		}

		catch (IOException ex) {
			// 如果启动失败，则注销连接器服务器。
			unregisterBeans();
			throw ex;
		}
	}


	@Override
	@Nullable
	public JMXConnectorServer getObject() {
		return this.connectorServer;
	}

	@Override
	public Class<? extends JMXConnectorServer> getObjectType() {
		return (this.connectorServer != null ? this.connectorServer.getClass() : JMXConnectorServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 停止由该类实例管理的 {@code JMXConnectorServer}。
	 * 在 {@code ApplicationContext} 关闭时自动调用。
	 * @throws IOException 如果停止连接器服务器时出错
	 */
	@Override
	public void destroy() throws IOException {
		try {
			if (this.connectorServer != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping JMX connector server: " + this.connectorServer);
				}
				this.connectorServer.stop();
			}
		}
		finally {
			unregisterBeans();
		}
	}

}
