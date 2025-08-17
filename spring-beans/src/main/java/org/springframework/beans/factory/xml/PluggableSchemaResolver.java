/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link EntityResolver} 的实现，尝试使用一组映射文件将 schema URL 解析为本地的 {@link ClassPathResource classpath 资源}。
 *
 * <p>默认情况下，该类会在类路径中查找映射文件，使用的模式为：
 * {@code META-INF/spring.schemas}，允许类路径中同时存在多个此类文件。
 *
 * <p>{@code META-INF/spring.schemas} 的格式为属性文件，每一行应为：
 * {@code systemId=schema-location}，其中 {@code schema-location} 也应为类路径中的 schema 文件。
 * 由于 {@code systemId} 通常是 URL，因此必须注意转义 ':' 字符，因为在属性文件中它们被视为分隔符。
 *
 * <p>映射文件的模式可以通过 {@link #PluggableSchemaResolver(ClassLoader, String)} 构造方法覆盖。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class PluggableSchemaResolver implements EntityResolver {

	/**
	 * 定义模式映射的文件的位置。可以存在于多个JAR文件中。
	 */
	public static final String DEFAULT_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";


	private static final Log logger = LogFactory.getLog(PluggableSchemaResolver.class);

	@Nullable
	private final ClassLoader classLoader;

	/**
	 * 模式映射位置
	 */
	private final String schemaMappingsLocation;

	/**
	 * 存储模式URL &rarr; 本地模式路径的映射。
	 */
	@Nullable
	private volatile Map<String, String> schemaMappings;


	/**
	 * 使用默认映射文件模式 "META-INF/spring.schemas" 加载 schema URL → schema 文件位置映射。
	 *
	 * @param classLoader 用于加载的 ClassLoader（可以为 {@code null}，表示使用默认 ClassLoader）
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.schemaMappingsLocation = DEFAULT_SCHEMA_MAPPINGS_LOCATION;
	}

	/**
	 * 使用给定的映射文件模式加载 schema URL → schema 文件位置映射。
	 *
	 * @param classLoader            用于加载的 ClassLoader（可以为 {@code null}，表示使用默认 ClassLoader）
	 * @param schemaMappingsLocation 定义 schema 映射的文件位置（不能为空）
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader, String schemaMappingsLocation) {
		Assert.hasText(schemaMappingsLocation, "'schemaMappingsLocation' must not be empty");
		this.classLoader = classLoader;
		this.schemaMappingsLocation = schemaMappingsLocation;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public id [" + publicId +
					"] and system id [" + systemId + "]");
		}

		if (systemId == null) {
			//如果系统ID为空，返回null
			return null;
		}
		//获取资源位置，映射格式为：
		//http\://www.springframework.org/schema/beans/spring-beans.xsd=org/springframework/beans/factory/xml/spring-beans.xsd
		String resourceLocation = getSchemaMappings().get(systemId);
		if (resourceLocation == null && systemId.startsWith("https:")) {
			//如果位置为空，且以https开头，将https改为http开头，重新获取资源位置
			// Retrieve canonical http schema mapping even for https declaration
			resourceLocation = getSchemaMappings().get("http:" + systemId.substring(6));
		}
		if (resourceLocation == null) {
			//没有在META-INF/spring.schemas中定义，返回null
			return null;
		}
		//根据映射位置获取对应的文件
		Resource resource = new ClassPathResource(resourceLocation, this.classLoader);
		try {
			//获取文件流，并包装成InputSource
			InputSource source = new InputSource(resource.getInputStream());
			//设置公共ID
			source.setPublicId(publicId);
			//设置系统ID
			source.setSystemId(systemId);
			if (logger.isTraceEnabled()) {
				logger.trace("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
			}
			return source;
		} catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find XML schema [" + systemId + "]: " + resource, ex);
			}
		}

		//回退到解析器的默认行为
		return null;
	}


	/**
	 * 懒加载指定的schema映射。
	 */
	private Map<String, String> getSchemaMappings() {
		Map<String, String> schemaMappings = this.schemaMappings;
		if (schemaMappings == null) {
			synchronized (this) {
				schemaMappings = this.schemaMappings;
				if (schemaMappings == null) {
					//双重检测检查schemaMappings是否在多线程环境中被正确的初始化
					if (logger.isTraceEnabled()) {
						logger.trace("Loading schema mappings from [" + this.schemaMappingsLocation + "]");
					}
					try {
						//加载指定位置的schema文件的所有属性
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.schemaMappingsLocation, this.classLoader);
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded schema mappings: " + mappings);
						}
						schemaMappings = new ConcurrentHashMap<>(mappings.size());
						//将所有属性合并到schemaMappings中
						CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
						this.schemaMappings = schemaMappings;
					} catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load schema mappings from location [" + this.schemaMappingsLocation + "]", ex);
					}
				}
			}
		}
		return schemaMappings;
	}


	@Override
	public String toString() {
		return "EntityResolver using schema mappings " + getSchemaMappings();
	}

}
