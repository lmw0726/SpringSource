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

package org.springframework.web.servlet.config.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 辅助创建 {@link MappedInterceptor} 实例。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	/**
	 * 处理拦截器
	 */
	private final HandlerInterceptor interceptor;

	/**
	 * 要包含的路径模式
	 */
	@Nullable
	private List<String> includePatterns;

	/**
	 * 要排序的路径模式
	 */
	@Nullable
	private List<String> excludePatterns;

	/**
	 * 路径匹配器
	 */
	@Nullable
	private PathMatcher pathMatcher;

	/**
	 * 排序值，默认为0
	 */
	private int order = 0;


	/**
	 * 创建一个 {@link InterceptorRegistration} 实例。
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}


	/**
	 * 添加拦截器应包含的 URL 模式。
	 * <p>有关模式语法，请参见 {@link PathPattern}，当启用 {@link PathMatchConfigurer#setPatternParser}
	 * 解析模式时，否则为 {@link AntPathMatcher}。 语法基本相同，{@link PathPattern} 更适合 web 用途且更高效。
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		return addPathPatterns(Arrays.asList(patterns));
	}

	/**
	 * {@link #addPathPatterns(String...)} 的基于列表的变体。
	 *
	 * @since 5.0.3
	 */
	public InterceptorRegistration addPathPatterns(List<String> patterns) {
		// 如果包含模式不为空，则使用已有的包含模式；否则创建一个新的包含模式列表
		this.includePatterns = (this.includePatterns != null ?
				this.includePatterns : new ArrayList<>(patterns.size()));
		// 将提供的模式添加到包含模式列表中
		this.includePatterns.addAll(patterns);
		// 返回当前对象
		return this;
	}

	/**
	 * 添加拦截器应从中排除的 URL 模式。
	 * <p>有关模式语法，请参见 {@link PathPattern}，当启用 {@link PathMatchConfigurer#setPatternParser}
	 * 解析模式时，否则为 {@link AntPathMatcher}。 语法基本相同，{@link PathPattern} 更适合 web 用途且更高效。
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		return excludePathPatterns(Arrays.asList(patterns));
	}

	/**
	 * {@link #excludePathPatterns(String...)} 的基于列表的变体。
	 *
	 * @since 5.0.3
	 */
	public InterceptorRegistration excludePathPatterns(List<String> patterns) {
		// 如果排除模式不为空，则使用已有的排除模式；否则创建一个新的排除模式列表
		this.excludePatterns = (this.excludePatterns != null ?
				this.excludePatterns : new ArrayList<>(patterns.size()));
		// 将提供的模式添加到排除模式列表中
		this.excludePatterns.addAll(patterns);
		// 返回当前对象
		return this;
	}

	/**
	 * 配置用于与包含和排除模式匹配 URL 路径的 PathMatcher。
	 * <p>这是一个高级属性，仅在需要自定义 {@link AntPathMatcher} 或自定义 PathMatcher 时使用。
	 * <p>默认为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong> 设置 {@code PathMatcher} 强制使用字符串模式匹配，即使存在
	 * {@link ServletRequestPathUtils#parseAndCache 解析并缓存的} {@code RequestPath}。
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * 指定要使用的排序位置。默认为 0。
	 *
	 * @since 4.3.23
	 */
	public InterceptorRegistration order(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回要使用的排序位置。
	 */
	protected int getOrder() {
		return this.order;
	}

	/**
	 * 构建底层的拦截器。如果提供了 URL 模式，则返回类型为 {@link MappedInterceptor}；否则返回 {@link HandlerInterceptor}。
	 */
	protected Object getInterceptor() {

		// 如果包含模式和排除模式均为空，则直接返回拦截器
		if (this.includePatterns == null && this.excludePatterns == null) {
			return this.interceptor;
		}

		// 创建映射拦截器，使用提供的包含模式和排除模式
		MappedInterceptor mappedInterceptor = new MappedInterceptor(
				StringUtils.toStringArray(this.includePatterns),
				StringUtils.toStringArray(this.excludePatterns),
				this.interceptor);

		// 如果路径匹配器不为空，则设置映射拦截器的路径匹配器
		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}

		// 返回映射拦截器
		return mappedInterceptor;
	}

}
