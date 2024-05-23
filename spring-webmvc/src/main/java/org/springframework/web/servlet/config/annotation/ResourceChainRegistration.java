/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.resource.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助注册资源解析器和转换器。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ResourceChainRegistration {
	/**
	 * 默认资源链缓存名称
	 */
	private static final String DEFAULT_CACHE_NAME = "spring-resource-chain-cache";

	/**
	 * 是否存在WebJarsAssetLocator
	 */
	private static final boolean isWebJarsAssetLocatorPresent = ClassUtils.isPresent(
			"org.webjars.WebJarAssetLocator", ResourceChainRegistration.class.getClassLoader());

	/**
	 * 资源解析器列表
	 */
	private final List<ResourceResolver> resolvers = new ArrayList<>(4);

	/**
	 * 资源转换器列表
	 */
	private final List<ResourceTransformer> transformers = new ArrayList<>(4);

	/**
	 * 是否有版本解析器
	 */
	private boolean hasVersionResolver;

	/**
	 * 是否有路径解析器
	 */
	private boolean hasPathResolver;

	/**
	 * 是否有CSS链接转换器
	 */
	private boolean hasCssLinkTransformer;

	/**
	 * 是否有Web Jar解析器
	 */
	private boolean hasWebjarsResolver;


	/**
	 * 使用缓存资源的构造函数。
	 *
	 * @param cacheResources 是否缓存资源
	 */
	public ResourceChainRegistration(boolean cacheResources) {
		this(cacheResources, (cacheResources ? new ConcurrentMapCache(DEFAULT_CACHE_NAME) : null));
	}

	/**
	 * 使用缓存资源和自定义缓存的构造函数。
	 *
	 * @param cacheResources 是否缓存资源
	 * @param cache          缓存实例
	 */
	public ResourceChainRegistration(boolean cacheResources, @Nullable Cache cache) {
		Assert.isTrue(!cacheResources || cache != null, "'cache' is required when cacheResources=true");
		if (cacheResources) {
			// 如果要缓存资源,添加缓存资源解析器
			this.resolvers.add(new CachingResourceResolver(cache));
			// 添加缓存资源转换器
			this.transformers.add(new CachingResourceTransformer(cache));
		}
	}


	/**
	 * 将资源解析器添加到链中。
	 *
	 * @param resolver 要添加的解析器
	 * @return 当前实例以进行链式方法调用
	 */
	public ResourceChainRegistration addResolver(ResourceResolver resolver) {
		Assert.notNull(resolver, "The provided ResourceResolver should not be null");
		// 将解析器添加到解析器列表中
		this.resolvers.add(resolver);
		if (resolver instanceof VersionResourceResolver) {
			// 如果解析器是版本资源解析器的实例，设置 hasVersionResolver 为 true
			this.hasVersionResolver = true;
		} else if (resolver instanceof PathResourceResolver) {
			// 如果解析器是路径资源解析器的实例，设置 hasPathResolver 为 true
			this.hasPathResolver = true;
		} else if (resolver instanceof WebJarsResourceResolver) {
			// 如果解析器是 WebJars 资源解析器的实例，设置 hasWebjarsResolver 为 true
			this.hasWebjarsResolver = true;
		}
		// 返回当前对象
		return this;
	}

	/**
	 * 将资源转换器添加到链中。
	 *
	 * @param transformer 要添加的转换器
	 * @return 当前实例以进行链式方法调用
	 */
	public ResourceChainRegistration addTransformer(ResourceTransformer transformer) {
		Assert.notNull(transformer, "The provided ResourceTransformer should not be null");
		// 将转换器添加到转换器列表中
		this.transformers.add(transformer);
		if (transformer instanceof CssLinkResourceTransformer) {
			// 如果转换器是 CSS 链接资源转换器的实例，设置 hasCssLinkTransformer 为 true
			this.hasCssLinkTransformer = true;
		}
		// 返回当前对象
		return this;
	}

	/**
	 * 获取资源解析器列表。
	 *
	 * @return 资源解析器列表
	 */
	protected List<ResourceResolver> getResourceResolvers() {
		// 如果没有路径解析器
		if (!this.hasPathResolver) {
			// 使用 现有的解析器 创建一个新的解析器列表
			List<ResourceResolver> result = new ArrayList<>(this.resolvers);
			// 如果存在 WebJars 资源定位器且没有 WebJars 解析器
			if (isWebJarsAssetLocatorPresent && !this.hasWebjarsResolver) {
				// 添加一个新的 WebJars 资源解析器
				result.add(new WebJarsResourceResolver());
			}
			// 添加一个新的路径资源解析器
			result.add(new PathResourceResolver());
			// 返回新的解析器列表
			return result;
		}
		// 返回现有的解析器列表
		return this.resolvers;
	}

	/**
	 * 获取资源转换器列表。
	 *
	 * @return 资源转换器列表
	 */
	protected List<ResourceTransformer> getResourceTransformers() {
		// 如果存在版本解析器且没有 CSS 链接转换器
		if (this.hasVersionResolver && !this.hasCssLinkTransformer) {
			// 使用 现有的转换器 创建一个新的转换器列表
			List<ResourceTransformer> result = new ArrayList<>(this.transformers);
			// 检查是否已有转换器
			boolean hasTransformers = !this.transformers.isEmpty();
			// 检查第一个转换器是否是缓存资源转换器
			boolean hasCaching = hasTransformers && this.transformers.get(0) instanceof CachingResourceTransformer;
			// 在适当位置添加 CSS 链接资源转换器
			result.add(hasCaching ? 1 : 0, new CssLinkResourceTransformer());
			// 返回新的转换器列表
			return result;
		}
		// 返回现有的转换器列表
		return this.transformers;
	}

}
