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

package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import freemarker.cache.TemplateLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 通过 Spring {@link ResourceLoader} 加载的 FreeMarker {@link TemplateLoader} 适配器。
 * 由 {@link FreeMarkerConfigurationFactory} 使用，适用于无法解析为
 * {@link java.io.File} 的资源加载器路径。
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 * @see FreeMarkerConfigurationFactory#setTemplateLoaderPath
 * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
 */
public class SpringTemplateLoader implements TemplateLoader {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ResourceLoader resourceLoader;

	private final String templateLoaderPath;


	/**
	 * 创建一个新的 SpringTemplateLoader。
	 * @param resourceLoader 要使用的 Spring ResourceLoader
	 * @param templateLoaderPath 要使用的模板加载器路径
	 */
	public SpringTemplateLoader(ResourceLoader resourceLoader, String templateLoaderPath) {
		this.resourceLoader = resourceLoader;
		if (!templateLoaderPath.endsWith("/")) {
			templateLoaderPath += "/";
		}
		this.templateLoaderPath = templateLoaderPath;
		if (logger.isDebugEnabled()) {
			logger.debug("SpringTemplateLoader for FreeMarker: using resource loader [" + this.resourceLoader +
					"] and template loader path [" + this.templateLoaderPath + "]");
		}
	}


	@Override
	@Nullable
	public Object findTemplateSource(String name) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for FreeMarker template with name [" + name + "]");
		}
		Resource resource = this.resourceLoader.getResource(this.templateLoaderPath + name);
		return (resource.exists() ? resource : null);
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException {
		Resource resource = (Resource) templateSource;
		try {
			return new InputStreamReader(resource.getInputStream(), encoding);
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find FreeMarker template: " + resource);
			}
			throw ex;
		}
	}

	@Override
	public long getLastModified(Object templateSource) {
		Resource resource = (Resource) templateSource;
		try {
			return resource.lastModified();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not obtain last-modified timestamp for FreeMarker template in " +
						resource + ": " + ex);
			}
			return -1;
		}
	}

	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {
	}

}
