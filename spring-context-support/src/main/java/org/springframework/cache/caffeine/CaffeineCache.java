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

package org.springframework.cache.caffeine;

import java.util.concurrent.Callable;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.LoadingCache;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 Caffeine {@link com.github.benmanes.caffeine.cache.Cache} 实例的
 * Spring {@link org.springframework.cache.Cache} 适配器实现。
 *
 * <p>需要 Caffeine 2.1 或更高版本。
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.3
 * @see CaffeineCacheManager
 */
public class CaffeineCache extends AbstractValueAdaptingCache {

	private final String name;

	private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;


	/**
	 * 使用指定的名称和给定的内部 {@link com.github.benmanes.caffeine.cache.Cache} 创建 {@link CaffeineCache} 实例。
	 * @param name 缓存的名称
	 * @param cache 底层的 Caffeine Cache 实例
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this(name, cache, true);
	}

	/**
	 * 使用指定的名称和给定的内部 {@link com.github.benmanes.caffeine.cache.Cache} 创建 {@link CaffeineCache} 实例。
	 * @param name 缓存的名称
	 * @param cache 底层的 Caffeine Cache 实例
	 * @param allowNullValues 是否接受并转换此缓存中的 {@code null} 值
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
			boolean allowNullValues) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
	}


	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, final Callable<T> valueLoader) {
		return (T) fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		if (this.cache instanceof LoadingCache) {
			return ((LoadingCache<Object, Object>) this.cache).get(key);
		}
		return this.cache.getIfPresent(key);
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
		PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
		Object result = this.cache.get(key, callable);
		return (callable.called ? null : toValueWrapper(result));
	}

	@Override
	public void evict(Object key) {
		this.cache.invalidate(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return (this.cache.asMap().remove(key) != null);
	}

	@Override
	public void clear() {
		this.cache.invalidateAll();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = !this.cache.asMap().isEmpty();
		this.cache.invalidateAll();
		return notEmpty;
	}


	private class PutIfAbsentFunction implements Function<Object, Object> {

		@Nullable
		private final Object value;

		private boolean called;

		public PutIfAbsentFunction(@Nullable Object value) {
			this.value = value;
		}

		@Override
		public Object apply(Object key) {
			this.called = true;
			return toStoreValue(this.value);
		}
	}


	private class LoadFunction implements Function<Object, Object> {

		private final Callable<?> valueLoader;

		public LoadFunction(Callable<?> valueLoader) {
			this.valueLoader = valueLoader;
		}

		@Override
		public Object apply(Object o) {
			try {
				return toStoreValue(this.valueLoader.call());
			}
			catch (Exception ex) {
				throw new ValueRetrievalException(o, this.valueLoader, ex);
			}
		}
	}

}
