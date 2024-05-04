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

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.web.servlet.resource.ResourceResolver}
 * ，从 {@link org.springframework.cache.Cache} 解析资源，或者以其他方式委托给解析器链并将结果保存在缓存中。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	/**
	 * 用于解析的资源缓存键的前缀。
	 */
	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	/**
	 * 用于解析的 URL 路径缓存键的前缀。
	 */
	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";

	/**
	 * 缓存
	 */
	private final Cache cache;

	/**
	 * 内容编码列表
	 */
	private final List<String> contentCodings = new ArrayList<>(EncodedResourceResolver.DEFAULT_CODINGS);


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
		// 从缓存管理器中获取缓存
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			// 如果缓存为空，抛出异常
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		// 设置缓存
		this.cache = cache;
	}


	/**
	 * 返回配置的 {@code Cache}。
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * 配置从 {@literal "Accept-Encoding"} 标头中支持的内容编码，用于缓存资源的变体。
	 * <p>此处配置的编码通常预期与 {@link EncodedResourceResolver#setContentCodings(List)} 中配置的编码相匹配。
	 * <p>默认情况下，此属性设置为 {@literal ["br", "gzip"]}，基于 {@link EncodedResourceResolver#DEFAULT_CODINGS} 的值。
	 *
	 * @param codings 支持的一个或多个内容编码
	 * @since 5.1
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回支持的内容编码的只读列表。
	 *
	 * @since 5.1
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		// 计算缓存键
		String key = computeKey(request, requestPath);
		// 从缓存中获取资源
		Resource resource = this.cache.get(key, Resource.class);

		// 如果从缓存中获取到资源
		if (resource != null) {
			// 如果日志级别为TRACE，则记录缓存命中的日志
			if (logger.isTraceEnabled()) {
				logger.trace("Resource resolved from cache");
			}
			// 返回该资源
			return resource;
		}

		// 如果缓存中没有找到资源，则尝试通过资源链解析资源
		resource = chain.resolveResource(request, requestPath, locations);
		// 如果成功解析到资源，则将其放入缓存
		if (resource != null) {
			this.cache.put(key, resource);
		}

		// 返回解析的资源
		return resource;
	}

	protected String computeKey(@Nullable HttpServletRequest request, String requestPath) {
		// 如果请求对象不为null
		if (request != null) {
			// 获取请求对象的内容编码键
			String codingKey = getContentCodingKey(request);
			// 如果内容编码键不为空，则返回带有编码键的缓存键
			if (StringUtils.hasText(codingKey)) {
				return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=" + codingKey;
			}
		}

		// 返回不带编码键的缓存键
		return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath;
	}

	@Nullable
	private String getContentCodingKey(HttpServletRequest request) {
		// 获取请求头中的Accept-Encoding字段的值
		String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);

		// 如果请求头中的Accept-Encoding字段的值为空，则返回null
		if (!StringUtils.hasText(header)) {
			return null;
		}

		// 将Accept-Encoding字段的值按逗号分隔，并转换为流进行处理
		return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
				// 对每个编码进行处理，提取编码名称并转换为小写形式
				.map(token -> {
					// 查找分号的索引位置，如果存在则截取分号之前的部分
					int index = token.indexOf(';');
					return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase();
				})
				// 过滤出在 内容编码列表 中存在的编码
				.filter(this.contentCodings::contains)
				// 对编码进行排序
				.sorted()
				// 将过滤并排序后的编码连接成字符串，用逗号分隔
				.collect(Collectors.joining(","));
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
											List<? extends Resource> locations, ResourceResolverChain chain) {

		// 构建缓存键
		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
		// 从缓存中获取已解析的URL路径
		String resolvedUrlPath = this.cache.get(key, String.class);

		// 如果已解析的URL路径不为null，则直接返回
		if (resolvedUrlPath != null) {
			// 如果日志级别为TRACE，则记录路径解析命中缓存的日志
			if (logger.isTraceEnabled()) {
				logger.trace("Path resolved from cache");
			}
			// 返回已解析的URL路径
			return resolvedUrlPath;
		}

		// 如果缓存中没有找到已解析的URL路径，则尝试通过资源链解析URL路径
		resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
		// 如果成功解析到URL路径，则将其放入缓存
		if (resolvedUrlPath != null) {
			this.cache.put(key, resolvedUrlPath);
		}

		// 返回解析的URL路径
		return resolvedUrlPath;
	}

}
