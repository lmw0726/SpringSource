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

package org.springframework.cache.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} 实现，用于暴露一个 EhCache {@link net.sf.ehcache.CacheManager}
 * 实例（独立的或共享的），根据指定的配置位置进行配置。
 *
 * <p>如果未指定配置位置，则将从类路径根目录下的 "ehcache.xml" 配置 CacheManager
 * （即应用默认的 EhCache 初始化方式——如 EhCache 文档中所述）。
 *
 * <p>在使用 EhCacheFactoryBean 时，也建议单独设置一个 EhCacheManagerFactoryBean，
 * 因为它提供了一个（默认）独立的 CacheManager 实例，并负责正确关闭 CacheManager。
 * EhCacheManagerFactoryBean 对于从非默认配置位置加载 EhCache 配置也是必需的。
 *
 * <p>注意：从 Spring 5.0 开始，Spring 的 EhCache 支持要求 EhCache 2.10 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 1.1.1
 * @see #setConfigLocation
 * @see #setShared
 * @see EhCacheFactoryBean
 * @see net.sf.ehcache.CacheManager
 */
public class EhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Resource configLocation;

	@Nullable
	private String cacheManagerName;

	private boolean acceptExisting = false;

	private boolean shared = false;

	@Nullable
	private CacheManager cacheManager;

	private boolean locallyManaged = true;


	/**
	 * 设置 EhCache 配置文件的位置。典型值为 "/WEB-INF/ehcache.xml"。
	 * <p>默认值为类路径根目录下的 "ehcache.xml"，如果未找到，
	 * 则使用 EhCache jar 中的 "ehcache-failsafe.xml"（默认 EhCache 初始化）。
	 * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
	 * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置 EhCache CacheManager 的名称（如果需要指定特定名称）。
	 * @see net.sf.ehcache.config.Configuration#setName(String)
	 */
	public void setCacheManagerName(String cacheManagerName) {
		this.cacheManagerName = cacheManagerName;
	}

	/**
	 * 设置是否接受同名的现有 EhCache CacheManager，用于此 EhCacheManagerFactoryBean 的配置。
	 * 默认值为 "false"。
	 * <p>通常与 {@link #setCacheManagerName "cacheManagerName"} 配合使用，
	 * 但即使未指定名称，也会使用默认的 CacheManager 名称正常工作。
	 * 之后，同一 ClassLoader 空间中所有引用相同 CacheManager 名称（或相同默认值）的
	 * 代码都将共享指定的 CacheManager。
	 * @see #setCacheManagerName
	 * @see #setShared
	 * @see net.sf.ehcache.CacheManager#getCacheManager(String)
	 * @see net.sf.ehcache.CacheManager#CacheManager()
	 */
	public void setAcceptExisting(boolean acceptExisting) {
		this.acceptExisting = acceptExisting;
	}

	/**
	 * 设置 EhCache CacheManager 是否应共享（在 ClassLoader 级别作为单例）
	 * 或独立使用（通常在应用程序内部独立）。
	 * 默认值为 "false"，即创建一个独立的本地实例。
	 * <p><b>注意：</b>此功能允许将此 EhCacheManagerFactoryBean 的 CacheManager
	 * 与同一 ClassLoader 空间中调用 <code>CacheManager.create()</code> 的任何代码共享，
	 * 无需约定特定的 CacheManager 名称。
	 * 但是，它仅支持涉及单个 EhCacheManagerFactoryBean 的场景，
	 * 该 Bean 将控制底层 CacheManager 的生命周期（特别是其关闭）。
	 * <p>如果同时设置了此标志和 {@link #setAcceptExisting "acceptExisting"}，
	 * 则此标志将覆盖后者，因为它表示"更强"的共享模式。
	 * @see #setCacheManagerName
	 * @see #setAcceptExisting
	 * @see net.sf.ehcache.CacheManager#create()
	 * @see net.sf.ehcache.CacheManager#CacheManager()
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing EhCache CacheManager" +
					(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
		}

		Configuration configuration = (this.configLocation != null ?
				EhCacheManagerUtils.parseConfiguration(this.configLocation) : ConfigurationFactory.parseConfiguration());
		if (this.cacheManagerName != null) {
			configuration.setName(this.cacheManagerName);
		}

		if (this.shared) {
			// 传统的 EhCache 单例共享方式...
			// 无法确定我们是实际创建了一个新的 CacheManager
			// 还是只是接收了一个现有的单例引用。
			this.cacheManager = CacheManager.create(configuration);
		}
		else if (this.acceptExisting) {
			// EhCache 2.5+：重用同名的现有 CacheManager。
			// 基本上与 CacheManager.getInstance(String) 中的代码相同，
			// 只是记录了我们正在处理的是否为现有实例。
			synchronized (CacheManager.class) {
				this.cacheManager = CacheManager.getCacheManager(this.cacheManagerName);
				if (this.cacheManager == null) {
					this.cacheManager = new CacheManager(configuration);
				}
				else {
					this.locallyManaged = false;
				}
			}
		}
		else {
			// 如果已存在同名的 CacheManager 则抛出异常...
			this.cacheManager = new CacheManager(configuration);
		}
	}


	@Override
	@Nullable
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<? extends CacheManager> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.cacheManager != null && this.locallyManaged) {
			if (logger.isDebugEnabled()) {
				logger.debug("Shutting down EhCache CacheManager" +
						(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
			}
			this.cacheManager.shutdown();
		}
	}

}
