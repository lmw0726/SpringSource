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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Bean 工厂后置处理器，用于记录被 {@link Deprecated @Deprecated} 注解标记的 Bean 的警告信息。
 *
 * @author Arjen Poutsma
 * @since 3.0.3
 */
public class DeprecatedBeanWarner implements BeanFactoryPostProcessor {

	/**
	 * 可供子类使用的日志对象。
	 */
	protected transient Log logger = LogFactory.getLog(getClass());

	/**
	 * 设置要使用的日志器名称。
	 * 该名称将通过 Commons Logging 传递给底层日志实现，
	 * 并根据日志器的配置被解释为日志类别。
	 * <p>可通过此方式指定，将日志写入特定命名的类别，而不是此警告类的默认类别。
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setLoggerName(String loggerName) {
		this.logger = LogFactory.getLog(loggerName);
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (isLogEnabled()) {
			String[] beanNames = beanFactory.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				String nameToLookup = beanName;
				if (beanFactory.isFactoryBean(beanName)) {
					nameToLookup = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
				}
				Class<?> beanType = beanFactory.getType(nameToLookup);
				if (beanType != null) {
					Class<?> userClass = ClassUtils.getUserClass(beanType);
					if (userClass.isAnnotationPresent(Deprecated.class)) {
						BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
						logDeprecatedBean(beanName, beanType, beanDefinition);
					}
				}
			}
		}
	}

	/**
	 * 为被 {@link Deprecated @Deprecated} 注解标记的 Bean 记录警告日志。
	 * @param beanName 被废弃 Bean 的名称
	 * @param beanType 被废弃 Bean 的用户指定类型
	 * @param beanDefinition 被废弃 Bean 的定义
	 */
	protected void logDeprecatedBean(String beanName, Class<?> beanType, BeanDefinition beanDefinition) {
		StringBuilder builder = new StringBuilder();
		builder.append(beanType);
		builder.append(" ['");
		builder.append(beanName);
		builder.append('\'');
		String resourceDescription = beanDefinition.getResourceDescription();
		if (StringUtils.hasLength(resourceDescription)) {
			builder.append(" in ");
			builder.append(resourceDescription);
		}
		builder.append("] has been deprecated");
		writeToLog(builder.toString());
	}

	/**
	 * 将日志实际写入底层日志系统。
	 * <p>默认实现将消息记录在 "warn" 级别。
	 * @param message 要写入的消息
	 */
	protected void writeToLog(String message) {
		logger.warn(message);
	}

	/**
	 * 判断 {@link #logger} 是否启用。
	 * <p>默认情况下，当 "warn" 级别启用时返回 {@code true}。
	 * 子类可以重写此方法以改变日志输出的级别。
	 */
	protected boolean isLogEnabled() {
		return logger.isWarnEnabled();
	}

}
