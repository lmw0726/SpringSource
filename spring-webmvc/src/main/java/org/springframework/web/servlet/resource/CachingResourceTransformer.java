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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 一个实现了ResourceTransformer接口的类，用于检查缓存中是否存在已转换的资源，
 * 如果存在则返回，否则委托给解析器链并将结果保存在缓存中。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CachingResourceTransformer implements ResourceTransformer {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(CachingResourceTransformer.class);

	/**
	 * 缓存对象
	 */
	private final Cache cache;

	/**
	 * 构造函数，接收一个Cache对象作为参数。
	 *
	 * @param cache 缓存对象，不能为空
	 */
	public CachingResourceTransformer(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	/**
	 * 构造函数，接收一个CacheManager对象和一个缓存名称作为参数。
	 *
	 * @param cacheManager 缓存管理器对象
	 * @param cacheName 缓存名称
	 * @throws IllegalArgumentException 如果找不到指定的缓存名称
	 */
	public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
		// 获取缓存
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			// 如果不存在缓存，抛出异常
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * 返回配置的缓存对象。
	 *
	 * @return 缓存对象
	 */
	public Cache getCache() {
		return this.cache;
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		// 从缓存中获取已转换的资源
		Resource transformed = this.cache.get(resource, Resource.class);
		if (transformed != null) {
			logger.trace("Resource resolved from cache");
			return transformed;
		}

		// 如果缓存中没有找到已转换的资源，则委托给解析器链进行转换
		transformed = transformerChain.transform(request, resource);
		// 将转换后的资源保存到缓存中
		this.cache.put(resource, transformed);

		return transformed;
	}

}
