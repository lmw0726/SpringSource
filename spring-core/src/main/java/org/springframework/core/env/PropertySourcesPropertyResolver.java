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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * {@link PropertyResolver} 的实现，根据底层的一组 {@link PropertySources} 来解析属性值。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 * @since 3.1
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {
	/**
	 * 属性资源
	 */
	@Nullable
	private final PropertySources propertySources;


	/**
	 * 根据给定的属性源创建一个新的解析器。
	 *
	 * @param propertySources 要使用的 {@link PropertySource} 对象集合
	 */
	public PropertySourcesPropertyResolver(@Nullable PropertySources propertySources) {
		this.propertySources = propertySources;
	}


	@Override
	public boolean containsProperty(String key) {
		// 如果属性源不为空，则遍历属性源列表
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				// 检查属性源是否包含指定的属性键，如果是，则返回 true
				if (propertySource.containsProperty(key)) {
					return true;
				}
			}
		}
		// 如果未找到属性，则返回 false
		return false;
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	@Nullable
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	@Nullable
	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				//遍历属性资源
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for key '" + key + "' in PropertySource '" +
							propertySource.getName() + "'");
				}
				//获取属性值
				Object value = propertySource.getProperty(key);
				if (value != null) {
					if (resolveNestedPlaceholders && value instanceof String) {
						//如果需要解析嵌套的占位符，且值是字符串类型
						value = resolveNestedPlaceholders((String) value);
					}
					//记录日志
					logKeyFound(key, propertySource, value);
					//如果有必要，转换类型
					return convertValueIfNecessary(value, targetValueType);
				}
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Could not find key '" + key + "' in any property source");
		}
		return null;
	}

	/**
	 * 记录在给定的 {@link PropertySource} 中找到的给定键，从而得出给定值。
	 * <p> 默认实现使用key和source写入调试日志消息。
	 * 从4.3.3开始，这不再记录该值，以避免意外记录敏感设置。
	 * 子类可以重写此方法来更改日志级别和或日志消息，如果需要，包括属性的值。
	 *
	 * @param key            找到的键
	 * @param propertySource 已在其中找到键的 {@code PropertySource}
	 * @param value          对应的值
	 * @since 4.3.1
	 */
	protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
		if (logger.isDebugEnabled()) {
			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
					"' with value of type " + value.getClass().getSimpleName());
		}
	}

}
