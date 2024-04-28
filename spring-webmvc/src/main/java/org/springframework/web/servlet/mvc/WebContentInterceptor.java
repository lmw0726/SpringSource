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

package org.springframework.web.servlet.mvc;

import org.springframework.http.CacheControl;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 处理拦截器，检查请求是否支持的方法和所需的会话，并通过应用配置的缓存设置来准备响应。
 *
 * <p>可以通过路径模式为特定URL配置缓存设置，使用{@link #addCacheMapping(CacheControl, String...)}和
 * {@link #setCacheMappings(Properties)}，以及对所有URL的默认设置进行回退使用{@link #setCacheControl(CacheControl)}。
 *
 * <p>可以使用{@link PathMatcher}或解析的{@link PathPattern}进行模式匹配。
 * 语法基本相同，但后者更适合Web使用并且更高效。
 * 选择取决于{@link UrlPathHelper#resolveAndCacheLookupPath resolved} {@code String} lookupPath的存在或
 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}的存在，后者又取决于匹配当前请求的
 * {@link HandlerMapping}。
 *
 * <p>此拦截器支持的所有设置也可以在{@link AbstractController}上设置。
 * 此拦截器主要用于对由HandlerMapping映射的一组控制器应用检查和准备。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see PathMatcher
 * @see AntPathMatcher
 * @since 27.11.2003
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {
	/**
	 * 默认路径匹配器，为Ant风格的路径匹配器
	 */
	private static PathMatcher defaultPathMatcher = new AntPathMatcher();

	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser patternParser;

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = defaultPathMatcher;

	/**
	 * 路径模式与缓存时间（单位为秒）的缓存映射
	 */
	private Map<PathPattern, Integer> cacheMappings = new HashMap<>();

	/**
	 * 路径模式与缓存控制映射
	 */
	private Map<PathPattern, CacheControl> cacheControlMappings = new HashMap<>();


	/**
	 * 默认构造函数，使用{@link PathPatternParser#defaultInstance}。
	 */
	public WebContentInterceptor() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * 使用{@link PathPatternParser}解析模式的构造函数。
	 *
	 * @since 5.3
	 */
	public WebContentInterceptor(PathPatternParser parser) {
		// 默认情况下，没有对HTTP方法的限制，特别是用于注解控制器...
		super(false);
		this.patternParser = parser;
	}


	/**
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath 相同的属性}的快捷方式，
	 * 在配置的{@code UrlPathHelper}上。
	 *
	 * @deprecated 自5.3起，路径是在外部解析的，并通过{@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}获取
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
	}

	/**
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode 相同的属性}的快捷方式，
	 * 在配置的{@code UrlPathHelper}上。
	 *
	 * @deprecated 自5.3起，路径是在外部解析的，并通过{@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}获取
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
	}

	/**
	 * 设置要用于解析查找路径的{@link UrlPathHelper}。
	 *
	 * @deprecated 自5.3起，路径是在外部解析的，并通过{@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}获取
	 */
	@Deprecated
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
	}

	/**
	 * 配置用于与注册的URL模式匹配URL路径的PathMatcher，以选择请求的缓存设置。
	 * <p>这是一个高级属性，仅在需要自定义{@link AntPathMatcher}或自定义PathMatcher时使用。
	 * <p>默认情况下，这是{@link AntPathMatcher}。
	 * <p><strong>注意:</strong> 设置{@code PathMatcher}即使在{@link ServletRequestPathUtils#parseAndCache 解析和缓存}的
	 * {@code RequestPath}可用时，也会强制使用String模式匹配。
	 *
	 * @see #addCacheMapping
	 * @see #setCacheMappings
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 通过模式将缓存秒数映射到特定URL路径。
	 * <p>覆盖此拦截器的默认缓存秒数设置。
	 * 可以指定"-1"来排除URL路径的默认缓存。
	 * <p>有关模式语法，请参见{@link AntPathMatcher}和{@link PathPattern}，以及类级别的Javadoc，
	 * 了解何时使用每种模式。与{@link PathPattern}一样，语法基本相同，更适合Web使用。
	 * <p><b>注意:</b> 路径模式不应重叠。如果请求匹配多个映射，则有效地未定义将应用哪一个
	 * (由于{@code java.util.Properties}中的缺少键排序)。
	 *
	 * @param cacheMappings 将URL路径（作为键）与缓存秒数（作为值，需要可解析为整数）进行映射
	 * @see #setCacheSeconds
	 */
	public void setCacheMappings(Properties cacheMappings) {
		// 清空缓存映射
		this.cacheMappings.clear();

		// 获取缓存映射中的所有属性名
		Enumeration<?> propNames = cacheMappings.propertyNames();

		// 遍历属性名
		while (propNames.hasMoreElements()) {
			// 获取属性名对应的路径
			String path = (String) propNames.nextElement();
			// 获取路径对应的缓存秒数
			int cacheSeconds = Integer.parseInt(cacheMappings.getProperty(path));
			// 将路径和缓存秒数解析并放入缓存映射中
			this.cacheMappings.put(this.patternParser.parse(path), cacheSeconds);
		}
	}

	/**
	 * 将特定URL路径映射到特定的{@link org.springframework.http.CacheControl}。
	 * <p>覆盖此拦截器的默认缓存秒数设置。
	 * 可以指定一个空的{@link org.springframework.http.CacheControl}实例来排除URL路径的默认缓存。
	 * <p>有关模式语法，请参见{@link AntPathMatcher}和{@link PathPattern}，以及类级别的Javadoc，
	 * 了解何时使用每种模式。与{@link PathPattern}一样，语法基本相同，更适合Web使用。
	 * <p><b>注意:</b> 路径模式不应重叠。如果请求匹配多个映射，则有效地未定义将应用哪一个
	 * (由于基础{@code java.util.HashMap}中的键排序的缺失)。
	 *
	 * @param cacheControl 要使用的{@code CacheControl}
	 * @param paths        将映射到给定{@code CacheControl}的URL路径
	 * @see #setCacheSeconds
	 * @since 4.2
	 */
	public void addCacheMapping(CacheControl cacheControl, String... paths) {
		// 遍历所有路径
		for (String path : paths) {
			// 将解析好的路径模式和缓存控制添加到缓存中
			this.cacheControlMappings.put(this.patternParser.parse(path), cacheControl);
		}
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		// 检查请求是否合法
		checkRequest(request);

		// 获取请求的路径
		Object path = ServletRequestPathUtils.getCachedPath(request);

		// 如果自定义的路径匹配器不是默认的路径匹配器
		if (this.pathMatcher != defaultPathMatcher) {
			// 将路径转换为字符串
			path = path.toString();
		}

		// 如果缓存控制映射不为空
		if (!ObjectUtils.isEmpty(this.cacheControlMappings)) {
			// 查找路径对应的缓存控制信息
			CacheControl control = (path instanceof PathContainer ?
					lookupCacheControl((PathContainer) path) : lookupCacheControl((String) path));
			// 如果找到缓存控制信息
			if (control != null) {
				// 如果日志跟踪启用
				if (logger.isTraceEnabled()) {
					logger.trace("Applying " + control);
				}
				// 应用缓存控制信息到响应
				applyCacheControl(response, control);
				return true;
			}
		}

		// 如果缓存映射不为空
		if (!ObjectUtils.isEmpty(this.cacheMappings)) {
			// 查找路径对应的缓存秒数
			Integer cacheSeconds = (path instanceof PathContainer ?
					lookupCacheSeconds((PathContainer) path) : lookupCacheSeconds((String) path));
			// 如果找到缓存秒数
			if (cacheSeconds != null) {
				// 如果日志跟踪启用
				if (logger.isTraceEnabled()) {
					logger.trace("Applying cacheSeconds " + cacheSeconds);
				}
				// 应用缓存秒数到响应
				applyCacheSeconds(response, cacheSeconds);
				return true;
			}
		}

		// 准备响应
		prepareResponse(response);
		return true;
	}

	/**
	 * 查找给定解析的{@link PathContainer path}的{@link org.springframework.http.CacheControl}实例。
	 * 当{@code HandlerMapping}使用解析的{@code PathPatterns}时使用此方法。
	 *
	 * @param path 要匹配的路径
	 * @return 匹配的{@code CacheControl}，如果没有匹配则为{@code null}
	 * @since 5.3
	 */
	@Nullable
	protected CacheControl lookupCacheControl(PathContainer path) {
		// 遍历缓存控制映射中的每个条目
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			// 如果路径匹配当前条目的路径模式
			if (entry.getKey().matches(path)) {
				// 返回该条目的缓存控制信息
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的缓存控制信息，则返回 null
		return null;
	}

	/**
	 * 查找给定String查找路径的{@link org.springframework.http.CacheControl}实例。
	 * 当{@code HandlerMapping}依赖使用{@link PathMatcher}的String模式匹配时使用此方法。
	 *
	 * @param lookupPath 要匹配的路径
	 * @return 匹配的{@code CacheControl}，如果没有匹配则为{@code null}
	 */
	@Nullable
	protected CacheControl lookupCacheControl(String lookupPath) {
		// 遍历缓存控制映射中的每个条目
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			// 如果当前条目的路径模式与查找路径匹配
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				// 返回该条目的缓存控制信息
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的缓存控制信息，则返回 null
		return null;
	}

	/**
	 * 查找给定解析的{@link PathContainer path}的缓存秒数值。
	 * 当{@code HandlerMapping}使用解析的{@code PathPatterns}时使用此方法。
	 *
	 * @param path 要匹配的路径
	 * @return 匹配的缓存秒数，如果没有匹配则为{@code null}
	 * @since 5.3
	 */
	@Nullable
	protected Integer lookupCacheSeconds(PathContainer path) {
		// 遍历缓存映射中的每个条目
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			// 如果路径匹配当前条目的路径模式
			if (entry.getKey().matches(path)) {
				// 返回该条目的缓存秒数
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的缓存秒数，则返回 null
		return null;
	}

	/**
	 * 查找给定String查找路径的缓存秒数值。
	 * 当{@code HandlerMapping}依赖使用{@link PathMatcher}的String模式匹配时使用此方法。
	 *
	 * @param lookupPath 要匹配的路径
	 * @return 匹配的缓存秒数，如果没有匹配则为{@code null}
	 */
	@Nullable
	protected Integer lookupCacheSeconds(String lookupPath) {
		// 遍历缓存映射中的每个条目
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			// 如果当前条目的路径模式与查找路径匹配
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				// 返回该条目的缓存秒数
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的缓存秒数，则返回 null
		return null;
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
						   @Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
								@Nullable Exception ex) throws Exception {
	}

}
