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

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 用于创建 {@link javax.management.ObjectName} 实例的辅助类。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.management.ObjectName#getInstance(String)
 */
public final class ObjectNameManager {

	private ObjectNameManager() {
	}


	/**
	 * 获取与提供的名称对应的 {@code ObjectName} 实例。
	 * @param objectName {@code ObjectName} 格式的 {@code ObjectName} 对象
	 * @return {@code ObjectName} 实例
	 * @throws MalformedObjectNameException 在对象名称规范无效时抛出
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(Object objectName) throws MalformedObjectNameException {
		if (objectName instanceof ObjectName) {
			return (ObjectName) objectName;
		}
		if (!(objectName instanceof String)) {
			throw new MalformedObjectNameException("Invalid ObjectName value type [" +
					objectName.getClass().getName() + "]: only ObjectName and String supported.");
		}
		return getInstance((String) objectName);
	}

	/**
	 * 获取与提供的名称对应的 {@code ObjectName} 实例。
	 * @param objectName {@code String} 格式的 {@code ObjectName} 对象
	 * @return {@code ObjectName} 实例
	 * @throws MalformedObjectNameException 在对象名称规范无效时抛出
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(String objectName) throws MalformedObjectNameException {
		return ObjectName.getInstance(objectName);
	}

	/**
	 * 获取指定域以及具有提供的键和值的单个属性的 {@code ObjectName} 实例。
	 * @param domainName {@code ObjectName} 的域名称
	 * @param key {@code ObjectName} 中单个属性的键
	 * @param value {@code ObjectName} 中单个属性的值
	 * @return {@code ObjectName} 实例
	 * @throws MalformedObjectNameException 在对象名称规范无效时抛出
	 * @see ObjectName#ObjectName(String, String, String)
	 * @see ObjectName#getInstance(String, String, String)
	 */
	public static ObjectName getInstance(String domainName, String key, String value)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, key, value);
	}

	/**
	 * 获取指定域名称以及提供的键/值属性的 {@code ObjectName} 实例。
	 * @param domainName {@code ObjectName} 的域名称
	 * @param properties {@code ObjectName} 的属性
	 * @return {@code ObjectName} 实例
	 * @throws MalformedObjectNameException 在对象名称规范无效时抛出
	 * @see ObjectName#ObjectName(String, java.util.Hashtable)
	 * @see ObjectName#getInstance(String, java.util.Hashtable)
	 */
	public static ObjectName getInstance(String domainName, Hashtable<String, String> properties)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, properties);
	}

}
