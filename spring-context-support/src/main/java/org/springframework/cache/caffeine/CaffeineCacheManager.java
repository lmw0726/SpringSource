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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link CacheManager} 的实现类，为每个 {@link #getCache} 请求延迟构建 {@link CaffeineCache}
 * 实例。同时支持"静态"模式，通过 {@link #setCacheNames} 预定义缓存名称集合，
 * 运行时不再动态创建额外的缓存区域。
 *
 * <p>底层缓存的配置可以通过 {@link Caffeine} 构建器或 {@link CaffeineSpec} 进行微调，
 * 并通过 {@link #setCaffeine}/{@link #setCaffeineSpec} 传入此 CacheManager。
 * 符合 {@link CaffeineSpec} 规范的表达式值也可以通过
 * {@link #setCacheSpecification "cacheSpecification"} Bean 属性来设置。
 *
 * <p>要求 Caffeine 2.1 或更高版本。
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.3
 * @see CaffeineCache
 */
public class CaffeineCacheManager implements CacheManager {

	private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

	@Nullable
	private CacheLoader<Object, Object> cacheLoader;

	private boolean allowNullValues = true;

	private boolean dynamic = true;

	private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private final Collection<String> customCacheNames = new CopyOnWriteArrayList<>();


	/**
	 * 构造一个动态的 CaffeineCacheManager，
	 * 在请求时延迟创建缓存实例。
	 */
	public CaffeineCacheManager() {
	}

	/**
	 * 构造一个静态的 CaffeineCacheManager，
	 * 仅管理指定缓存名称对应的缓存。
	 */
	public CaffeineCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * 设置此 CacheManager 的"静态"模式下使用的缓存名称集合。
	 * <p>调用此方法后，缓存的数量和名称将被固定，
	 * 运行时不再创建额外的缓存区域。
	 * <p>以 {@code null} 集合作为参数调用此方法会将模式重置为"动态"，
	 * 允许再次动态创建缓存。
	 */
	public void setCacheNames(@Nullable Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createCaffeineCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * 设置用于构建每个 {@link CaffeineCache} 实例的 Caffeine 对象。
	 * @see #createNativeCaffeineCache
	 * @see com.github.benmanes.caffeine.cache.Caffeine#build()
	 */
	public void setCaffeine(Caffeine<Object, Object> caffeine) {
		Assert.notNull(caffeine, "Caffeine must not be null");
		doSetCaffeine(caffeine);
	}

	/**
	 * 设置用于构建每个 {@link CaffeineCache} 实例的 {@link CaffeineSpec} 对象。
	 * @see #createNativeCaffeineCache
	 * @see com.github.benmanes.caffeine.cache.Caffeine#from(CaffeineSpec)
	 */
	public void setCaffeineSpec(CaffeineSpec caffeineSpec) {
		doSetCaffeine(Caffeine.from(caffeineSpec));
	}

	/**
	 * 设置用于构建每个 {@link CaffeineCache} 实例的 Caffeine 缓存规范字符串。
	 * 给定的值需要符合 Caffeine 的 {@link CaffeineSpec} 规范（参见其 javadoc）。
	 * @see #createNativeCaffeineCache
	 * @see com.github.benmanes.caffeine.cache.Caffeine#from(String)
	 */
	public void setCacheSpecification(String cacheSpecification) {
		doSetCaffeine(Caffeine.from(cacheSpecification));
	}

	private void doSetCaffeine(Caffeine<Object, Object> cacheBuilder) {
		if (!ObjectUtils.nullSafeEquals(this.cacheBuilder, cacheBuilder)) {
			this.cacheBuilder = cacheBuilder;
			refreshCommonCaches();
		}
	}

	/**
	 * 设置用于构建每个 {@link CaffeineCache} 实例的 Caffeine CacheLoader，
	 * 将其转变为 LoadingCache。
	 * @see #createNativeCaffeineCache
	 * @see com.github.benmanes.caffeine.cache.Caffeine#build(CacheLoader)
	 * @see com.github.benmanes.caffeine.cache.LoadingCache
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader)) {
			this.cacheLoader = cacheLoader;
			refreshCommonCaches();
		}
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的 {@code null} 值。
	 * <p>默认值为 "true"，尽管 Caffeine 本身不支持 {@code null} 值。
	 * 将使用内部持有对象来存储用户级别的 {@code null} 值。
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (this.allowNullValues != allowNullValues) {
			this.allowNullValues = allowNullValues;
			refreshCommonCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否接受并转换其所有缓存的 {@code null} 值。
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		return this.cacheMap.computeIfAbsent(name, cacheName ->
				this.dynamic ? createCaffeineCache(cacheName) : null);
	}


	/**
	 * 将给定的原生 Caffeine Cache 实例注册到此缓存管理器，
	 * 将其适配为 Spring 的缓存 API，以便通过 {@link #getCache} 进行访问。
	 * 可以并行注册任意数量的自定义缓存。
	 * <p>这允许为每个缓存设置自定义配置（而不是所有缓存共享
	 * 缓存管理器配置中的通用设置），通常与 Caffeine 构建器 API 一起使用：
	 * {@code registerCustomCache("myCache", Caffeine.newBuilder().maximumSize(10).build())}
	 * <p>请注意，其他缓存——无论是通过 {@link #setCacheNames} 静态指定的，
	 * 还是按需动态构建的——仍然使用缓存管理器配置中的通用设置。
	 * @param name 缓存名称
	 * @param cache 要注册的自定义 Caffeine Cache 实例
	 * @since 5.2.8
	 * @see #adaptCaffeineCache
	 */
	public void registerCustomCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this.customCacheNames.add(name);
		this.cacheMap.put(name, adaptCaffeineCache(name, cache));
	}

	/**
	 * 将给定的新原生 Caffeine Cache 实例适配为 Spring 的 {@link Cache}
	 * 抽象，用于指定的缓存名称。
	 * @param name 缓存名称
	 * @param cache 原生 Caffeine Cache 实例
	 * @return Spring CaffeineCache 适配器（或其装饰器）
	 * @since 5.2.8
	 * @see CaffeineCache
	 * @see #isAllowNullValues()
	 */
	protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		return new CaffeineCache(name, cache, isAllowNullValues());
	}

	/**
	 * 为指定的缓存名称构建通用的 {@link CaffeineCache} 实例，
	 * 使用此缓存管理器上指定的通用 Caffeine 配置。
	 * <p>委托给 {@link #adaptCaffeineCache} 作为适配到 Spring 缓存抽象的方法
	 * （允许集中装饰等），并传入一个新构建的原生 Caffeine Cache 实例。
	 * @param name 缓存名称
	 * @return Spring CaffeineCache 适配器（或其装饰器）
	 * @see #adaptCaffeineCache
	 * @see #createNativeCaffeineCache
	 */
	protected Cache createCaffeineCache(String name) {
		return adaptCaffeineCache(name, createNativeCaffeineCache(name));
	}

	/**
	 * 为指定的缓存名称构建通用的 Caffeine Cache 实例，
	 * 使用此缓存管理器上指定的通用 Caffeine 配置。
	 * @param name 缓存名称
	 * @return 原生 Caffeine Cache 实例
	 * @see #createCaffeineCache
	 */
	protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
		return (this.cacheLoader != null ? this.cacheBuilder.build(this.cacheLoader) : this.cacheBuilder.build());
	}

	/**
	 * 使用此管理器的当前状态重新创建通用缓存。
	 */
	private void refreshCommonCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			if (!this.customCacheNames.contains(entry.getKey())) {
				entry.setValue(createCaffeineCache(entry.getKey()));
			}
		}
	}

}
