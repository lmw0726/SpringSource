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

import java.util.Collection;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 一个简单的 {@link CacheResolver} 实现，根据可配置的 {@link CacheManager}
 * 以及 {@link BasicOperation#getCacheNames() getCacheNames()} 提供的一个或多个缓存名称
 * 来解析对应的 {@link Cache} 实例。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see BasicOperation#getCacheNames()
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

	/**
	 * 构造一个新的 {@code SimpleCacheResolver}。
	 * @see #setCacheManager
	 */
	public SimpleCacheResolver() {
	}

	/**
	 * 为给定的 {@link CacheManager} 构造一个新的 {@code SimpleCacheResolver}。
	 * @param cacheManager 要使用的 CacheManager
	 */
	public SimpleCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
	}


	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return context.getOperation().getCacheNames();
	}


	/**
	 * 为给定的 {@link CacheManager} 返回一个 {@code SimpleCacheResolver}。
	 * @param cacheManager CacheManager（可能为 {@code null}）
	 * @return SimpleCacheResolver（如果 CacheManager 为 {@code null}，则返回 {@code null}）
	 * @since 5.1
	 */
	@Nullable
	static SimpleCacheResolver of(@Nullable CacheManager cacheManager) {
		return (cacheManager != null ? new SimpleCacheResolver(cacheManager) : null);
	}

}
