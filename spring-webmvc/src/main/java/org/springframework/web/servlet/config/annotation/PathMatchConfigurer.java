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

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 配置路径匹配选项。这些选项应用于以下内容：
 * <ul>
 * <li>{@link WebMvcConfigurationSupport#requestMappingHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#viewControllerHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#resourceHandlerMapping}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @since 4.0.3
 */
public class PathMatchConfigurer {

	/**
	 * 路径模式解析器
	 */
	@Nullable
	private PathPatternParser patternParser;

	/**
	 * 是否启用后缀模式匹配
	 */
	@Nullable
	private Boolean trailingSlashMatch;

	/**
	 * 路径前缀 - 断言映射
	 */
	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;

	/**
	 * 是否启用后缀模式匹配
	 */
	@Nullable
	private Boolean suffixPatternMatch;

	/**
	 * 是否启用已注册的后缀模式匹配
	 */
	@Nullable
	private Boolean registeredSuffixPatternMatch;

	/**
	 * URL路径助手
	 */
	@Nullable
	private UrlPathHelper urlPathHelper;

	/**
	 * 路径匹配器
	 */
	@Nullable
	private PathMatcher pathMatcher;

	/**
	 * 默认路径模式解析器
	 */
	@Nullable
	private PathPatternParser defaultPatternParser;

	/**
	 * 默认路径模式解析器
	 */
	@Nullable
	private UrlPathHelper defaultUrlPathHelper;

	/**
	 * 默认路径模式解析器
	 */
	@Nullable
	private PathMatcher defaultPathMatcher;


	/**
	 * 启用解析后的 {@link PathPattern}，如
	 * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)} 中所述。
	 * <p><strong>注意：</strong>这与使用 {@link #setUrlPathHelper(UrlPathHelper)}
	 * 和 {@link #setPathMatcher(PathMatcher)} 互斥。
	 * <p>默认情况下不启用此功能。
	 *
	 * @param patternParser 用于预解析模式的解析器
	 * @since 5.3
	 */
	public PathMatchConfigurer setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
		return this;
	}

	/**
	 * 是否匹配 URL，而不管是否存在尾部斜杠。
	 * 如果启用，则映射到 "/users" 的方法也匹配 "/users/"。
	 * <p>默认值是 {@code true}。
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * 配置路径前缀以应用于匹配的控制器方法。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，
	 * 其控制器类型由相应的 {@code Predicate} 匹配。使用第一个匹配的谓词的前缀。
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} 来分组控制器。
	 *
	 * @param prefix    要应用的前缀
	 * @param predicate 用于匹配控制器类型的谓词
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}

	/**
	 * 是否在匹配模式与请求时使用后缀模式匹配（".*"）。
	 * 如果启用，则映射到 "/users" 的方法也匹配 "/users.*"。
	 * <p>默认情况下此设置为 {@code false}。
	 * <p><strong>注意：</strong>当设置了 {@link #setPatternParser(PathPatternParser)} 时，
	 * 此属性互斥且被忽略。
	 *
	 * @deprecated 自 5.2.4 起。请参阅 {@link RequestMappingHandlerMapping} 中关于路径扩展
	 * 配置选项的类级注释。由于此方法没有替代方法，在 5.2.x 中有必要将其设置为 {@code false}。
	 * 在 5.3 中，默认值更改为 {@code false}，并且不再需要使用此属性。
	 */
	@Deprecated
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * 是否仅针对在配置内容协商时显式注册的路径扩展使用后缀模式匹配。
	 * 通常建议这样做以减少歧义并避免在路径中由于其他原因出现 "." 时出现问题。
	 * <p>默认情况下此设置为 "false"。
	 * <p><strong>注意：</strong>当设置了 {@link #setPatternParser(PathPatternParser)} 时，
	 * 此属性互斥且被忽略。
	 *
	 * @deprecated 自 5.2.4 起。请参阅 {@link RequestMappingHandlerMapping} 中关于路径扩展
	 * 配置选项的类级注释。
	 */
	@Deprecated
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * 设置用于解析应用程序映射路径的 UrlPathHelper。
	 * <p><strong>注意：</strong>当设置了 {@link #setPatternParser(PathPatternParser)} 时，
	 * 此属性互斥且被忽略。
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * 设置用于字符串模式匹配的 PathMatcher。
	 * <p>默认情况下这是 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>当设置了 {@link #setPatternParser(PathPatternParser)} 时，
	 * 此属性互斥且被忽略。
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}


	/**
	 * 返回要使用的 {@link PathPatternParser}，如果已配置。
	 *
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	@Nullable
	@Deprecated
	public Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * 是否使用注册的后缀进行模式匹配。
	 *
	 * @deprecated 自 5.2.4 起，请参阅 {@link #setUseRegisteredSuffixPatternMatch(Boolean)} 上的弃用说明。
	 */
	@Nullable
	@Deprecated
	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	/**
	 * 是否使用后缀模式匹配。
	 *
	 * @deprecated 自 5.2.4 起，请参阅 {@link #setUseSuffixPatternMatch(Boolean)} 上的弃用说明。
	 */
	@Nullable
	@Deprecated
	public Boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	@Nullable
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 返回已配置的 UrlPathHelper，否则返回默认的共享实例。
	 *
	 * @since 5.3
	 */
	protected UrlPathHelper getUrlPathHelperOrDefault() {
		// 如果 URL 路径帮助器不为空
		if (this.urlPathHelper != null) {
			// 返回 URL 路径帮助器
			return this.urlPathHelper;
		}
		// 如果默认 URL 路径帮助器为空
		if (this.defaultUrlPathHelper == null) {
			// 创建默认 URL 路径帮助器
			this.defaultUrlPathHelper = new UrlPathHelper();
		}
		// 返回默认 URL 路径帮助器
		return this.defaultUrlPathHelper;
	}

	/**
	 * 返回已配置的 PathMatcher，否则返回默认的共享实例。
	 *
	 * @since 5.3
	 */
	protected PathMatcher getPathMatcherOrDefault() {
		// 如果路径匹配器不为空
		if (this.pathMatcher != null) {
			// 返回路径匹配器
			return this.pathMatcher;
		}
		// 如果默认路径匹配器为空
		if (this.defaultPathMatcher == null) {
			// 创建默认路径匹配器
			this.defaultPathMatcher = new AntPathMatcher();
		}
		// 返回默认路径匹配器
		return this.defaultPathMatcher;
	}

	/**
	 * 返回已配置的 PathPatternParser，否则返回默认的共享实例。
	 *
	 * @since 5.3.4
	 */
	public PathPatternParser getPatternParserOrDefault() {
		// 如果路径模式解析器不为空
		if (this.patternParser != null) {
			// 返回路径模式解析器
			return this.patternParser;
		}
		// 如果默认路径模式解析器为空
		if (this.defaultPatternParser == null) {
			// 创建默认路径模式解析器
			this.defaultPatternParser = new PathPatternParser();
		}
		// 返回默认路径模式解析器
		return this.defaultPatternParser;
	}
}
