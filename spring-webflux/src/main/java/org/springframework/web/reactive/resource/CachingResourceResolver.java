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

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 从缓存中解析资源或委托给解析器链，并缓存结果的 {@link ResourceResolver}。
 *
 * <p>此类从缓存中获取已解析的资源或 URL 路径，如果缓存中不存在，它会委托给解析器链并缓存解析的结果。
 *
 * <p>它可以配置支持的内容编码，以用于缓存资源的不同变体。默认情况下，这个类支持 "br" 和 "gzip" 两种编码。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	/**
	 * 用于已解析的资源缓存键的前缀。
	 */
	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	/**
	 * 用于已解析的 URL 路径缓存键的前缀。
	 */
	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";

	/**
	 * 缓存对象
	 */
	private final Cache cache;

	/**
	 * 内容编码列表，包含默认的编码方式
	 */
	private final List<String> contentCodings = new ArrayList<>(EncodedResourceResolver.DEFAULT_CODINGS);


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * 返回配置的 {@code Cache}。
	 *
	 * @return 配置的 {@code Cache}
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * 配置用于缓存资源变体的 {@literal "Accept-Encoding"} 头中支持的内容编码类型。
	 * <p>这里配置的编码类型通常应与 {@link EncodedResourceResolver#setContentCodings(List)} 中配置的相匹配。
	 * <p>默认情况下，此属性基于 {@link EncodedResourceResolver#DEFAULT_CODINGS} 的值设置为 {@literal ["br", "gzip"]}。
	 *
	 * @param codings 一个或多个支持的内容编码类型
	 * @since 5.1
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回支持的内容编码类型的只读列表。
	 *
	 * @return 支持的内容编码类型的只读列表
	 * @since 5.1
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * 解析资源的内部方法。如果缓存中存在已解析的资源，则直接从缓存中获取；否则委托给解析器链进行解析，并将解析结果缓存起来。
	 *
	 * @param exchange    当前的 ServerWebExchange
	 * @param requestPath 要解析的资源路径
	 * @param locations   要查找资源的位置列表
	 * @param chain       资源解析器链
	 * @return 解析的资源，或者空的 Mono 如果未找到资源
	 */

	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		// 计算缓存键值
		String key = computeKey(exchange, requestPath);

		// 从缓存中获取资源
		Resource cachedResource = this.cache.get(key, Resource.class);

		// 如果资源存在于缓存中，则直接返回
		if (cachedResource != null) {
			String logPrefix = exchange != null ? exchange.getLogPrefix() : "";
			logger.trace(logPrefix + "Resource resolved from cache");
			return Mono.just(cachedResource);
		}

		// 否则通过链式处理获取资源，并将结果放入缓存
		return chain.resolveResource(exchange, requestPath, locations)
				.doOnNext(resource -> this.cache.put(key, resource));
	}

	/**
	 * 计算缓存键。
	 *
	 * @param exchange    服务器Web交换对象
	 * @param requestPath 请求路径
	 * @return 计算得到的缓存键
	 */
	protected String computeKey(@Nullable ServerWebExchange exchange, String requestPath) {
		if (exchange != null) {
			String codingKey = getContentCodingKey(exchange);
			if (StringUtils.hasText(codingKey)) {
				return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=" + codingKey;
			}
		}
		return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath;
	}

	/**
	 * 获取内容编码的键。
	 *
	 * @param exchange 服务器Web交换对象
	 * @return 内容编码的键，如果请求头中不包含编码信息则返回 null
	 */
	@Nullable
	private String getContentCodingKey(ServerWebExchange exchange) {
		// 获取请求中的 Accept-Encoding 头部信息
		String header = exchange.getRequest().getHeaders().getFirst("Accept-Encoding");

		// 如果头部信息为空，则返回空值
		if (!StringUtils.hasText(header)) {
			return null;
		}

		// 对头部信息进行处理，提取支持的编码方式并进行排序后返回
		return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
				.map(token -> {
					int index = token.indexOf(';');
					return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase();
				})
				.filter(this.contentCodings::contains)
				.sorted()
				.collect(Collectors.joining(","));
	}


	/**
	 * 解析 URL 路径的内部方法。如果缓存中存在已解析的路径，则直接从缓存中获取；否则委托给解析器链进行解析，并将解析结果缓存起来。
	 *
	 * @param resourceUrlPath 要解析的 URL 路径
	 * @param locations       要查找资源的位置列表
	 * @param chain           资源解析器链
	 * @return 解析的 URL 路径，或者空的 Mono 如果未找到路径
	 */
	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
												  List<? extends Resource> locations, ResourceResolverChain chain) {

		// 构建缓存的键，使用资源路径作为前缀
		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;

		// 从缓存中获取已解析的 URL 路径
		String cachedUrlPath = this.cache.get(key, String.class);

		// 如果已有缓存，则直接返回缓存中的路径
		if (cachedUrlPath != null) {
			logger.trace("Path resolved from cache");
			return Mono.just(cachedUrlPath);
		}

		// 否则，通过链式调用解析资源的 URL 路径，并将解析后的路径放入缓存
		return chain.resolveUrlPath(resourceUrlPath, locations)
				.doOnNext(resolvedPath -> this.cache.put(key, resolvedPath));
	}

}
