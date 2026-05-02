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

package org.springframework.aop.target;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.ObjectUtils;

/**
 * 基于 Spring {@link org.springframework.beans.factory.BeanFactory} 的
 * {@link org.springframework.aop.TargetSource} 实现的基类，
 * 委托给 Spring 管理的 Bean 实例。
 *
 * <p>子类可以创建原型实例或延迟访问单例目标对象。
 * 参见 {@link LazyInitTargetSource} 和 {@link AbstractPrototypeBasedTargetSource}
 * 的子类以了解具体的策略。
 *
 * <p>基于 BeanFactory 的 TargetSource 是可序列化的。
 * 这涉及断开当前目标对象的连接并转换为 {@link SingletonTargetSource}。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.1.4
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @see LazyInitTargetSource
 * @see PrototypeTargetSource
 * @see ThreadLocalTargetSource
 * @see CommonsPool2TargetSource
 */
public abstract class AbstractBeanFactoryBasedTargetSource implements TargetSource, BeanFactoryAware, Serializable {

	/** 使用 Spring 1.2.7 的 serialVersionUID 以保证互操作性。 */
	private static final long serialVersionUID = -4721607536018568393L;


	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 每次调用时将要创建的目标 Bean 的名称。 */
	private String targetBeanName;

	/** 目标对象的类。 */
	private volatile Class<?> targetClass;

	/**
	 * 拥有此 TargetSource 的 BeanFactory。我们需要持有此引用，
	 * 以便在必要时创建新的原型实例。
	 */
	private BeanFactory beanFactory;


	/**
	 * 设置工厂中目标 Bean 的名称。
	 * <p>目标 Bean 不应该是单例的，否则每次都会从工厂中获取相同的实例，
	 * 导致与 {@link SingletonTargetSource} 提供的行为相同。
	 * @param targetBeanName 拥有此拦截器的 BeanFactory 中目标 Bean 的名称
	 * @see SingletonTargetSource
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 返回工厂中目标 Bean 的名称。
	 */
	public String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * 显式指定目标类，以避免对目标 Bean 的任何形式的访问
	 * （例如，避免初始化 FactoryBean 实例）。
	 * <p>默认是通过 BeanFactory 上的 {@code getType} 调用自动检测类型
	 * （甚至以完整的 {@code getBean} 调用作为回退）。
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 设置拥有的 BeanFactory。我们需要保存引用，
	 * 以便在每次调用时使用 {@code getBean} 方法。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.targetBeanName == null) {
			throw new IllegalStateException("Property 'targetBeanName' is required");
		}
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回拥有的 BeanFactory。
	 */
	public BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public Class<?> getTargetClass() {
		Class<?> targetClass = this.targetClass;
		if (targetClass != null) {
			return targetClass;
		}
		synchronized (this) {
			// 在同步块内进行完整检查，仅进入一次 BeanFactory 交互算法...
			targetClass = this.targetClass;
			if (targetClass == null && this.beanFactory != null) {
				// 确定目标 Bean 的类型。
				targetClass = this.beanFactory.getType(this.targetBeanName);
				if (targetClass == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Getting bean with name '" + this.targetBeanName + "' for type determination");
					}
					Object beanInstance = this.beanFactory.getBean(this.targetBeanName);
					targetClass = beanInstance.getClass();
				}
				this.targetClass = targetClass;
			}
			return targetClass;
		}
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public void releaseTarget(Object target) throws Exception {
		// 此处无需任何操作。
	}


	/**
	 * 从另一个 AbstractBeanFactoryBasedTargetSource 对象复制配置。
	 * 如果子类希望暴露此功能，应重写此方法。
	 * @param other 要从中复制配置的对象
	 */
	protected void copyFrom(AbstractBeanFactoryBasedTargetSource other) {
		this.targetBeanName = other.targetBeanName;
		this.targetClass = other.targetClass;
		this.beanFactory = other.beanFactory;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractBeanFactoryBasedTargetSource otherTargetSource = (AbstractBeanFactoryBasedTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.beanFactory, otherTargetSource.beanFactory) &&
				ObjectUtils.nullSafeEquals(this.targetBeanName, otherTargetSource.targetBeanName));
	}

	@Override
	public int hashCode() {
		int hashCode = getClass().hashCode();
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.beanFactory);
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.targetBeanName);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" for target bean '").append(this.targetBeanName).append('\'');
		if (this.targetClass != null) {
			sb.append(" of type [").append(this.targetClass.getName()).append(']');
		}
		return sb.toString();
	}

}
