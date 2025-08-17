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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * {@code EntityResolver} 实现，尝试通过 {@link org.springframework.core.io.ResourceLoader}（通常相对于
 * {@code ApplicationContext} 的资源基础路径）解析实体引用（如果适用）。
 * 扩展 {@link DelegatingEntityResolver} 以提供 DTD 和 XSD 查找。
 *
 * <p>允许使用标准 XML 实体将 XML 片段包含到应用上下文定义中，例如将大型 XML 文件拆分为多个模块。
 * 包含路径可以像往常一样相对于应用上下文的资源基础路径，而不是相对于 JVM 工作目录（XML 解析器的默认值）。
 *
 * <p>注意：除了相对路径之外，任何指定当前系统根目录中文件的 URL（即 JVM 工作目录），
 * 也将相对于应用上下文进行解释。
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
	 * 为指定的 ResourceLoader（通常是 ApplicationContext）创建一个 ResourceEntityResolver。
	 *
	 * @param resourceLoader 用于加载 XML 实体包含的 ResourceLoader（或 ApplicationContext）
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
