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

package org.springframework.core.io.support;

import org.springframework.core.SpringProperties;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * 加载 {@code java.util.Properties} 的便捷工具方法，
 * 进行输入流的标准处理。
 *
 * <p>如果需要更灵活的属性加载（包括自定义编码选项），
 * 建议使用 PropertiesLoaderSupport 类。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sebastien Deleuze
 * @see PropertiesLoaderSupport
 * @since 2.0
 */
public abstract class PropertiesLoaderUtils {

	private static final String XML_FILE_EXTENSION = ".xml";

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，
	 * 指示 Spring 是否忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	/**
	 * 从给定的 EncodedResource 加载属性，
	 * 可能会为属性文件指定特定编码。
	 *
	 * @see #fillProperties(java.util.Properties, EncodedResource)
	 */
	public static Properties loadProperties(EncodedResource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * 从给定的 EncodedResource 填充指定的 Properties，
	 * 可能会为属性文件指定特定编码。
	 *
	 * @param props    要加载的 Properties 实例
	 * @param resource 要加载的资源
	 * @throws IOException 如果发生 I/O 错误
	 */
	public static void fillProperties(Properties props, EncodedResource resource)
			throws IOException {

		fillProperties(props, resource, ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * 从给定的 EncodedResource 实际加载属性到指定的 Properties 实例中。
	 *
	 * @param props     要加载的 Properties 实例
	 * @param resource  要加载的资源
	 * @param persister 使用的 PropertiesPersister
	 * @throws IOException 如果发生 I/O 错误
	 */
	static void fillProperties(Properties props, EncodedResource resource, PropertiesPersister persister)
			throws IOException {

		InputStream stream = null;
		Reader reader = null;
		try {
			String filename = resource.getResource().getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				if (shouldIgnoreXml) {
					throw new UnsupportedOperationException("XML support disabled");
				}
				stream = resource.getInputStream();
				persister.loadFromXml(props, stream);
			} else if (resource.requiresReader()) {
				reader = resource.getReader();
				persister.load(props, reader);
			} else {
				stream = resource.getInputStream();
				persister.load(props, stream);
			}
		} finally {
			if (stream != null) {
				stream.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * 从给定资源加载属性（采用 ISO-8859-1 编码）。
	 *
	 * @param resource 要加载的资源
	 * @return 填充好的 Properties 实例
	 * @throws IOException 如果加载失败
	 * @see #fillProperties(java.util.Properties, Resource)
	 */
	public static Properties loadProperties(Resource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * 从给定资源填充指定的 Properties（采用 ISO-8859-1 编码）。
	 *
	 * @param props    要填充的 Properties 实例
	 * @param resource 要加载的资源
	 * @throws IOException 如果加载失败
	 */
	public static void fillProperties(Properties props, Resource resource) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			String filename = resource.getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				if (shouldIgnoreXml) {
					throw new UnsupportedOperationException("XML support disabled");
				}
				props.loadFromXML(is);
			} else {
				props.load(is);
			}
		}
	}

	/**
	 * 使用默认类加载器从指定的类路径资源加载所有属性（采用 ISO-8859-1 编码）。
	 * <p>如果类路径中发现多个同名资源，则合并这些属性。
	 *
	 * @param resourceName 类路径资源名称
	 * @return 填充好的 Properties 实例
	 * @throws IOException 如果加载失败
	 */
	public static Properties loadAllProperties(String resourceName) throws IOException {
		return loadAllProperties(resourceName, null);
	}

	/**
	 * 使用给定的类加载器从指定的类路径资源 (以ISO-8859-1编码) 加载所有属性。
	 * 如果在类路径中找到多个同名资源，则
	 * <p> 合并属性。
	 *
	 * @param resourceName 类路径资源的名称
	 * @param classLoader  用于加载的类加载器 (或 {@code null} 使用默认的类加载器)
	 * @return 填充的属性实例
	 * @throws IOException 如果加载失败
	 */
	public static Properties loadAllProperties(String resourceName, @Nullable ClassLoader classLoader) throws IOException {
		Assert.notNull(resourceName, "Resource name must not be null");
		//赋值给局部变量，对classLoaderToUse的修改，不影响参数classLoader
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = ClassUtils.getDefaultClassLoader();
		}
		//如果当前使用的类加载器为空，则从系统类加载器中加载资源，否则从类加载器中获取该资源
		Enumeration<URL> urls = (classLoaderToUse == null ? ClassLoader.getSystemResources(resourceName) : classLoaderToUse.getResources(resourceName));

		Properties props = new Properties();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			URLConnection con = url.openConnection();
			//如有必要，则使用缓存
			ResourceUtils.useCachesIfNecessary(con);
			try (InputStream is = con.getInputStream()) {
				if (resourceName.endsWith(XML_FILE_EXTENSION)) {
					if (shouldIgnoreXml) {
						throw new UnsupportedOperationException("XML support disabled");
					}
					props.loadFromXML(is);
				} else {
					props.load(is);
				}
			}
		}
		return props;
	}

}
