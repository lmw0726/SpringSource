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

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.StringUtils;

/**
 * {@link CacheResult} 操作的 {@link JCacheOperation} 实现。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheResult
 */
class CacheResultOperation extends AbstractJCacheKeyOperation<CacheResult> {

	private final ExceptionTypeFilter exceptionTypeFilter;

	@Nullable
	private final CacheResolver exceptionCacheResolver;

	@Nullable
	private final String exceptionCacheName;


	public CacheResultOperation(CacheMethodDetails<CacheResult> methodDetails, CacheResolver cacheResolver,
			KeyGenerator keyGenerator, @Nullable CacheResolver exceptionCacheResolver) {

		super(methodDetails, cacheResolver, keyGenerator);

		CacheResult ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.cachedExceptions(), ann.nonCachedExceptions());
		this.exceptionCacheResolver = exceptionCacheResolver;
		this.exceptionCacheName = (StringUtils.hasText(ann.exceptionCacheName()) ? ann.exceptionCacheName() : null);
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定是否无论缓存是否命中都始终调用该方法。
	 * 默认情况下，仅在缓存未命中时调用该方法。
	 * @see javax.cache.annotation.CacheResult#skipGet()
	 */
	public boolean isAlwaysInvoked() {
		return getCacheAnnotation().skipGet();
	}

	/**
	 * 返回用于解析与此操作抛出的异常匹配的缓存的 {@link CacheResolver} 实例。
	 */
	@Nullable
	public CacheResolver getExceptionCacheResolver() {
		return this.exceptionCacheResolver;
	}

	/**
	 * 返回用于缓存异常的缓存名称，如果应禁用异常缓存则返回 {@code null}。
	 * @see javax.cache.annotation.CacheResult#exceptionCacheName()
	 */
	@Nullable
	public String getExceptionCacheName() {
		return this.exceptionCacheName;
	}

}
