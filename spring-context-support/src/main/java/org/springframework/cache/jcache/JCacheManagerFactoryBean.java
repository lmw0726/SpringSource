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

package org.springframework.cache.jcache;

import java.net.URI;
import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * JCache {@link CacheManager javax.cache.CacheManager} 的 {@link FactoryBean} 实现，
 * 通过标准 JCache {@link Caching javax.cache.Caching} 类按名称获取预定义的 {@code CacheManager}。
 *
 * <p>注意：自 Spring 4.0 起，此类已针对 JCache 1.0 进行了更新。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see javax.cache.Caching#getCachingProvider()
 * @see javax.cache.spi.CachingProvider#getCacheManager()
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	@Nullable
	private URI cacheManagerUri;

	@Nullable
	private Properties cacheManagerProperties;

	@Nullable
	private ClassLoader beanClassLoader;

	@Nullable
	private CacheManager cacheManager;


	/**
	 * 指定所需 {@code CacheManager} 的 URI。
	 * <p>默认值为 {@code null}（即使用 JCache 的默认值）。
	 */
	public void setCacheManagerUri(@Nullable URI cacheManagerUri) {
		this.cacheManagerUri = cacheManagerUri;
	}

	/**
	 * 指定待创建的 {@code CacheManager} 的属性。
	 * <p>默认值为 {@code null}（即不应用任何特殊属性）。
	 * @see javax.cache.spi.CachingProvider#getCacheManager(URI, ClassLoader, Properties)
	 */
	public void setCacheManagerProperties(@Nullable Properties cacheManagerProperties) {
		this.cacheManagerProperties = cacheManagerProperties;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.cacheManager = Caching.getCachingProvider().getCacheManager(
				this.cacheManagerUri, this.beanClassLoader, this.cacheManagerProperties);
	}


	@Override
	@Nullable
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.cacheManager != null) {
			this.cacheManager.close();
		}
	}

}
