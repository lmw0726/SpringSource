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

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.lang.Nullable;

/**
 * 通过标准 JMX 1.2 {@link javax.management.MBeanServerFactory}
 * API 获取 {@link javax.management.MBeanServer} 引用的 {@link FactoryBean}。
 *
 * <p>将 {@code MBeanServer} 暴露为 bean 引用。
 *
 * <p>默认情况下，{@code MBeanServerFactoryBean} 总是会创建一个新的
 * {@code MBeanServer}，即使已经有一个正在运行。要让
 * {@code MBeanServerFactoryBean} 先尝试定位正在运行的
 * {@code MBeanServer}，请将 "locateExistingServerIfPossible"
 * 属性的值设置为 "true"。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setLocateExistingServerIfPossible
 * @see #locateMBeanServer
 * @see javax.management.MBeanServer
 * @see javax.management.MBeanServerFactory#findMBeanServer
 * @see javax.management.MBeanServerFactory#createMBeanServer
 * @see javax.management.MBeanServerFactory#newMBeanServer
 * @see MBeanServerConnectionFactoryBean
 * @see ConnectorServerFactoryBean
 */
public class MBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean, DisposableBean {


	protected final Log logger = LogFactory.getLog(getClass());

	private boolean locateExistingServerIfPossible = false;

	@Nullable
	private String agentId;

	@Nullable
	private String defaultDomain;

	private boolean registerWithFactory = true;

	@Nullable
	private MBeanServer server;

	private boolean newlyRegistered = false;


	/**
	 * 设置 {@code MBeanServerFactoryBean} 是否应在创建新的 MBeanServer
	 * 之前尝试定位正在运行的 {@code MBeanServer}。
	 * <p>默认值为 {@code false}。
	 */
	public void setLocateExistingServerIfPossible(boolean locateExistingServerIfPossible) {
		this.locateExistingServerIfPossible = locateExistingServerIfPossible;
	}

	/**
	 * 设置要定位的 {@code MBeanServer} 的 agent id。
	 * <p>默认为无。如果指定了此值，将会自动尝试定位对应的 MBeanServer，
	 * 并且（重要的是）如果无法定位到该 MBeanServer，则不会尝试创建新的
	 * MBeanServer（并在解析时抛出 MBeanServerNotFoundException）。
	 * <p>指定空字符串表示平台 MBeanServer。
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * 设置 {@code MBeanServer} 使用的默认域，该值将传递给
	 * {@code MBeanServerFactory.createMBeanServer()}
	 * 或 {@code MBeanServerFactory.findMBeanServer()}。
	 * <p>默认为无。
	 * @see javax.management.MBeanServerFactory#createMBeanServer(String)
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	/**
	 * 设置是否将 {@code MBeanServer} 注册到
	 * {@code MBeanServerFactory}，使其可通过
	 * {@code MBeanServerFactory.findMBeanServer()} 访问。
	 * <p>默认值为 {@code true}。
	 * @see javax.management.MBeanServerFactory#createMBeanServer
	 * @see javax.management.MBeanServerFactory#findMBeanServer
	 */
	public void setRegisterWithFactory(boolean registerWithFactory) {
		this.registerWithFactory = registerWithFactory;
	}


	/**
	 * 创建 {@code MBeanServer} 实例。
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		// 如果需要，尝试定位现有的 MBeanServer。
		if (this.locateExistingServerIfPossible || this.agentId != null) {
			try {
				this.server = locateMBeanServer(this.agentId);
			}
			catch (MBeanServerNotFoundException ex) {
				// 如果指定了 agentId，则只需要定位该特定的 MBeanServer；
				// 因此如果找不到就直接退出。
				if (this.agentId != null) {
					throw ex;
				}
				logger.debug("No existing MBeanServer found - creating new one");
			}
		}

		// 如果需要，创建一个新的 MBeanServer 并注册它。
		if (this.server == null) {
			this.server = createMBeanServer(this.defaultDomain, this.registerWithFactory);
			this.newlyRegistered = this.registerWithFactory;
		}
	}

	/**
	 * 尝试定位现有的 {@code MBeanServer}。
	 * 当 {@code locateExistingServerIfPossible} 设置为 {@code true} 时调用。
	 * <p>默认实现尝试使用标准查找来定位 {@code MBeanServer}。
	 * 子类可以重写此方法以添加额外的定位逻辑。
	 * @param agentId 要检索的 MBeanServer 的 agent 标识符。
	 * 如果此参数为 {@code null}，则考虑所有已注册的 MBeanServer。
	 * @return 如果找到则返回 {@code MBeanServer}
	 * @throws org.springframework.jmx.MBeanServerNotFoundException
	 * 如果找不到 {@code MBeanServer}
	 * @see #setLocateExistingServerIfPossible
	 * @see JmxUtils#locateMBeanServer(String)
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	protected MBeanServer locateMBeanServer(@Nullable String agentId) throws MBeanServerNotFoundException {
		return JmxUtils.locateMBeanServer(agentId);
	}

	/**
	 * 创建一个新的 {@code MBeanServer} 实例，并根据需要将其注册到
	 * {@code MBeanServerFactory}。
	 * @param defaultDomain 默认域，如果无则为 {@code null}
	 * @param registerWithFactory 是否将 {@code MBeanServer}
	 * 注册到 {@code MBeanServerFactory}
	 * @see javax.management.MBeanServerFactory#createMBeanServer
	 * @see javax.management.MBeanServerFactory#newMBeanServer
	 */
	protected MBeanServer createMBeanServer(@Nullable String defaultDomain, boolean registerWithFactory) {
		if (registerWithFactory) {
			return MBeanServerFactory.createMBeanServer(defaultDomain);
		}
		else {
			return MBeanServerFactory.newMBeanServer(defaultDomain);
		}
	}


	@Override
	@Nullable
	public MBeanServer getObject() {
		return this.server;
	}

	@Override
	public Class<? extends MBeanServer> getObjectType() {
		return (this.server != null ? this.server.getClass() : MBeanServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 如果需要，取消注册 {@code MBeanServer} 实例。
	 */
	@Override
	public void destroy() {
		if (this.newlyRegistered) {
			MBeanServerFactory.releaseMBeanServer(this.server);
		}
	}

}
