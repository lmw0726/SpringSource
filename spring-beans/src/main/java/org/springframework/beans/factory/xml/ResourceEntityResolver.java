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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * {@code EntityResolver} implementation that tries to resolve entity references
 * through a {@link org.springframework.core.io.ResourceLoader} (usually,
 * relative to the resource base of an {@code ApplicationContext}), if applicable.
 * Extends {@link DelegatingEntityResolver} to also provide DTD and XSD lookup.
 *
 * <p>Allows to use standard XML entities to include XML snippets into an
 * application context definition, for example to split a large XML file
 * into various modules. The include paths can be relative to the
 * application context's resource base as usual, instead of relative
 * to the JVM working directory (the XML parser's default).
 *
 * <p>Note: In addition to relative paths, every URL that specifies a
 * file in the current system root, i.e. the JVM working directory,
 * will be interpreted relative to the application context too.
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @since 31.07.2003
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

	private static final Log logger = LogFactory.getLog(ResourceEntityResolver.class);
	/**
	 * 资源加载器
	 */
	private final ResourceLoader resourceLoader;


	/**
	 * Create a ResourceEntityResolver for the specified ResourceLoader
	 * (usually, an ApplicationContext).
	 *
	 * @param resourceLoader the ResourceLoader (or ApplicationContext)
	 *                       to load XML entity includes with
	 */
	public ResourceEntityResolver(ResourceLoader resourceLoader) {
		super(resourceLoader.getClassLoader());
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {

		InputSource source = super.resolveEntity(publicId, systemId);

		if (source != null || systemId == null) {
			//如果输入来源不是空，且系统ID为空，则返回当前InputSource
			return source;
		}
		String resourcePath = null;
		try {
			String decodedSystemId = URLDecoder.decode(systemId, "UTF-8");
			String givenUrl = new URL(decodedSystemId).toString();
			//获取系统根路径URL，如：file:/E:/backEnd/spring-framework-5.3.21/
			String systemRootUrl = new File("").toURI().toURL().toString();
			// Try relative to resource base if currently in system root.
			//如果当前处于系统根目录，尝试相对于资源库。
			if (givenUrl.startsWith(systemRootUrl)) {
				resourcePath = givenUrl.substring(systemRootUrl.length());
			}
		} catch (Exception ex) {
			//通常是MalformedURLException 或 AccessControlException。
			if (logger.isDebugEnabled()) {
				logger.debug("Could not resolve XML entity [" + systemId + "] against system root URL", ex);
			}
			// 没有URL (或没有可解析的URL) -> 尝试相对于资源库。
			resourcePath = systemId;
		}
		if (resourcePath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Trying to locate XML entity [" + systemId + "] as resource [" + resourcePath + "]");
			}
			//根据资源路径获取资源
			Resource resource = this.resourceLoader.getResource(resourcePath);
			//获取输入流，并包装成InputSource
			source = new InputSource(resource.getInputStream());
			//设置公共ID
			source.setPublicId(publicId);
			//设置系统ID
			source.setSystemId(systemId);
			if (logger.isDebugEnabled()) {
				logger.debug("Found XML entity [" + systemId + "]: " + resource);
			}
		} else if (systemId.endsWith(DTD_SUFFIX) || systemId.endsWith(XSD_SUFFIX)) {
			//如果系统ID以DTD或XSD结尾，则获取DTD或XSD
			//通过https进行外部dtd/xsd查找，即使是规范的http声明
			String url = systemId;
			if (url.startsWith("http:")) {
				url = "https:" + url.substring(5);
			}
			try {
				source = new InputSource(new URL(url).openStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			} catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve XML entity [" + systemId + "] through URL [" + url + "]", ex);
				}
				// 回退到解析器的默认行为。
				source = null;
			}
		}

		return source;
	}


}
