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

package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.spi.ClassLoadHelper;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 将 Quartz 的 {@link ClassLoadHelper} 接口适配到 Spring 的 {@link ResourceLoader} 接口的包装器。
 * 当 SchedulerFactoryBean 在 Spring ApplicationContext 中运行时默认使用。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see SchedulerFactoryBean#setApplicationContext
 */
public class ResourceLoaderClassLoadHelper implements ClassLoadHelper {

	protected static final Log logger = LogFactory.getLog(ResourceLoaderClassLoadHelper.class);

	@Nullable
	private ResourceLoader resourceLoader;


	/**
	 * 为默认的 ResourceLoader 创建一个新的 ResourceLoaderClassLoadHelper。
	 * @see SchedulerFactoryBean#getConfigTimeResourceLoader()
	 */
	public ResourceLoaderClassLoadHelper() {
	}

	/**
	 * 为给定的 ResourceLoader 创建一个新的 ResourceLoaderClassLoadHelper。
	 * @param resourceLoader 要委托的 ResourceLoader
	 */
	public ResourceLoaderClassLoadHelper(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	@Override
	public void initialize() {
		if (this.resourceLoader == null) {
			this.resourceLoader = SchedulerFactoryBean.getConfigTimeResourceLoader();
			if (this.resourceLoader == null) {
				this.resourceLoader = new DefaultResourceLoader();
			}
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Assert.state(this.resourceLoader != null, "ResourceLoaderClassLoadHelper not initialized");
		return ClassUtils.forName(name, this.resourceLoader.getClassLoader());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<? extends T> loadClass(String name, Class<T> clazz) throws ClassNotFoundException {
		return (Class<? extends T>) loadClass(name);
	}

	@Override
	@Nullable
	public URL getResource(String name) {
		Assert.state(this.resourceLoader != null, "ResourceLoaderClassLoadHelper not initialized");
		Resource resource = this.resourceLoader.getResource(name);
		if (resource.exists()) {
			try {
				return resource.getURL();
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not load " + resource);
				}
				return null;
			}
		}
		else {
			return getClassLoader().getResource(name);
		}
	}

	@Override
	@Nullable
	public InputStream getResourceAsStream(String name) {
		Assert.state(this.resourceLoader != null, "ResourceLoaderClassLoadHelper not initialized");
		Resource resource = this.resourceLoader.getResource(name);
		if (resource.exists()) {
			try {
				return resource.getInputStream();
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not load " + resource);
				}
				return null;
			}
		}
		else {
			return getClassLoader().getResourceAsStream(name);
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		Assert.state(this.resourceLoader != null, "ResourceLoaderClassLoadHelper not initialized");
		ClassLoader classLoader = this.resourceLoader.getClassLoader();
		Assert.state(classLoader != null, "No ClassLoader");
		return classLoader;
	}

}
