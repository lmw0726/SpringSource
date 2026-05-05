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
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 创建到远程 {@code MBeanServer}（通过 {@code JMXServerConnector} 暴露）的
 * JMX 1.2 {@code MBeanServerConnection} 的 {@link FactoryBean}。
 * 将 {@code MBeanServer} 暴露以供 Bean 引用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanServerFactoryBean
 * @see ConnectorServerFactoryBean
 * @see org.springframework.jmx.access.MBeanClientInterceptor#setServer
 * @see org.springframework.jmx.access.NotificationListenerRegistrar#setServer
 */
public class MBeanServerConnectionFactoryBean
		implements FactoryBean<MBeanServerConnection>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	@Nullable
	private JMXServiceURL serviceUrl;

	private Map<String, Object> environment = new HashMap<>();

	private boolean connectOnStartup = true;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private JMXConnector connector;

	@Nullable
	private MBeanServerConnection connection;

	@Nullable
	private JMXConnectorLazyInitTargetSource connectorTargetSource;


	/**
	 * 设置远程 {@code MBeanServer} 的服务 URL。
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 设置用于构造 {@code JMXConnector} 的环境属性，
	 * 格式为 {@code java.util.Properties}（String 键/值对）。
	 */
	public void setEnvironment(Properties environment) {
		CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
	}

	/**
	 * 设置用于构造 {@code JMXConnector} 的环境属性，
	 * 格式为 String 键和任意 Object 值的 {@code Map}。
	 */
	public void setEnvironmentMap(@Nullable Map<String, ?> environment) {
		if (environment != null) {
			this.environment.putAll(environment);
		}
	}

	/**
	 * 设置是否在启动时连接到服务器。
	 * <p>默认值为 {@code true}。
	 * <p>可以关闭以允许 JMX 服务器延迟启动。
	 * 在这种情况下，JMX 连接器将在首次访问时获取。
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 根据给定的设置创建一个 {@code JMXConnector}
	 * 并暴露关联的 {@code MBeanServerConnection}。
	 */
	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.serviceUrl == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}

		if (this.connectOnStartup) {
			connect();
		}
		else {
			createLazyConnection();
		}
	}

	/**
	 * 使用配置的服务 URL 和环境属性连接到远程 {@code MBeanServer}。
	 */
	private void connect() throws IOException {
		Assert.state(this.serviceUrl != null, "No JMXServiceURL set");
		this.connector = JMXConnectorFactory.connect(this.serviceUrl, this.environment);
		this.connection = this.connector.getMBeanServerConnection();
	}

	/**
	 * 为 {@code JMXConnector} 和 {@code MBeanServerConnection} 创建延迟代理。
	 */
	private void createLazyConnection() {
		this.connectorTargetSource = new JMXConnectorLazyInitTargetSource();
		TargetSource connectionTargetSource = new MBeanServerConnectionLazyInitTargetSource();

		this.connector = (JMXConnector)
				new ProxyFactory(JMXConnector.class, this.connectorTargetSource).getProxy(this.beanClassLoader);
		this.connection = (MBeanServerConnection)
				new ProxyFactory(MBeanServerConnection.class, connectionTargetSource).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public MBeanServerConnection getObject() {
		return this.connection;
	}

	@Override
	public Class<? extends MBeanServerConnection> getObjectType() {
		return (this.connection != null ? this.connection.getClass() : MBeanServerConnection.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 关闭底层的 {@code JMXConnector}。
	 */
	@Override
	public void destroy() throws IOException {
		if (this.connector != null &&
				(this.connectorTargetSource == null || this.connectorTargetSource.isInitialized())) {
			this.connector.close();
		}
	}


	/**
	 * 使用配置的服务 URL 和环境属性延迟创建一个 {@code JMXConnector}。
	 * @see MBeanServerConnectionFactoryBean#setServiceUrl(String)
	 * @see MBeanServerConnectionFactoryBean#setEnvironment(java.util.Properties)
	 */
	private class JMXConnectorLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			Assert.state(serviceUrl != null, "No JMXServiceURL set");
			return JMXConnectorFactory.connect(serviceUrl, environment);
		}

		@Override
		public Class<?> getTargetClass() {
			return JMXConnector.class;
		}
	}


	/**
	 * 延迟创建一个 {@code MBeanServerConnection}。
	 */
	private class MBeanServerConnectionLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			Assert.state(connector != null, "JMXConnector not initialized");
			return connector.getMBeanServerConnection();
		}

		@Override
		public Class<?> getTargetClass() {
			return MBeanServerConnection.class;
		}
	}

}
