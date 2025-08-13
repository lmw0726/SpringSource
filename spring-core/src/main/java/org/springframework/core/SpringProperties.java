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

package org.springframework.core;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Spring 本地属性的静态持有类，即在 Spring 库级别定义的属性。
 *
 * <p>从 Spring 库类路径根目录读取 {@code spring.properties} 文件，
 * 同时允许通过 {@link #setProperty} 方法以编程方式设置属性。
 * 在检查属性时，会优先检查本地属性，如果本地不存在，则回退到
 * JVM 级别的系统属性（通过 {@link System#getProperty} 检查）。
 *
 * <p>这是一种设置 Spring 相关系统属性的替代方式，例如
 * "spring.getenv.ignore" 和 "spring.beaninfo.ignore"。
 * 特别适用于某些目标平台（如 WebSphere）中 JVM 系统属性被锁定的场景。
 * 参见 {@link #setFlag}，它提供了一种将此类标志本地设置为 "true" 的便捷方式。
 *
 * @author Juergen Hoeller
 * @since 3.2.7
 * @see org.springframework.beans.CachedIntrospectionResults#IGNORE_BEANINFO_PROPERTY_NAME
 * @see org.springframework.context.index.CandidateComponentsIndexLoader#IGNORE_INDEX
 * @see org.springframework.core.env.AbstractEnvironment#IGNORE_GETENV_PROPERTY_NAME
 * @see org.springframework.expression.spel.SpelParserConfiguration#SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
 * @see org.springframework.jdbc.core.StatementCreatorUtils#IGNORE_GETPARAMETERTYPE_PROPERTY_NAME
 * @see org.springframework.jndi.JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME
 * @see org.springframework.objenesis.SpringObjenesis#IGNORE_OBJENESIS_PROPERTY_NAME
 * @see org.springframework.test.context.NestedTestConfiguration#ENCLOSING_CONFIGURATION_PROPERTY_NAME
 * @see org.springframework.test.context.TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
 * @see org.springframework.test.context.cache.ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
 */
public final class SpringProperties {

	private static final String PROPERTIES_RESOURCE_LOCATION = "spring.properties";

	private static final Properties localProperties = new Properties();


	static {
		try {
			ClassLoader cl = SpringProperties.class.getClassLoader();
			URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
			if (url != null) {
				try (InputStream is = url.openStream()) {
					localProperties.load(is);
				}
			}
		}
		catch (IOException ex) {
			System.err.println("Could not load 'spring.properties' file from local classpath: " + ex);
		}
	}


	private SpringProperties() {
	}


	/**
	 * 以编程方式设置本地属性，覆盖 {@code spring.properties} 文件中的条目（如果存在）。
	 * @param key 属性键
	 * @param value 属性值，如果为 {@code null} 则重置该属性
	 */
	public static void setProperty(String key, @Nullable String value) {
		if (value != null) {
			localProperties.setProperty(key, value);
		}
		else {
			localProperties.remove(key);
		}
	}

	/**
	 * 获取指定属性键的属性值，优先检查本地 Spring 属性，
	 * 如果本地不存在，则回退到 JVM 级别的系统属性。
	 * @param key 属性键
	 * @return 对应的属性值，如果未找到则返回 {@code null}
	 */
	@Nullable
	public static String getProperty(String key) {
		String value = localProperties.getProperty(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			}
			catch (Throwable ex) {
				System.err.println("Could not retrieve system property '" + key + "': " + ex);
			}
		}
		return value;
	}

	/**
	 * 以编程方式将本地标志设置为 "true"，覆盖 {@code spring.properties} 文件中的条目（如果存在）。
	 * @param key 属性键
	 */
	public static void setFlag(String key) {
		localProperties.put(key, Boolean.TRUE.toString());
	}

	/**
	 * 获取指定属性键对应的标志。
	 * @param key 属性键
	 * @return 如果属性值为 "true" 则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	public static boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

}
