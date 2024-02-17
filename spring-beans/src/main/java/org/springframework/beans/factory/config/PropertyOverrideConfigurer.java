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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于在应用程序上下文定义中覆盖bean属性值的属性资源配置程序。它从属性文件中将值“推送”到bean定义中。
 *
 * <p>配置行应具有以下形式：
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * 示例属性文件：
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * 与PropertyPlaceholderConfigurer不同，原始定义可以对此类bean属性具有默认值或根本没有值。如果覆盖的属性文件没有特定bean属性的条目，则使用默认的上下文定义。
 *
 * <p>请注意，上下文定义<i>不知道</i>被覆盖；因此，当查看XML定义文件时，这并不明显。此外，请注意，指定的覆盖值始终是<i>文字</i>值；它们不会被转换为bean引用。当XML bean定义中的原始值指定bean引用时，也适用于此。
 *
 * <p>如果有多个PropertyOverrideConfigurer为同一bean属性定义不同的值，则<i>最后一个</i>将获胜（由于覆盖机制）。
 *
 * <p>在读取属性后，可以通过覆盖{@code convertPropertyValue}方法来转换属性值。例如，可以在处理它们之前检测并相应地解密加密的值。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 12.03.2003
 * @see #convertPropertyValue
 * @see PropertyPlaceholderConfigurer
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {

	/**
	 * 默认的bean名称分隔符。
	 */
	public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";

	/**
	 * bean名称分隔符
	 */
	private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

	/**
	 * 是否忽略无效的Key
	 */
	private boolean ignoreInvalidKeys = false;

	/**
	 * 包含具有覆盖的bean的名称。
	 */
	private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(16));


	/**
	 * 设置期望的bean名称和属性路径之间的分隔符。
	 * 默认为句点（“.”）。
	 */
	public void setBeanNameSeparator(String beanNameSeparator) {
		this.beanNameSeparator = beanNameSeparator;
	}

	/**
	 * 设置是否忽略无效的键。默认为“false”。
	 * <p>如果忽略无效的键，则不符合'beanName.property'格式的键（或引用无效的bean名称或属性）将仅以调试级别记录日志。
	 * 这允许在属性文件中具有任意其他键。
	 */
	public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
		this.ignoreInvalidKeys = ignoreInvalidKeys;
	}


	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {

		// 获取所有属性名的枚举，并循环处理每个属性名
		for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements();) {
			// 获取属性名
			String key = (String) names.nextElement();
			try {
				// 处理属性键值对
				processKey(beanFactory, key, props.getProperty(key));
			} catch (BeansException ex) {
				// 处理异常情况
				String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
				if (!this.ignoreInvalidKeys) {
					throw new BeanInitializationException(msg, ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug(msg, ex);
				}
			}
		}
	}

	/**
	 * 将给定的键处理为'beanName.property'条目。
	 */
	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value)
			throws BeansException {

		// 查找属性键中分隔符的位置
		int separatorIndex = key.indexOf(this.beanNameSeparator);
		// 如果找不到分隔符，抛出异常
		if (separatorIndex == -1) {
			throw new BeanInitializationException("Invalid key '" + key +
					"': expected 'beanName" + this.beanNameSeparator + "property'");
		}
		// 提取出bean的名称和属性名称
		String beanName = key.substring(0, separatorIndex);
		String beanProperty = key.substring(separatorIndex + 1);
		// 将bean名称添加到beanNames集合中
		this.beanNames.add(beanName);
		// 应用属性值到相应的bean上
		applyPropertyValue(factory, beanName, beanProperty, value);
		// 如果调试日志开启，则记录设置属性值的日志
		if (logger.isDebugEnabled()) {
			logger.debug("Property '" + key + "' set to value [" + value + "]");
		}
	}

	/**
	 * 将给定的属性值应用于相应的 bean。
	 */
	protected void applyPropertyValue(
			ConfigurableListableBeanFactory factory, String beanName, String property, String value) {

		// 获取指定bean名称的BeanDefinition
		BeanDefinition bd = factory.getBeanDefinition(beanName);
		// 初始化一个用于存储最终使用的BeanDefinition的变量
		BeanDefinition bdToUse = bd;
		// 循环遍历原始BeanDefinition链，直到找到最原始的BeanDefinition
		while (bd != null) {
			bdToUse = bd;
			bd = bd.getOriginatingBeanDefinition();
		}
		// 创建一个新的PropertyValue对象，表示要设置的属性及其值
		PropertyValue pv = new PropertyValue(property, value);
		// 设置PropertyValue对象是否为可选属性（是否忽略无效的键）
		pv.setOptional(this.ignoreInvalidKeys);
		// 将PropertyValue对象添加到最终使用的BeanDefinition的属性值集合中
		bdToUse.getPropertyValues().addPropertyValue(pv);
	}


	/**
	 * 此 bean 是否存在属性覆盖？
	 * 只有在至少处理过一次后才有效。
	 * @param beanName 要查询状态的 bean 的名称
	 * @return 是否存在命名 bean 的属性覆盖
	 */
	public boolean hasPropertyOverridesFor(String beanName) {
		return this.beanNames.contains(beanName);
	}

}
