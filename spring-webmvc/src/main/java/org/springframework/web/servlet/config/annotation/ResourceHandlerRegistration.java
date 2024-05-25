/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 封装创建资源处理器所需的信息。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author Brian Clozel
 * @since 3.1
 */
public class ResourceHandlerRegistration {

	/**
	 * 路径模式数组
	 */
	private final String[] pathPatterns;

	/**
	 * 静态资源位置列表
	 */
	private final List<String> locationValues = new ArrayList<>();

	/**
	 * 静态资源列表
	 */
	private final List<Resource> locationsResources = new ArrayList<>();

	/**
	 * 缓存周期，单位秒
	 */
	@Nullable
	private Integer cachePeriod;

	/**
	 * 缓存控制
	 */
	@Nullable
	private CacheControl cacheControl;

	/**
	 * 资源链注册器
	 */
	@Nullable
	private ResourceChainRegistration resourceChainRegistration;

	/**
	 * 是否使用最后更新时间戳
	 */
	private boolean useLastModified = true;

	/**
	 * 是否通过启动时的存在性检查优化位置
	 */
	private boolean optimizeLocations = false;


	/**
	 * 创建一个 {@link ResourceHandlerRegistration} 实例。
	 *
	 * @param pathPatterns 一个或多个资源 URL 路径模式
	 */
	public ResourceHandlerRegistration(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.pathPatterns = pathPatterns;
	}


	/**
	 * 添加一个或多个资源位置以从中提供静态内容。
	 * 每个位置都必须指向有效目录。可以指定多个位置作为逗号分隔列表，并按指定顺序检查位置以查找给定资源。
	 * <p>例如，{@code "/"} 和 {@code "classpath:/META-INF/public-web-resources/"}
	 * 允许从 web 应用程序根目录和包含 {@code /META-INF/public-web-resources/} 目录的任何 JAR 提供资源，
	 * web 应用程序根目录中的资源优先。
	 * <p>对于 {@link org.springframework.core.io.UrlResource 基于 URL 的资源}
	 * （例如文件、HTTP URL 等），此方法支持一个特殊前缀来指示 URL 关联的字符集，以便可以正确编码追加的相对路径，
	 * 例如 {@code [charset=Windows-31J]https://example.org/path}。
	 *
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 */
	public ResourceHandlerRegistration addResourceLocations(String... locations) {
		this.locationValues.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * 配置基于预解析 {@code Resource} 引用的资源位置以提供静态资源。
	 *
	 * @param locations 要使用的资源位置
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @since 5.3.3
	 */
	public ResourceHandlerRegistration addResourceLocations(Resource... locations) {
		this.locationsResources.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * 指定资源处理器提供的资源的缓存期限（以秒为单位）。默认情况下不会发送任何缓存标头，而是仅依赖于最后修改的时间戳。
	 * 设置为 0 以发送禁止缓存的缓存标头，或者设置为正数秒以发送具有给定最大年龄值的缓存标头。
	 *
	 * @param cachePeriod 缓存资源的时间，以秒为单位
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * 指定资源处理器应使用的 {@link org.springframework.http.CacheControl}。
	 * <p>在此处设置自定义值将覆盖 {@link #setCachePeriod} 配置。
	 *
	 * @param cacheControl 要使用的 CacheControl 配置
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @since 4.2
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * 设置是否使用 {@link Resource#lastModified()} 信息来驱动 HTTP 响应。
	 * <p>此配置默认设置为 {@code true}。
	 *
	 * @param useLastModified 是否应使用“最后修改”资源信息
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @see ResourceHttpRequestHandler#setUseLastModified
	 * @since 5.3
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}

	/**
	 * 设置是否通过启动时的存在性检查优化指定的位置，预先筛选不存在的目录，以便在每次资源访问时无需检查它们。
	 * <p>默认值为 {@code false}，以防御 zip 文件中没有目录条目的情况，这些目录条目无法预先暴露目录的存在。
	 * 如果 jar 布局一致且有目录条目，请将此标志切换为 {@code true} 以优化访问。
	 *
	 * @param optimizeLocations 是否通过启动时的存在性检查优化位置
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @see ResourceHttpRequestHandler#setOptimizeLocations
	 * @since 5.3.13
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
		return this;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。这可以例如用于对资源 URL 应用版本策略。
	 * <p>如果不调用此方法，默认情况下仅使用简单的 {@link PathResourceResolver} 以匹配配置位置下的 URL 路径到资源。
	 *
	 * @param cacheResources 是否缓存资源解析结果；建议在生产环境中设置为“true”（在开发环境中特别是应用版本策略时设置为“false”）
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。这可以例如用于对资源 URL 应用版本策略。
	 * <p>如果不调用此方法，默认情况下仅使用简单的 {@link PathResourceResolver} 以匹配配置位置下的 URL 路径到资源。
	 *
	 * @param cacheResources 是否缓存资源解析结果；建议在生产环境中设置为“true”（在开发环境中特别是应用版本策略时设置为“false”）
	 * @param cache          用于存储解析和转换资源的缓存；默认情况下使用 {@link org.springframework.cache.concurrent.ConcurrentMapCache}。
	 *                       由于资源不是可序列化的，并且可能依赖于应用程序主机，因此不应使用分布式缓存，而应使用内存缓存。
	 * @return 相同的 {@link ResourceHandlerRegistration} 实例，以进行链式方法调用
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}


	/**
	 * 返回资源处理器的 URL 路径模式。
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * 返回 {@link ResourceHttpRequestHandler} 实例。
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		// 创建一个资源HTTP请求处理器实例
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

		// 如果资源链注册不为空
		if (this.resourceChainRegistration != null) {
			// 设置资源解析器
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			// 设置资源转换器
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}

		// 设置位置值
		handler.setLocationValues(this.locationValues);
		// 设置位置资源
		handler.setLocations(this.locationsResources);

		// 如果缓存控制不为空
		if (this.cacheControl != null) {
			// 设置缓存控制
			handler.setCacheControl(this.cacheControl);
		} else if (this.cachePeriod != null) {
			// 设置缓存秒数
			handler.setCacheSeconds(this.cachePeriod);
		}

		// 设置是否使用最后修改时间
		handler.setUseLastModified(this.useLastModified);
		// 设置是否优化位置
		handler.setOptimizeLocations(this.optimizeLocations);

		// 返回资源HTTP请求处理器
		return handler;
	}

}
