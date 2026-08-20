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

package org.springframework.jmx.support;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 提供将 MBean 注册到 {@link javax.management.MBeanServer} 的支持基础设施。
 * 当遇到给定 {@link ObjectName} 处已存在的 MBean 时，其行为是完全可配置的，
 * 允许灵活的注册设置。
 *
 * <p>所有注册的 MBean 都会被跟踪，并且可以通过调用 #{@link #unregisterBeans()} 方法
 * 来取消注册。
 *
 * <p>子类可以在 MBean 被注册或取消注册时接收通知，方法是分别重写
 * {@link #onRegister(ObjectName)} 和 {@link #onUnregister(ObjectName)} 方法。
 *
 * <p>默认情况下，如果尝试使用已存在的 {@link javax.management.ObjectName} 注册 MBean，
 * 注册过程将失败。
 *
 * <p>通过将 {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy}
 * 属性设置为 {@link RegistrationPolicy#IGNORE_EXISTING}，注册过程将简单地忽略
 * 已存在的 MBean，使其保持注册状态。这在多个应用程序想要在共享的
 * {@link MBeanServer} 中共享公共 MBean 的环境中很有用。
 *
 * <p>将 {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy} 属性
 * 设置为 {@link RegistrationPolicy#REPLACE_EXISTING} 将导致在注册期间如有必要
 * 替换已存在的 MBean。这在无法保证 {@link MBeanServer} 状态的情况下很有用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 * @see #setServer
 * @see #setRegistrationPolicy
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MBeanRegistrationSupport {

	/**
 * 此类的 {@code Log} 实例。
 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 用于注册 bean 的 {@code MBeanServer} 实例。
	 */
	@Nullable
	protected MBeanServer server;

	/**
	 * 由此导出器注册的 bean。
	 */
	private final Set<ObjectName> registeredBeans = new LinkedHashSet<>();

	/**
	 * 注册 MBean 时发现其已存在时使用的策略。
	 * 默认情况下会抛出异常。
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;


	/**
	 * 指定所有 bean 应注册到的 {@code MBeanServer} 实例。
	 * 如果未提供，{@code MBeanExporter} 将尝试定位现有的 {@code MBeanServer}。
	 */
	public void setServer(@Nullable MBeanServer server) {
		this.server = server;
	}

	/**
	 * 返回 bean 将注册到的 {@code MBeanServer}。
	 */
	@Nullable
	public final MBeanServer getServer() {
		return this.server;
	}

	/**
	 * 尝试在已存在的 {@link javax.management.ObjectName} 下注册 MBean 时使用的策略。
	 * @param registrationPolicy 要使用的策略
	 * @since 3.2
	 */
	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		Assert.notNull(registrationPolicy, "RegistrationPolicy must not be null");
		this.registrationPolicy = registrationPolicy;
	}


	/**
	 * 实际将 MBean 注册到服务器。遇到已存在的 MBean 时的行为
	 * 可以通过 {@link #setRegistrationPolicy} 进行配置。
	 * @param mbean MBean 实例
	 * @param objectName 建议的 MBean 对象名称
	 * @throws JMException 如果注册失败
	 */
	protected void doRegister(Object mbean, ObjectName objectName) throws JMException {
		Assert.state(this.server != null, "No MBeanServer set");
		ObjectName actualObjectName;

		synchronized (this.registeredBeans) {
			ObjectInstance registeredBean = null;
			try {
				registeredBean = this.server.registerMBean(mbean, objectName);
			}
			catch (InstanceAlreadyExistsException ex) {
				if (this.registrationPolicy == RegistrationPolicy.IGNORE_EXISTING) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring existing MBean at [" + objectName + "]");
					}
				}
				else if (this.registrationPolicy == RegistrationPolicy.REPLACE_EXISTING) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Replacing existing MBean at [" + objectName + "]");
						}
						this.server.unregisterMBean(objectName);
						registeredBean = this.server.registerMBean(mbean, objectName);
					}
					catch (InstanceNotFoundException ex2) {
						if (logger.isInfoEnabled()) {
							logger.info("Unable to replace existing MBean at [" + objectName + "]", ex2);
						}
						throw ex;
					}
				}
				else {
					throw ex;
				}
			}

			// 跟踪注册并通知监听器。
			actualObjectName = (registeredBean != null ? registeredBean.getObjectName() : null);
			if (actualObjectName == null) {
				actualObjectName = objectName;
			}
			this.registeredBeans.add(actualObjectName);
		}

		onRegister(actualObjectName, mbean);
	}

	/**
	 * 取消注册由此类实例注册的所有 bean。
	 */
	protected void unregisterBeans() {
		Set<ObjectName> snapshot;
		synchronized (this.registeredBeans) {
			snapshot = new LinkedHashSet<>(this.registeredBeans);
		}
		if (!snapshot.isEmpty()) {
			logger.debug("Unregistering JMX-exposed beans");
			for (ObjectName objectName : snapshot) {
				doUnregister(objectName);
			}
		}
	}

	/**
	 * 实际从服务器取消注册指定的 MBean。
	 * @param objectName 建议的 MBean 对象名称
	 */
	protected void doUnregister(ObjectName objectName) {
		Assert.state(this.server != null, "No MBeanServer set");
		boolean actuallyUnregistered = false;

		synchronized (this.registeredBeans) {
			if (this.registeredBeans.remove(objectName)) {
				try {
					// MBean 可能已被外部进程取消注册
					if (this.server.isRegistered(objectName)) {
						this.server.unregisterMBean(objectName);
						actuallyUnregistered = true;
					}
					else {
						if (logger.isInfoEnabled()) {
							logger.info("Could not unregister MBean [" + objectName + "] as said MBean " +
									"is not registered (perhaps already unregistered by an external process)");
						}
					}
				}
				catch (JMException ex) {
					if (logger.isInfoEnabled()) {
						logger.info("Could not unregister MBean [" + objectName + "]", ex);
					}
				}
			}
		}

		if (actuallyUnregistered) {
			onUnregister(objectName);
		}
	}

	/**
	 * 返回所有已注册 bean 的 {@link ObjectName ObjectNames}。
	 */
	protected final ObjectName[] getRegisteredObjectNames() {
		synchronized (this.registeredBeans) {
			return this.registeredBeans.toArray(new ObjectName[0]);
		}
	}


	/**
	 * 当 MBean 在给定 {@link ObjectName} 下注册时调用。允许子类在 MBean 注册时
	 * 执行额外的处理。
	 * <p>默认实现委托给 {@link #onRegister(ObjectName)}。
	 * @param objectName MBean 注册时使用的实际 {@link ObjectName}
	 * @param mbean 已注册的 MBean 实例
	 */
	protected void onRegister(ObjectName objectName, Object mbean) {
		onRegister(objectName);
	}

	/**
	 * 当 MBean 在给定 {@link ObjectName} 下注册时调用。允许子类在 MBean 注册时
	 * 执行额外的处理。
	 * <p>默认实现为空。可在子类中重写。
	 * @param objectName MBean 注册时使用的实际 {@link ObjectName}
	 */
	protected void onRegister(ObjectName objectName) {
	}

	/**
	 * 当 MBean 在给定 {@link ObjectName} 下取消注册时调用。允许子类在 MBean 取消注册时
	 * 执行额外的处理。
	 * <p>默认实现为空。可在子类中重写。
	 * @param objectName MBean 注册时使用的 {@link ObjectName}
	 */
	protected void onUnregister(ObjectName objectName) {
	}

}
