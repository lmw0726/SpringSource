/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * 允许从属性资源（如属性文件）配置单个bean属性值的配置器。对于针对系统管理员的自定义配置文件，这非常有用，它可以覆盖应用程序上下文中配置的bean属性。
 *
 * <p>在发行版中提供了两个具体的实现：
 * <ul>
 * <li>{@link PropertyOverrideConfigurer}：用于“beanName.property=value”样式的覆盖（将属性文件中的值推送到bean定义中）
 * <li>{@link PropertyPlaceholderConfigurer}：用于替换“${...}”占位符（将属性文件中的值拉取到bean定义中）
 * </ul>
 *
 * <p>在读取属性后，可以通过覆盖{@link #convertPropertyValue}方法对属性值进行转换。例如，可以在处理属性值之前检测到并解密加密的值。
 *
 * @author Juergen Hoeller
 * @see PropertyOverrideConfigurer
 * @see PropertyPlaceholderConfigurer
 * @since 2003-10-02
 */
public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport
		implements BeanFactoryPostProcessor, PriorityOrdered {

	/**
	 * 默认值：与未排序值相同
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * 为排序目的设置此对象的顺序值。
	 *
	 * @see PriorityOrdered
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 对给定的 bean 工厂执行合并、转换和处理属性。
	 *
	 * @throws BeanInitializationException 如果无法加载任何属性
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			// 合并属性文件
			Properties mergedProps = mergeProperties();

			// 如果需要，转换合并后的属性
			convertProperties(mergedProps);

			// 让子类处理属性
			processProperties(beanFactory, mergedProps);
		} catch (IOException ex) {
			// 如果发生 IO 异常，抛出 BeanInitializationException 异常
			throw new BeanInitializationException("Could not load properties", ex);
		}
	}

	/**
	 * 转换给定的合并属性，必要时转换属性值。然后将处理结果。
	 * 默认实现将为每个属性值调用 convertPropertyValue，用转换后的值替换原始值。
	 *
	 * @param props 要转换的属性
	 * @see #processProperties
	 */
	protected void convertProperties(Properties props) {
		// 获取属性名的枚举
		Enumeration<?> propertyNames = props.propertyNames();
		// 循环处理每个属性
		while (propertyNames.hasMoreElements()) {
			// 获取属性名
			String propertyName = (String) propertyNames.nextElement();
			// 获取属性值
			String propertyValue = props.getProperty(propertyName);
			// 转换属性值
			String convertedValue = convertProperty(propertyName, propertyValue);
			// 如果转换后的值与原始值不相等，则更新属性值
			if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
				props.setProperty(propertyName, convertedValue);
			}
		}
	}

	/**
	 * 将来自属性源的给定属性转换为应该应用的值。
	 * <p>默认实现调用 {@link #convertPropertyValue(String)}。
	 *
	 * @param propertyName  属性的名称，值是为其定义的
	 * @param propertyValue 来自属性源的原始值
	 * @return 转换后的值，用于处理
	 * @see #convertPropertyValue(String)
	 */
	protected String convertProperty(String propertyName, String propertyValue) {
		return convertPropertyValue(propertyValue);
	}

	/**
	 * 将来自属性源的给定属性值转换为应该应用的值。
	 * <p>默认实现简单地返回原始值。
	 * 可以在子类中重写，例如检测加密的值并相应地解密它们。
	 *
	 * @param originalValue 属性源（属性文件或本地“属性”）中的原始值
	 * @return 转换后的值，用于处理
	 * @see #setProperties
	 * @see #setLocations
	 * @see #setLocation
	 * @see #convertProperty(String, String)
	 */
	protected String convertPropertyValue(String originalValue) {
		return originalValue;
	}


	/**
	 * 将给定的属性应用于给定的BeanFactory。
	 *
	 * @param beanFactory 应用程序上下文使用的BeanFactory
	 * @param props       要应用的属性
	 * @throws org.springframework.beans.BeansException 如果出现错误
	 */
	protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException;

}
