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

package org.springframework.web.cors;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 URL 路径模式选择请求的 {@code CorsConfiguration} 的 {@code CorsConfigurationSource} 实现。
 *
 * <p>模式匹配可以使用 {@link PathMatcher} 或预解析的 {@link PathPattern} 来完成。后者在语法上更适合 Web 使用且更高效。
 * 选择取决于 {@link UrlPathHelper#resolveAndCacheLookupPath 解析并缓存的 String lookupPath} 或
 * {@link ServletRequestPathUtils#parseAndCache 解析并缓存的 RequestPath}，如果没有解析到，
 * 则回退到 {@link PathMatcher}，但此回退可以被禁用。详情请参见 {@link #setAllowInitLookupPath(boolean)}。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see PathPattern
 * @see AntPathMatcher
 * @since 4.2
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {
	/**
	 * 默认的路径匹配器——Ant风格的路径匹配器
	 */
	private static PathMatcher defaultPathMatcher = new AntPathMatcher();

	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser patternParser;

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = defaultPathMatcher;

	/**
	 * 查找的路径属性名称
	 */
	@Nullable
	private String lookupPathAttributeName;

	/**
	 * 是否禁用延迟初始化，并在尚未解析时失败
	 */
	private boolean allowInitLookupPath = true;

	/**
	 * 路径模式 —— 跨域配置映射
	 */
	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();


	/**
	 * 使用 {@link PathPatternParser#defaultInstance} 的默认构造函数。
	 */
	public UrlBasedCorsConfigurationSource() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * 使用 {@link PathPatternParser} 解析模式的构造函数。
	 *
	 * @param parser 使用的解析器
	 * @since 5.3
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser parser) {
		Assert.notNull(parser, "PathPatternParser must not be null");
		this.patternParser = parser;
	}


	/**
	 * 设置配置的 {@code UrlPathHelper} 上相同属性的快捷方式
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath}。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link #setUrlPathHelper(UrlPathHelper)}（如果有的话）。
	 * 有关详细信息，请参见 {@link #setAllowInitLookupPath(boolean)}。
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		// 初始化路径助手
		initUrlPathHelper();
		// 设置是否始终使用完整路径
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置配置的 {@code UrlPathHelper} 上相同属性的快捷方式
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode}。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link #setUrlPathHelper(UrlPathHelper)}（如果有的话）。
	 * 有关详细信息，请参见 {@link #setAllowInitLookupPath(boolean)}。
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
		// 初始化路径助手
		initUrlPathHelper();
		// 设置是否使用URL解码
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置配置的 {@code UrlPathHelper} 上相同属性的快捷方式
	 * {@link org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent}。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link #setUrlPathHelper(UrlPathHelper)}（如果有的话）。
	 * 有关详细信息，请参见 {@link #setAllowInitLookupPath(boolean)}。
	 */
	@Deprecated
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		// 初始化路径助手
		initUrlPathHelper();
		// 设置是否删除分号内容
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	private void initUrlPathHelper() {
		if (this.urlPathHelper == UrlPathHelper.defaultInstance) {
			// 如果URL路径助手不是默认的路径助手，则初始化URL路径助手
			this.urlPathHelper = new UrlPathHelper();
		}
	}

	/**
	 * 配置 {@code UrlPathHelper} 以解析查找路径。如果期望预先解析查找路径或者使用解析后的
	 * {@code PathPatterns}，则可能不需要此配置。有关详细信息，请参见
	 * {@link #setAllowInitLookupPath(boolean)}。
	 * <p>默认情况下，使用 {@link UrlPathHelper#defaultInstance}。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 当启用时，如果既没有已解析的查找路径，也没有已解析的 {@code RequestPath}，则使用
	 * {@link #setUrlPathHelper 配置的} {@code UrlPathHelper} 来解析查找路径。
	 * 这将确定是使用 {@link PathMatcher} 还是使用解析后的 {@link PathPattern} 进行
	 * URL 模式匹配。
	 * <p>在 Spring MVC 中，{@code DispatcherServlet} 处理中始终可用已解析的查找路径
	 * 或解析后的 {@code RequestPath}。但在 Servlet {@code Filter}（如 {@code CorsFilter}）
	 * 中，情况可能不同。
	 * <p>默认情况下，此选项设置为 {@code true}，允许延迟查找路径初始化。如果应用程序使用解析后的
	 * {@code PathPatterns}，请将此设置为 {@code false}，在这种情况下，{@code RequestPath}
	 * 可以通过 {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 提前解析。
	 *
	 * @param allowInitLookupPath 是否禁用延迟初始化，并在尚未解析时失败
	 * @since 5.3
	 */
	public void setAllowInitLookupPath(boolean allowInitLookupPath) {
		this.allowInitLookupPath = allowInitLookupPath;
	}

	/**
	 * 配置持有通过 {@link UrlPathHelper#getLookupPathForRequest(HttpServletRequest)} 提取的查找路径的属性的名称。
	 * <p>默认情况下，这是 {@link UrlPathHelper#PATH_ATTRIBUTE}。
	 *
	 * @param name 要检查的请求属性
	 * @since 5.2
	 * @deprecated 自 5.3 起，建议使用 {@link UrlPathHelper#PATH_ATTRIBUTE}。
	 */
	@Deprecated
	public void setLookupPathAttributeName(String name) {
		this.lookupPathAttributeName = name;
	}

	/**
	 * 配置要用于模式匹配的 {@code PathMatcher}。
	 * <p>这是一个高级属性，仅在需要自定义的 {@link AntPathMatcher} 或自定义的 PathMatcher
	 * 时才应使用。
	 * <p>默认情况下，使用 {@link AntPathMatcher}。
	 * <p><strong>注意:</strong> 设置 {@code PathMatcher} 会强制使用字符串模式匹配，
	 * 即使已提供解析后的 {@code RequestPath}。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 设置 CORS 配置映射。
	 * <p>有关模式语法，请参见 {@link AntPathMatcher} 和 {@link PathPattern}，以及类级别的
	 * Javadoc 了解详情，可以使用哪种模式。通常情况下，语法基本相同，{@link PathPattern} 更适合
	 * Web 使用。
	 *
	 * @param corsConfigurations 要使用的映射
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		// 清空跨域配置映射
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			// 如果要使用的映射不为空，则遍历每一个键值，注册跨域配置
			corsConfigurations.forEach(this::registerCorsConfiguration);
		}
	}

	/**
	 * 一次注册一个映射的 {@link #setCorsConfigurations(Map)} 变体。
	 *
	 * @param pattern 映射模式
	 * @param config  用于该模式的 CORS 配置
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void registerCorsConfiguration(String pattern, CorsConfiguration config) {
		this.corsConfigurations.put(this.patternParser.parse(pattern), config);
	}

	/**
	 * 返回所有配置的 CORS 映射。
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		// 创建一个新的 HashMap，用于存储 CORS 配置
		Map<String, CorsConfiguration> result = CollectionUtils.newHashMap(this.corsConfigurations.size());

		// 将每个 CORS 配置添加到结果中
		this.corsConfigurations.forEach((pattern, config) -> result.put(pattern.getPatternString(), config));

		// 返回不可修改的结果
		return Collections.unmodifiableMap(result);
	}


	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		// 解析请求路径
		Object path = resolvePath(request);
		// 判断是否是路径容器
		boolean isPathContainer = (path instanceof PathContainer);
		// 遍历跨域配置映射的键值对
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (match(path, isPathContainer, entry.getKey())) {
				// 如果当前的路径与路径模式匹配，则返回它的跨域配置
				return entry.getValue();
			}
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private Object resolvePath(HttpServletRequest request) {
		// 如果允许初始化查找路径且请求中没有缓存的路径，则执行以下操作
		if (this.allowInitLookupPath && !ServletRequestPathUtils.hasCachedPath(request)) {
			// 如果查找路径属性名不为空，则使用 URL路径助手 获取请求的查找路径
			return (this.lookupPathAttributeName != null ?
					this.urlPathHelper.getLookupPathForRequest(request, this.lookupPathAttributeName) :
					// 否则，直接使用 URL路径助手 获取请求的查找路径
					this.urlPathHelper.getLookupPathForRequest(request));
		}
		// 从请求中获取缓存的查找路径
		Object lookupPath = ServletRequestPathUtils.getCachedPath(request);
		// 如果使用的路径匹配器不是默认的路径匹配器，则将查找路径转换为字符串
		if (this.pathMatcher != defaultPathMatcher) {
			lookupPath = lookupPath.toString();
		}
		// 返回查找路径
		return lookupPath;
	}

	private boolean match(Object path, boolean isPathContainer, PathPattern pattern) {
		// 如果是路径容器，则执行以下操作
		return (isPathContainer ?
				// 使用模式匹配路径容器
				pattern.matches((PathContainer) path) :
				// 否则，使用路径匹配器匹配模式字符串和路径
				this.pathMatcher.match(pattern.getPatternString(), (String) path));
	}

}
