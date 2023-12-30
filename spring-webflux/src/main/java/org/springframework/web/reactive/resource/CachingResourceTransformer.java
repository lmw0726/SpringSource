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

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code CachingResourceTransformer} 是一个 {@link ResourceTransformer}，它检查一个 {@link Cache}，
 * 查看之前是否在缓存中存在已转换的资源，如果找到，则返回该资源，否则委托给解析器链并缓存结果。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CachingResourceTransformer implements ResourceTransformer {

	private static final Log logger = LogFactory.getLog(CachingResourceTransformer.class);

	private final Cache cache;


	public CachingResourceTransformer(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * 返回配置的 {@code Cache}。
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * 对给定的资源进行转换。
	 *
	 * @param exchange         当前的交换对象
	 * @param resource         要转换的资源
	 * @param transformerChain 转换器链
	 * @return 转换后的资源（可能从缓存获取，也可能委托转换器链进行转换）
	 */
	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource resource,
									ResourceTransformerChain transformerChain) {

		// 从缓存中获取资源
		Resource cachedResource = this.cache.get(resource, Resource.class);

		// 如果缓存中存在资源
		if (cachedResource != null) {
			// 如果日志级别允许跟踪信息
			if (logger.isTraceEnabled()) {
				// 记录资源从缓存中解析的日志信息
				logger.trace(exchange.getLogPrefix() + "从缓存中解析到资源");
			}
			// 返回一个包含缓存资源的 Mono
			return Mono.just(cachedResource);
		}

		// 如果缓存中不存在资源，从转换器链中获取资源，并放入缓存中
		return transformerChain.transform(exchange, resource)
				.doOnNext(transformed -> this.cache.put(resource, transformed));
	}

}
