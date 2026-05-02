/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 BeanFactory 的抽象 PointcutAdvisor，允许将任何 Advice
 * 配置为 BeanFactory 中 Advice bean 的引用。
 *
 * <p>（在 BeanFactory 中运行时）指定 advice bean 的名称而不是 advice 对象本身，
 * 可以在初始化时增强松耦合，以便直到切点实际匹配时才初始化 advice 对象。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	@Nullable
	private String adviceBeanName;

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private transient volatile Advice advice;

	private transient volatile Object adviceMonitor = new Object();


	/**
	 * 指定此 advisor 应引用的 advice bean 的名称。
	 * <p>首次访问此 advisor 的 advice 时，将获取指定 bean 的一个实例。
	 * 此 advisor 最多只会获取该 advice bean 的一个实例，
	 * 并在 advisor 的生命周期内缓存该实例。
	 * @see #getAdvice()
	 */
	public void setAdviceBeanName(@Nullable String adviceBeanName) {
		this.adviceBeanName = adviceBeanName;
	}

	/**
	 * 返回此 advisor 所引用的 advice bean 的名称（如果有）。
	 */
	@Nullable
	public String getAdviceBeanName() {
		return this.adviceBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		resetAdviceMonitor();
	}

	private void resetAdviceMonitor() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			this.adviceMonitor = new Object();
		}
	}

	/**
	 * 直接指定目标 advice 的特定实例，
	 * 避免在 {@link #getAdvice()} 中延迟解析。
	 * @since 3.1
	 */
	public void setAdvice(Advice advice) {
		synchronized (this.adviceMonitor) {
			this.advice = advice;
		}
	}

	@Override
	public Advice getAdvice() {
		Advice advice = this.advice;
		if (advice != null) {
			return advice;
		}

		Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
		Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

		if (this.beanFactory.isSingleton(this.adviceBeanName)) {
			// 依赖工厂提供的单例语义。
			advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
			this.advice = advice;
			return advice;
		}
		else {
			// 工厂没有单例保证 -> 在本地加锁，
			// 但复用工厂的单例锁，以防我们的 advice bean 的某个延迟依赖
			// 恰好隐式触发单例锁...
			synchronized (this.adviceMonitor) {
				advice = this.advice;
				if (advice == null) {
					advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
					this.advice = advice;
				}
				return advice;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": advice ");
		if (this.adviceBeanName != null) {
			sb.append("bean '").append(this.adviceBeanName).append('\'');
		}
		else {
			sb.append(this.advice);
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化，只在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化 transient 字段。
		resetAdviceMonitor();
	}

}
