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

package org.springframework.web.reactive.config;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.resource.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助注册资源解析器和转换器的类。
 *
 * <p>通过此类可以注册资源解析器和转换器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceChainRegistration {

	/**
	 * 默认缓存名称
	 */
	private static final String DEFAULT_CACHE_NAME = "spring-resource-chain-cache";

	/**
	 * 是否存在WebJarsAssetLocator类
	 */
	private static final boolean isWebJarsAssetLocatorPresent = ClassUtils.isPresent(
			"org.webjars.WebJarAssetLocator", ResourceChainRegistration.class.getClassLoader());

	/**
	 * 资源解析器列表，初始容量为4
	 */
	private final List<ResourceResolver> resolvers = new ArrayList<>(4);

	/**
	 * 资源转换器列表，初始容量为4
	 */
	private final List<ResourceTransformer> transformers = new ArrayList<>(4);

	/**
	 * 是否存在版本解析器
	 */
	private boolean hasVersionResolver;

	/**
	 * 是否存在路径解析器
	 */
	private boolean hasPathResolver;

	/**
	 * 是否存在CSS链接转换器
	 */
	private boolean hasCssLinkTransformer;

	/**
	 * 是否存在Webjars解析器
	 */
	private boolean hasWebjarsResolver;


	/**
	 * 构造函数，用于初始化资源链注册。
	 *
	 * @param cacheResources 是否缓存资源
	 */
	public ResourceChainRegistration(boolean cacheResources) {
		this(cacheResources, cacheResources ? new ConcurrentMapCache(DEFAULT_CACHE_NAME) : null);
	}

	/**
	 * 构造函数，用于初始化资源链注册。
	 *
	 * @param cacheResources 是否缓存资源
	 * @param cache          缓存实例
	 */
	public ResourceChainRegistration(boolean cacheResources, @Nullable Cache cache) {
		Assert.isTrue(!cacheResources || cache != null, "'cache' is required when cacheResources=true");
		// 如果缓存资源被启用
		if (cacheResources) {
			// 添加缓存资源解析器和缓存资源转换器到对应的列表中
			this.resolvers.add(new CachingResourceResolver(cache));
			this.transformers.add(new CachingResourceTransformer(cache));
		}
	}


	/**
	 * 添加资源解析器到资源链中。
	 *
	 * @param resolver 要添加的解析器
	 * @return 当前实例，支持链式方法调用
	 */
	public ResourceChainRegistration addResolver(ResourceResolver resolver) {
		// 断言提供的资源解析器不为空
		Assert.notNull(resolver, "The provided ResourceResolver should not be null");

		// 将提供的资源解析器添加到解析器列表中
		this.resolvers.add(resolver);

		// 检查资源解析器的类型并设置对应的标志位
		if (resolver instanceof VersionResourceResolver) {
			this.hasVersionResolver = true;
		} else if (resolver instanceof PathResourceResolver) {
			this.hasPathResolver = true;
		} else if (resolver instanceof WebJarsResourceResolver) {
			this.hasWebjarsResolver = true;
		}
		// 返回当前实例
		return this;
	}

	/**
	 * 添加资源转换器到资源链中。
	 *
	 * @param transformer 要添加的转换器
	 * @return 当前实例，支持链式方法调用
	 */
	public ResourceChainRegistration addTransformer(ResourceTransformer transformer) {
		// 断言提供的资源转换器不为空
		Assert.notNull(transformer, "The provided ResourceTransformer should not be null");

		// 将提供的资源转换器添加到转换器列表中
		this.transformers.add(transformer);

		// 检查资源转换器的类型并设置对应的标志位
		if (transformer instanceof CssLinkResourceTransformer) {
			this.hasCssLinkTransformer = true;
		}
		// 返回当前实例
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
			// 创建结果列表并复制现有的解析器列表
			List<ResourceResolver> result = new ArrayList<>(this.resolvers);

			// 如果存在 WebJarsAssetLocator 但没有 WebJars 解析器，则添加 WebJars 解析器
			if (isWebJarsAssetLocatorPresent && !this.hasWebjarsResolver) {
				result.add(new WebJarsResourceResolver());
			}

			// 最后添加路径资源解析器
			result.add(new PathResourceResolver());

			// 返回包含新解析器的结果列表
			return result;
		}

		// 如果已经存在路径解析器，则直接返回现有的解析器列表
		return this.resolvers;
	}

	/**
	 * 获取资源转换器列表。
	 *
	 * @return 资源转换器列表
	 */
	protected List<ResourceTransformer> getResourceTransformers() {
		// 如果存在版本解析器但不存在 CSS 链接转换器
		if (this.hasVersionResolver && !this.hasCssLinkTransformer) {
			// 创建结果列表并复制现有的转换器列表
			List<ResourceTransformer> result = new ArrayList<>(this.transformers);

			// 检查当前转换器列表是否非空，并且第一个转换器是否为缓存资源转换器
			boolean hasTransformers = !this.transformers.isEmpty();
			boolean hasCaching = hasTransformers && this.transformers.get(0) instanceof CachingResourceTransformer;

			// 将 CSS 链接转换器添加到结果列表中
			// 如果存在缓存转换器，则在其后添加 CSS 链接转换器，否则将其添加到列表开头
			result.add(hasCaching ? 1 : 0, new CssLinkResourceTransformer());

			// 返回包含新转换器的结果列表
			return result;
		}

		// 如果不存在版本解析器或已经存在 CSS 链接转换器，则直接返回现有的转换器列表
		return this.transformers;
	}

}
