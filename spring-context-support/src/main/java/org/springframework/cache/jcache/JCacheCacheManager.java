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

package org.springframework.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.CacheManager} 实现，
 * 由 JCache {@link CacheManager javax.cache.CacheManager} 提供支持。
 *
 * <p>注意：自 Spring 4.0 起，此类已针对 JCache 1.0 进行了更新。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 * @see JCacheCache
 */
public class JCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	@Nullable
	private CacheManager cacheManager;

	private boolean allowNullValues = true;


	/**
	 * 创建一个新的 {@code JCacheCacheManager}，不指定底层 JCache
	 * {@link CacheManager javax.cache.CacheManager}。
	 * <p>底层 JCache {@code javax.cache.CacheManager} 可以通过
	 * {@link #setCacheManager} bean 属性来设置。
	 */
	public JCacheCacheManager() {
	}

	/**
	 * 为给定的底层 JCache {@link CacheManager javax.cache.CacheManager}
	 * 创建一个新的 {@code JCacheCacheManager}。
	 * @param cacheManager 底层 JCache {@code javax.cache.CacheManager}
	 */
	public JCacheCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置底层 JCache {@link CacheManager javax.cache.CacheManager}。
	 */
	public void setCacheManager(@Nullable CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回底层 JCache {@link CacheManager javax.cache.CacheManager}。
	 */
	@Nullable
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的 {@code null} 值。
	 * <p>默认值为 "true"，尽管 JSR-107 本身不支持 {@code null} 值。
	 * 将使用内部的 holder 对象来存储用户级别的 {@code null} 值。
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	/**
	 * 返回此缓存管理器是否接受并转换其所有缓存的 {@code null} 值。
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(Caching.getCachingProvider().getCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		Collection<Cache> caches = new LinkedHashSet<>();
		for (String cacheName : cacheManager.getCacheNames()) {
			javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(cacheName);
			caches.add(new JCacheCache(jcache, isAllowNullValues()));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		// 再次检查 JCache 缓存（以防缓存在运行时被添加）
		javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(name);
		if (jcache != null) {
			return new JCacheCache(jcache, isAllowNullValues());
		}
		return null;
	}

}
