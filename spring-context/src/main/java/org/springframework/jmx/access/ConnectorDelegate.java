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

package org.springframework.jmx.access;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.lang.Nullable;

/**
 * 用于管理 JMX 连接器的内部辅助类。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
class ConnectorDelegate {

	private static final Log logger = LogFactory.getLog(ConnectorDelegate.class);

	@Nullable
	private JMXConnector connector;


	/**
	 * 使用配置的 {@code JMXServiceURL} 连接到远程 {@code MBeanServer}：
	 * 连接到指定的 JMX 服务，如果未指定服务 URL 则连接到本地 MBeanServer。
	 * @param serviceUrl 要连接的 JMX 服务 URL（可能为 {@code null}）
	 * @param environment 连接器的 JMX 环境（可能为 {@code null}）
	 * @param agentId 本地 JMX MBeanServer 的 agent id（可能为 {@code null}）
	 */
	public MBeanServerConnection connect(@Nullable JMXServiceURL serviceUrl, @Nullable Map<String, ?> environment, @Nullable String agentId)
			throws MBeanServerNotFoundException {

		if (serviceUrl != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to remote MBeanServer at URL [" + serviceUrl + "]");
			}
			try {
				this.connector = JMXConnectorFactory.connect(serviceUrl, environment);
				return this.connector.getMBeanServerConnection();
			}
			catch (IOException ex) {
				throw new MBeanServerNotFoundException("Could not connect to remote MBeanServer [" + serviceUrl + "]", ex);
			}
		}
		else {
			logger.debug("Attempting to locate local MBeanServer");
			return JmxUtils.locateMBeanServer(agentId);
		}
	}

	/**
	 * 关闭此拦截器可能管理的任何 {@code JMXConnector}。
	 */
	public void close() {
		if (this.connector != null) {
			try {
				this.connector.close();
			}
			catch (IOException ex) {
				logger.debug("Could not close JMX connector", ex);
			}
		}
	}

}
