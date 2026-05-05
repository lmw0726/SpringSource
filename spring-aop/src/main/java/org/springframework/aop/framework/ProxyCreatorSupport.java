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

package org.springframework.aop.framework;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理工厂的基类。
 * 提供对可配置 AopProxyFactory 的便捷访问。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #createAopProxy()
 */
@SuppressWarnings("serial")
public class ProxyCreatorSupport extends AdvisedSupport {

	private AopProxyFactory aopProxyFactory;

	private final List<AdvisedSupportListener> listeners = new ArrayList<>();

	/** 当第一个 AOP 代理已创建时设置为 true。 */
	private boolean active = false;


	/**
	 * 创建新的 ProxyCreatorSupport 实例。
	 */
	public ProxyCreatorSupport() {
		this.aopProxyFactory = new DefaultAopProxyFactory();
	}

	/**
	 * 创建新的 ProxyCreatorSupport 实例。
	 * @param aopProxyFactory 要使用的 AopProxyFactory
	 */
	public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}


	/**
	 * 自定义 AopProxyFactory，允许插入不同策略而无需更改核心框架。
	 * <p>默认为 {@link DefaultAopProxyFactory}，
	 * 根据需求使用动态 JDK 代理或 CGLIB 代理。
	 */
	public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}

	/**
	 * 返回此 ProxyConfig 使用的 AopProxyFactory。
	 */
	public AopProxyFactory getAopProxyFactory() {
		return this.aopProxyFactory;
	}

	/**
	 * 将给定 AdvisedSupportListener 添加到此代理配置中。
	 * @param listener 要注册的监听器
	 */
	public void addListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.add(listener);
	}

	/**
	 * 从此代理配置中移除给定 AdvisedSupportListener。
	 * @param listener 要注销的监听器
	 */
	public void removeListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.remove(listener);
	}


	/**
	 * 子类应调用此方法以获取新的 AOP 代理。
	 * 它们<b>不应</b>以 {@code this} 作为参数创建 AOP 代理。
	 */
	protected final synchronized AopProxy createAopProxy() {
		// 如果当前 ProxyFactory 还未激活
		if (!this.active) {
			// 执行激活逻辑
			activate();
		}
		// 获取 AopProxyFactory（默认是 DefaultAopProxyFactory）
		// 并根据当前配置创建具体的 AopProxy 实例
		return getAopProxyFactory().createAopProxy(this);
	}

	/**
	 * 激活此代理配置。
	 * @see AdvisedSupportListener#activated
	 */
	private void activate() {
		this.active = true;
		for (AdvisedSupportListener listener : this.listeners) {
			// 触发建言支持监听器的激活方法
			listener.activated(this);
		}
	}

	/**
	 * 将 advice 变更事件传播给所有 AdvisedSupportListeners。
	 * @see AdvisedSupportListener#adviceChanged
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		synchronized (this) {
			if (this.active) {
				for (AdvisedSupportListener listener : this.listeners) {
					listener.adviceChanged(this);
				}
			}
		}
	}

	/**
	 * 子类可以调用此方法来检查是否已创建任何 AOP 代理。
	 */
	protected final synchronized boolean isActive() {
		return this.active;
	}

}
