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

package org.springframework.cache.ehcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 EhCache {@link net.sf.ehcache.CacheManager} 的 CacheManager 实现。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 * @see EhCacheCache
 */
public class EhCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	@Nullable
	private net.sf.ehcache.CacheManager cacheManager;


	/**
	 * 创建一个新的 EhCacheCacheManager，通过 {@link #setCacheManager} bean 属性设置目标 EhCache CacheManager。
	 */
	public EhCacheCacheManager() {
	}

	/**
	 * 为给定的底层 EhCache CacheManager 创建一个新的 EhCacheCacheManager。
	 * @param cacheManager 底层 EhCache {@link net.sf.ehcache.CacheManager}
	 */
	public EhCacheCacheManager(net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置底层 EhCache {@link net.sf.ehcache.CacheManager}。
	 */
	public void setCacheManager(@Nullable net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回底层 EhCache {@link net.sf.ehcache.CacheManager}。
	 */
	@Nullable
	public net.sf.ehcache.CacheManager getCacheManager() {
		return this.cacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(EhCacheManagerUtils.buildCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		net.sf.ehcache.CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		Status status = cacheManager.getStatus();
		if (!Status.STATUS_ALIVE.equals(status)) {
			throw new IllegalStateException(
					"An 'alive' EhCache CacheManager is required - current cache is " + status.toString());
		}

		String[] names = getCacheManager().getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<>(names.length);
		for (String name : names) {
			caches.add(new EhCacheCache(getCacheManager().getEhcache(name)));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		net.sf.ehcache.CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		// 再次检查 EhCache 缓存（以防缓存是在运行时添加的）
		Ehcache ehcache = cacheManager.getEhcache(name);
		if (ehcache != null) {
			return new EhCacheCache(ehcache);
		}
		return null;
	}

}
