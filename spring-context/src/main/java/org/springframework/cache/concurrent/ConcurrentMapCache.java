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

package org.springframework.cache.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于核心 JDK {@code java.util.concurrent} 包的简单 {@link org.springframework.cache.Cache} 实现。
 *
 * <p>适用于测试或简单的缓存场景，通常与
 * {@link org.springframework.cache.support.SimpleCacheManager} 配合使用，
 * 或通过 {@link ConcurrentMapCacheManager} 动态使用。
 *
 * <p><b>注意：</b>由于 {@link ConcurrentHashMap}（默认使用的实现）
 * 不允许存储 {@code null} 值，本类会将 {@code null} 值替换为预定义的内部对象。
 * 该行为可以通过 {@link #ConcurrentMapCache(String, ConcurrentMap, boolean)} 构造函数进行更改。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 * @see ConcurrentMapCacheManager
 */
public class ConcurrentMapCache extends AbstractValueAdaptingCache {

	private final String name;

	private final ConcurrentMap<Object, Object> store;

	@Nullable
	private final SerializationDelegate serialization;


	/**
	 * 使用指定名称创建一个新的 ConcurrentMapCache。
	 * @param name 缓存的名称
	 */
	public ConcurrentMapCache(String name) {
		this(name, new ConcurrentHashMap<>(256), true);
	}

	/**
	 * 使用指定名称创建一个新的 ConcurrentMapCache。
	 * @param name 缓存的名称
	 * @param allowNullValues 是否接受并转换此缓存的 {@code null} 值
	 */
	public ConcurrentMapCache(String name, boolean allowNullValues) {
		this(name, new ConcurrentHashMap<>(256), allowNullValues);
	}

	/**
	 * 使用指定名称和给定的内部 {@link ConcurrentMap} 创建一个新的 ConcurrentMapCache。
	 * @param name 缓存的名称
	 * @param store 用作内部存储的 ConcurrentMap
	 * @param allowNullValues 是否允许 {@code null} 值
	 * （将其适配为内部 null 持有者值）
	 */
	public ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store, boolean allowNullValues) {
		this(name, store, allowNullValues, null);
	}

	/**
	 * 使用指定名称和给定的内部 {@link ConcurrentMap} 创建一个新的 ConcurrentMapCache。
	 * 如果指定了 {@link SerializationDelegate}，则启用
	 * {@link #isStoreByValue() store-by-value}（按值存储）。
	 * @param name 缓存的名称
	 * @param store 用作内部存储的 ConcurrentMap
	 * @param allowNullValues 是否允许 {@code null} 值
	 * （将其适配为内部 null 持有者值）
	 * @param serialization 用于序列化缓存条目的 {@link SerializationDelegate}，
	 * 或传 {@code null} 以存储引用
	 * @since 4.3
	 */
	protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store,
			boolean allowNullValues, @Nullable SerializationDelegate serialization) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(store, "Store must not be null");
		this.name = name;
		this.store = store;
		this.serialization = serialization;
	}


	/**
	 * 返回此缓存是存储每个条目的副本（{@code true}）还是
	 * 存储引用（{@code false}，默认值）。如果启用了按值存储，
	 * 缓存中的每个条目都必须是可序列化的。
	 * @since 4.3
	 */
	public final boolean isStoreByValue() {
		return (this.serialization != null);
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final ConcurrentMap<Object, Object> getNativeCache() {
		return this.store;
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		return this.store.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) fromStoreValue(this.store.computeIfAbsent(key, k -> {
			try {
				return toStoreValue(valueLoader.call());
			}
			catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
		}));
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.store.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		Object existing = this.store.putIfAbsent(key, toStoreValue(value));
		return toValueWrapper(existing);
	}

	@Override
	public void evict(Object key) {
		this.store.remove(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return (this.store.remove(key) != null);
	}

	@Override
	public void clear() {
		this.store.clear();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = !this.store.isEmpty();
		this.store.clear();
		return notEmpty;
	}

	@Override
	protected Object toStoreValue(@Nullable Object userValue) {
		Object storeValue = super.toStoreValue(userValue);
		if (this.serialization != null) {
			try {
				return this.serialization.serializeToByteArray(storeValue);
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to serialize cache value '" + userValue +
						"'. Does it implement Serializable?", ex);
			}
		}
		else {
			return storeValue;
		}
	}

	@Override
	protected Object fromStoreValue(@Nullable Object storeValue) {
		if (storeValue != null && this.serialization != null) {
			try {
				return super.fromStoreValue(this.serialization.deserializeFromByteArray((byte[]) storeValue));
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to deserialize cache value '" + storeValue + "'", ex);
			}
		}
		else {
			return super.fromStoreValue(storeValue);
		}
	}

}
