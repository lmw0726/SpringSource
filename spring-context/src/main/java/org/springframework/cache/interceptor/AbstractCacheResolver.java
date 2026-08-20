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

package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个基础的 {@link CacheResolver} 实现，要求具体实现根据调用上下文
 * 提供缓存名称的集合。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class AbstractCacheResolver implements CacheResolver, InitializingBean {

	@Nullable
	private CacheManager cacheManager;


	/**
	 * 构造一个新的 {@code AbstractCacheResolver}。
	 * @see #setCacheManager
	 */
	protected AbstractCacheResolver() {
	}

	/**
	 * 为给定的 {@link CacheManager} 构造一个新的 {@code AbstractCacheResolver}。
	 * @param cacheManager 要使用的 CacheManager
	 */
	protected AbstractCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置此实例应使用的 {@link CacheManager}。
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回此实例使用的 {@link CacheManager}。
	 */
	public CacheManager getCacheManager() {
		Assert.state(this.cacheManager != null, "No CacheManager set");
		return this.cacheManager;
	}

	@Override
	public void afterPropertiesSet()  {
		Assert.notNull(this.cacheManager, "CacheManager is required");
	}


	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		Collection<String> cacheNames = getCacheNames(context);
		if (cacheNames == null) {
			return Collections.emptyList();
		}
		Collection<Cache> result = new ArrayList<>(cacheNames.size());
		for (String cacheName : cacheNames) {
			Cache cache = getCacheManager().getCache(cacheName);
			if (cache == null) {
				throw new IllegalArgumentException("Cannot find cache named '" +
						cacheName + "' for " + context.getOperation());
			}
			result.add(cache);
		}
		return result;
	}

	/**
	 * 提供要针对当前缓存管理器解析的缓存名称。
	 * <p>可以返回 {@code null} 来表示本次调用无法解析任何缓存。
	 * @param context 特定调用的上下文
	 * @return 要解析的缓存名称，如果没有缓存需要解析则返回 {@code null}
	 */
	@Nullable
	protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
