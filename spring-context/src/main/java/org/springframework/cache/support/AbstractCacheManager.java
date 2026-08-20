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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 实现通用 {@link CacheManager} 方法的抽象基类。
 * 适用于底层缓存不会发生变化的"静态"环境。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private volatile Set<String> cacheNames = Collections.emptySet();


	// 启动时的早期缓存初始化

	@Override
	public void afterPropertiesSet() {
		initializeCaches();
	}

	/**
	 * 初始化缓存的静态配置。
	 * <p>通过 {@link #afterPropertiesSet()} 在启动时触发；
	 * 也可以在运行时调用以重新初始化。
	 * @since 4.2.2
	 * @see #loadCaches()
	 */
	public void initializeCaches() {
		Collection<? extends Cache> caches = loadCaches();

		synchronized (this.cacheMap) {
			this.cacheNames = Collections.emptySet();
			this.cacheMap.clear();
			Set<String> cacheNames = new LinkedHashSet<>(caches.size());
			for (Cache cache : caches) {
				String name = cache.getName();
				this.cacheMap.put(name, decorateCache(cache));
				cacheNames.add(name);
			}
			this.cacheNames = Collections.unmodifiableSet(cacheNames);
		}
	}

	/**
	 * 为该缓存管理器加载初始缓存。
	 * <p>在启动时由 {@link #afterPropertiesSet()} 调用。
	 * 返回的集合可以为空，但不能为 {@code null}。
	 */
	protected abstract Collection<? extends Cache> loadCaches();


	// 访问时的延迟缓存初始化

	@Override
	@Nullable
	public Cache getCache(String name) {
		// 快速检查是否已存在缓存...
		Cache cache = this.cacheMap.get(name);
		if (cache != null) {
			return cache;
		}

		// 提供者可能支持按需创建缓存...
		Cache missingCache = getMissingCache(name);
		if (missingCache != null) {
			// 现在完全同步以进行缺失缓存的注册
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = decorateCache(missingCache);
					this.cacheMap.put(name, cache);
					updateCacheNames(name);
				}
			}
		}
		return cache;
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.cacheNames;
	}


	// 子类的通用缓存初始化委托

	/**
	 * 检查是否存在指定名称的已注册缓存。
	 * 与 {@link #getCache(String)} 不同，此方法不会触发
	 * 通过 {@link #getMissingCache(String)} 延迟创建缺失缓存。
	 * @param name 缓存标识符（不能为 {@code null}）
	 * @return 关联的 Cache 实例，如果未找到则返回 {@code null}
	 * @since 4.1
	 * @see #getCache(String)
	 * @see #getMissingCache(String)
	 */
	@Nullable
	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
	}

	/**
	 * 向该管理器动态注册一个额外的缓存。
	 * @param cache 要注册的 Cache
	 * @deprecated 从 Spring 4.3 开始，推荐使用 {@link #getMissingCache(String)}
	 */
	@Deprecated
	protected final void addCache(Cache cache) {
		String name = cache.getName();
		synchronized (this.cacheMap) {
			if (this.cacheMap.put(name, decorateCache(cache)) == null) {
				updateCacheNames(name);
			}
		}
	}

	/**
	 * 用指定名称更新暴露的 {@link #cacheNames} 集合。
	 * <p>此方法始终在完整的 {@link #cacheMap} 锁内调用，
	 * 其行为类似于 {@code CopyOnWriteArraySet}，
	 * 保持顺序但以不可变引用的形式暴露。
	 * @param name 要添加的缓存名称
	 */
	private void updateCacheNames(String name) {
		Set<String> cacheNames = new LinkedHashSet<>(this.cacheNames);
		cacheNames.add(name);
		this.cacheNames = Collections.unmodifiableSet(cacheNames);
	}


	// 可覆盖的缓存初始化模板方法

	/**
	 * 在必要时装饰给定的 Cache 对象。
	 * @param cache 要添加到此 CacheManager 的 Cache 对象
	 * @return 装饰后要使用的 Cache 对象，
	 * 或默认情况下直接返回传入的 Cache 对象
	 */
	protected Cache decorateCache(Cache cache) {
		return cache;
	}

	/**
	 * 返回指定 {@code name} 的缺失缓存，如果该缓存不存在
	 * 或无法按需创建则返回 {@code null}。
	 * <p>如果原生提供者支持，缓存可以在运行时延迟创建。
	 * 如果按名称查找没有返回结果，{@code AbstractCacheManager}
	 * 的子类有机会在运行时注册这样的缓存。返回的缓存将自动添加到
	 * 此缓存管理器中。
	 * @param name 要检索的缓存名称
	 * @return 缺失的缓存，如果不存在或无法按需创建则返回 {@code null}
	 * @since 4.1
	 * @see #getCache(String)
	 */
	@Nullable
	protected Cache getMissingCache(String name) {
		return null;
	}

}
