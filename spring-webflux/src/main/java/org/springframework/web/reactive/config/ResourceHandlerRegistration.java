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

package org.springframework.web.reactive.config;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.resource.ResourceWebHandler;

import java.util.*;

/**
 * 辅助创建和配置静态资源处理器。
 *
 * <p>通过此类可以创建和配置静态资源处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceHandlerRegistration {
	/**
	 * 资源加载器
	 */
	private final ResourceLoader resourceLoader;

	/**
	 * 路径模式数组
	 */
	private final String[] pathPatterns;

	/**
	 * 位置值列表
	 */
	private final List<String> locationValues = new ArrayList<>();

	/**
	 * 缓存控制
	 */
	@Nullable
	private CacheControl cacheControl;

	/**
	 * 资源链注册
	 */
	@Nullable
	private ResourceChainRegistration resourceChainRegistration;

	/**
	 * 是否使用最后修改时间，默认为 true
	 */
	private boolean useLastModified = true;

	/**
	 * 是否优化位置，默认为 false
	 */
	private boolean optimizeLocations = false;

	/**
	 * 媒体类型映射
	 */
	@Nullable
	private Map<String, MediaType> mediaTypes;



	/**
	 * 创建一个{@link ResourceHandlerRegistration}实例。
	 *
	 * @param resourceLoader 用于将字符串位置转换为{@link Resource}的资源加载器
	 * @param pathPatterns   一个或多个资源URL路径模式
	 */
	public ResourceHandlerRegistration(ResourceLoader resourceLoader, String... pathPatterns) {
		Assert.notNull(resourceLoader, "ResourceLoader is required");
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling");
		this.resourceLoader = resourceLoader;
		this.pathPatterns = pathPatterns;
	}


	/**
	 * 添加一个或多个资源位置，用于提供静态内容。
	 *
	 * <p>每个位置必须指向有效目录。多个位置可以指定为逗号分隔的列表，并且将按照指定的顺序检查资源。
	 *
	 * <p>例如，{@code "/", "classpath:/META-INF/public-web-resources/"} 允许从Web应用程序根目录和类路径中包含
	 * {@code /META-INF/public-web-resources/} 目录的任何JAR中提供资源，Web应用程序根目录中的资源优先。
	 *
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 */
	public ResourceHandlerRegistration addResourceLocations(String... resourceLocations) {
		this.locationValues.addAll(Arrays.asList(resourceLocations));
		return this;
	}

	/**
	 * 指定资源处理器应使用的 {@link CacheControl}。
	 *
	 * @param cacheControl 要使用的CacheControl配置
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * 设置是否使用 {@link Resource#lastModified()} 信息来驱动HTTP响应。
	 *
	 * <p>此配置默认设置为 {@code true}。
	 *
	 * @param useLastModified 是否应使用“最后修改”资源信息
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 * @see ResourceWebHandler#setUseLastModified
	 * @since 5.3
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}

	/**
	 * 设置是否通过启动时的存在性检查来优化指定的位置，
	 * 通过在启动时过滤不存在的目录，从而不必在每次资源访问时进行检查。
	 *
	 * <p>默认值为 {@code false}，用于防御没有目录条目的zip文件，无法提前暴露目录的存在性。
	 * 将此标志切换为 {@code true} 可以在具有目录条目的一致jar布局情况下获得优化的访问。
	 *
	 * @param optimizeLocations 是否通过启动时的存在性检查来优化位置
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 * @see ResourceWebHandler#setOptimizeLocations
	 * @since 5.3.13
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
		return this;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，可以将版本策略应用于资源URL。
	 *
	 * <p>如果没有调用此方法，默认情况下仅使用简单的 {@code PathResourceResolver}，
	 * 以便将URL路径匹配到配置位置下的资源。
	 *
	 * @param cacheResources 是否缓存资源解析的结果；建议在生产环境中设置为“true”（在开发环境中设置为“false”，
	 *                       特别是应用版本策略时）
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，可以将版本策略应用于资源URL。
	 *
	 * <p>如果未调用此方法，则默认情况下只使用简单的 {@code PathResourceResolver}，
	 * 以便将URL路径匹配到配置位置下的资源。
	 *
	 * @param cacheResources 是否缓存资源解析的结果；
	 *                       建议在生产环境中设置为“true”（在开发环境中设置为“false”，特别是应用版本策略时）
	 * @param cache          用于存储已解析和转换的资源的缓存；
	 *                       默认情况下，使用 {@link org.springframework.cache.concurrent.ConcurrentMapCache}。
	 *                       由于资源不可序列化并且可能依赖于应用程序主机，因此不应使用分布式缓存，而应使用内存缓存。
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，用于链接方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}

	/**
	 * 向静态 {@link Resource} 的文件扩展名和用于响应的媒体类型之间添加映射。
	 *
	 * <p>通常不需要使用此方法，因为还可以通过 {@link MediaTypeFactory#getMediaType(Resource)} 确定映射。
	 *
	 * @param mediaTypes 媒体类型映射
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		this.mediaTypes.clear();
		this.mediaTypes.putAll(mediaTypes);
	}


	/**
	 * 返回资源处理程序的URL路径模式。
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * 返回一个 {@link ResourceWebHandler} 实例。
	 */
	protected ResourceWebHandler getRequestHandler() {
		// 创建 ResourceWebHandler 实例
		ResourceWebHandler handler = new ResourceWebHandler();

		// 设置资源加载器和位置值
		handler.setResourceLoader(this.resourceLoader);
		handler.setLocationValues(this.locationValues);

		// 如果存在资源链注册
		if (this.resourceChainRegistration != null) {
			// 设置资源解析器和资源转换器
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}

		// 如果存在缓存控制
		if (this.cacheControl != null) {
			// 设置缓存控制
			handler.setCacheControl(this.cacheControl);
		}

		// 设置是否使用最后修改时间、是否优化位置
		handler.setUseLastModified(this.useLastModified);
		handler.setOptimizeLocations(this.optimizeLocations);

		// 如果存在媒体类型映射
		if (this.mediaTypes != null) {
			// 设置媒体类型映射
			handler.setMediaTypes(this.mediaTypes);
		}

		// 返回配置完成的 ResourceWebHandler 实例
		return handler;
	}

}
