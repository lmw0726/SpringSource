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

package org.springframework.core.type.classreading;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link MetadataReaderFactory} 接口的缓存实现，
 * 为每个 Spring {@link Resource} 句柄（即每个 ".class" 文件）
 * 缓存一个 {@link MetadataReader} 实例。
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.5
 */
public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/** 本地 MetadataReader 缓存的默认最大条目数：256。 */
	public static final int DEFAULT_CACHE_LIMIT = 256;

	/** MetadataReader 缓存：可以是本地缓存，也可以是 ResourceLoader 级别共享缓存。 */
	@Nullable
	private Map<Resource, MetadataReader> metadataReaderCache;


	/**
	 * 为默认类加载器创建一个新的 CachingMetadataReaderFactory，
	 * 使用本地资源缓存。
	 */
	public CachingMetadataReaderFactory() {
		super();
		setCacheLimit(DEFAULT_CACHE_LIMIT);
	}

	/**
	 * 为指定的 {@link ClassLoader} 创建一个新的 CachingMetadataReaderFactory，
	 * 使用本地资源缓存。
	 * @param classLoader 要使用的 ClassLoader
	 */
	public CachingMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		super(classLoader);
		setCacheLimit(DEFAULT_CACHE_LIMIT);
	}

	/**
	 * 为指定的 {@link ResourceLoader} 创建一个新的 CachingMetadataReaderFactory，
	 * 如果支持则使用共享资源缓存，否则使用本地资源缓存。
	 * @param resourceLoader 要使用的 Spring ResourceLoader
	 * （也决定了要使用的 ClassLoader）
	 * @see DefaultResourceLoader#getResourceCache
	 */
	public CachingMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		super(resourceLoader);
		if (resourceLoader instanceof DefaultResourceLoader) {
			this.metadataReaderCache =
					((DefaultResourceLoader) resourceLoader).getResourceCache(MetadataReader.class);
		}
		else {
			setCacheLimit(DEFAULT_CACHE_LIMIT);
		}
	}


	/**
	 * 指定 MetadataReader 缓存的最大条目数。
	 * <p>本地缓存默认是 256，通常共享缓存则没有限制。
	 * 此方法强制使用本地资源缓存，
	 * 即使 {@link ResourceLoader} 支持共享资源缓存。
	 */
	public void setCacheLimit(int cacheLimit) {
		if (cacheLimit <= 0) {
			this.metadataReaderCache = null;
		}
		else if (this.metadataReaderCache instanceof LocalResourceCache) {
			((LocalResourceCache) this.metadataReaderCache).setCacheLimit(cacheLimit);
		}
		else {
			this.metadataReaderCache = new LocalResourceCache(cacheLimit);
		}
	}

	/**
	 * 返回 MetadataReader 缓存的最大条目数。
	 */
	public int getCacheLimit() {
		if (this.metadataReaderCache instanceof LocalResourceCache) {
			return ((LocalResourceCache) this.metadataReaderCache).getCacheLimit();
		}
		else {
			return (this.metadataReaderCache != null ? Integer.MAX_VALUE : 0);
		}
	}


	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		if (this.metadataReaderCache instanceof ConcurrentMap) {
			// 无需同步...
			MetadataReader metadataReader = this.metadataReaderCache.get(resource);
			if (metadataReader == null) {
				metadataReader = super.getMetadataReader(resource);
				this.metadataReaderCache.put(resource, metadataReader);
			}
			return metadataReader;
		}
		else if (this.metadataReaderCache != null) {
			synchronized (this.metadataReaderCache) {
				MetadataReader metadataReader = this.metadataReaderCache.get(resource);
				if (metadataReader == null) {
					metadataReader = super.getMetadataReader(resource);
					this.metadataReaderCache.put(resource, metadataReader);
				}
				return metadataReader;
			}
		}
		else {
			return super.getMetadataReader(resource);
		}
	}

	/**
	 * 清除本地 MetadataReader 缓存（如果存在），移除所有缓存的类元数据。
	 */
	public void clearCache() {
		if (this.metadataReaderCache instanceof LocalResourceCache) {
			synchronized (this.metadataReaderCache) {
				this.metadataReaderCache.clear();
			}
		}
		else if (this.metadataReaderCache != null) {
			// 共享资源缓存 -> 重置为本地缓存。
			setCacheLimit(DEFAULT_CACHE_LIMIT);
		}
	}


	@SuppressWarnings("serial")
	private static class LocalResourceCache extends LinkedHashMap<Resource, MetadataReader> {

		private volatile int cacheLimit;

		public LocalResourceCache(int cacheLimit) {
			super(cacheLimit, 0.75f, true);
			this.cacheLimit = cacheLimit;
		}

		public void setCacheLimit(int cacheLimit) {
			this.cacheLimit = cacheLimit;
		}

		public int getCacheLimit() {
			return this.cacheLimit;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
			return size() > this.cacheLimit;
		}
	}

}
