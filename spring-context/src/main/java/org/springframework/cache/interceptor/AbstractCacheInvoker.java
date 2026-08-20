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

package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SingletonSupplier;

/**
 * 用于调用 {@link Cache} 操作的基础组件，当发生异常时使用可配置的
 * {@link CacheErrorHandler} 来处理。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheErrorHandler
 */
public abstract class AbstractCacheInvoker {

	protected SingletonSupplier<CacheErrorHandler> errorHandler;


	protected AbstractCacheInvoker() {
		this.errorHandler = SingletonSupplier.of(SimpleCacheErrorHandler::new);
	}

	protected AbstractCacheInvoker(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}


	/**
	 * 设置用于处理缓存提供方抛出的错误的 {@link CacheErrorHandler} 实例。
	 * 默认使用 {@link SimpleCacheErrorHandler}，它会原样抛出任何异常。
	 */
	public void setErrorHandler(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}

	/**
	 * 返回要使用的 {@link CacheErrorHandler}。
	 */
	public CacheErrorHandler getErrorHandler() {
		return this.errorHandler.obtain();
	}


	/**
	 * 在指定的 {@link Cache} 上执行 {@link Cache#get(Object)}，如果发生异常则调用
	 * 错误处理器。如果处理器没有抛出任何异常，则返回 {@code null}，这模拟了出错
	 * 时的缓存未命中（cache miss）情况。
	 * @see Cache#get(Object)
	 */
	@Nullable
	protected Cache.ValueWrapper doGet(Cache cache, Object key) {
		try {
			return cache.get(key);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return null;  // 如果异常已被处理，则返回缓存未命中（cache miss）
		}
	}

	/**
	 * 在指定的 {@link Cache} 上执行 {@link Cache#put(Object, Object)}，如果发生异常
	 * 则调用错误处理器。
	 */
	protected void doPut(Cache cache, Object key, @Nullable Object result) {
		try {
			cache.put(key, result);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCachePutError(ex, cache, key, result);
		}
	}

	/**
	 * 在指定的 {@link Cache} 上执行 {@link Cache#evict(Object)}/{@link Cache#evictIfPresent(Object)}，
	 * 如果发生异常则调用错误处理器。
	 */
	protected void doEvict(Cache cache, Object key, boolean immediate) {
		try {
			if (immediate) {
				cache.evictIfPresent(key);
			}
			else {
				cache.evict(key);
			}
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheEvictError(ex, cache, key);
		}
	}

	/**
	 * 在指定的 {@link Cache} 上执行 {@link Cache#clear()}，如果发生异常则调用错误处理器。
	 */
	protected void doClear(Cache cache, boolean immediate) {
		try {
			if (immediate) {
				cache.invalidate();
			}
			else {
				cache.clear();
			}
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheClearError(ex, cache);
		}
	}

}
