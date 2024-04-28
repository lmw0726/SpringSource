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

package org.springframework.web.servlet.handler;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * 包装{@link HandlerInterceptor}并使用URL模式确定是否适用于给定的请求。
 *
 * <p>可以使用{@link PathMatcher}或解析的{@link PathPattern}进行模式匹配。
 * 后者的语法基本相同，但更适合Web使用并且更高效。
 * 选择由{@link UrlPathHelper#resolveAndCacheLookupPath resolved} {@code String} lookupPath的存在或
 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}的存在驱动，
 * 后者又取决于匹配当前请求的{@link HandlerMapping}。
 *
 * <p>{@code MappedInterceptor}由{@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping}的子类支持，后者检测到类型为{@code MappedInterceptor}的bean，
 * 并检查直接注册到其上的拦截器是否属于此类型。
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.0
 */
public final class MappedInterceptor implements HandlerInterceptor {
	/**
	 * 默认的路径匹配器
	 */
	private static PathMatcher defaultPathMatcher = new AntPathMatcher();

	/**
	 * 请求必须匹配的模式
	 */
	@Nullable
	private final PatternAdapter[] includePatterns;

	/**
	 * 请求不能匹配的模式
	 */
	@Nullable
	private final PatternAdapter[] excludePatterns;

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = defaultPathMatcher;

	/**
	 * 处理器过滤器
	 */
	private final HandlerInterceptor interceptor;


	/**
	 * 使用给定的包含和排除模式以及映射的目标拦截器创建实例。
	 *
	 * @param includePatterns 请求必须匹配的模式，为null表示匹配所有路径
	 * @param excludePatterns 请求不能匹配的模式
	 * @param interceptor     目标拦截器
	 * @param parser          用于预解析模式为{@link PathPattern}的解析器；如果未提供，则使用{@link PathPatternParser#defaultInstance}。
	 * @since 5.3
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
							 HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {

		this.includePatterns = PatternAdapter.initPatterns(includePatterns, parser);
		this.excludePatterns = PatternAdapter.initPatterns(excludePatterns, parser);
		this.interceptor = interceptor;
	}


	/**
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}的变体，仅包含包含模式。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * 不提供解析器的{@link #MappedInterceptor(String[], String[],
	 * HandlerInterceptor, PathPatternParser)}的变体。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
							 HandlerInterceptor interceptor) {

		this(includePatterns, excludePatterns, interceptor, null);
	}

	/**
	 * {@link WebRequestInterceptor}作为目标的
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}的变体。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * {@link WebRequestInterceptor}作为目标的
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}的变体。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
							 WebRequestInterceptor interceptor) {

		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * 返回此拦截器映射到的模式。
	 */
	@Nullable
	public String[] getPathPatterns() {
		return (!ObjectUtils.isEmpty(this.includePatterns) ?
				// 如果包含模式数组不为空
				// 对每个模式调用getPatternString方法，转换为字符串数组
				Arrays.stream(this.includePatterns)
						.map(PatternAdapter::getPatternString)
						.toArray(String[]::new)
				// 如果为空，则返回null
				: null);

	}

	/**
	 * 在匹配时调用的目标{@link HandlerInterceptor}。
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 * 配置用于与包含和排除模式匹配URL路径的PathMatcher。
	 * <p>这是一个高级属性，仅在需要自定义{@link AntPathMatcher}或自定义PathMatcher时使用。
	 * <p>默认情况下，这是{@link AntPathMatcher}。
	 * <p><strong>注意:</strong> 设置{@code PathMatcher}即使在{@link ServletRequestPathUtils#parseAndCache 解析和缓存}
	 * 的{@code RequestPath}可用时，也会强制使用String模式匹配。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * {@link #setPathMatcher(PathMatcher) 配置的} PathMatcher。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	/**
	 * 检查此拦截器是否映射到请求。
	 * <p>预期请求映射路径已在外部解析。另请参见类级别的 Javadoc。
	 *
	 * @param request 要匹配的请求
	 * @return 如果应将拦截器应用于请求，则返回 {@code true}
	 */
	public boolean matches(HttpServletRequest request) {
		// 获取请求的路径
		Object path = ServletRequestPathUtils.getCachedPath(request);

		// 如果自定义的路径匹配器不是默认的路径匹配器
		if (this.pathMatcher != defaultPathMatcher) {
			// 将路径转换为字符串
			path = path.toString();
		}

		// 判断路径是否是路径容器的实例
		boolean isPathContainer = (path instanceof PathContainer);

		// 如果排除模式不为空
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			// 遍历排除模式
			for (PatternAdapter adapter : this.excludePatterns) {
				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
					// 如果模式适配器匹配路径，则返回 false
					return false;
				}
			}
		}

		// 如果包含模式为空，则返回 true
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}

		// 遍历包含模式
		for (PatternAdapter adapter : this.includePatterns) {
			if (adapter.match(path, isPathContainer, this.pathMatcher)) {
				// 如果模式适配器匹配路径，则返回 true
				return true;
			}
		}

		// 如果没有匹配的模式，则返回 false
		return false;
	}

	/**
	 * 确定给定查找路径的匹配项。
	 *
	 * @param lookupPath  当前请求路径
	 * @param pathMatcher 用于路径模式匹配的路径匹配器
	 * @return 如果拦截器适用于给定的请求路径，则返回 {@code true}
	 * @deprecated 自 5.3 起已弃用，请改用 {@link #matches(HttpServletRequest)}
	 */
	@Deprecated
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		// 如果自定义的路径匹配器不是默认的路径匹配器，则将自定义的路径匹配器赋值给 pathMatcher，
		// 否则保持原有的 pathMatcher
		pathMatcher = (this.pathMatcher != defaultPathMatcher ? this.pathMatcher : pathMatcher);

		// 如果排除模式不为空
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			// 遍历排除模式
			for (PatternAdapter adapter : this.excludePatterns) {
				if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
					// 如果路径匹配器匹配模式字符串和查找路径，则返回 false
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
		// 如果包含模式为空，则返回 true
			return true;
		}

		// 遍历包含模式
		for (PatternAdapter adapter : this.includePatterns) {
			if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
				// 如果路径匹配器匹配模式字符串和查找路径，则返回 true
				return true;
			}
		}

		// 如果没有匹配的模式，则返回 false
		return false;
	}


	// HandlerInterceptor delegation

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return this.interceptor.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
						   @Nullable ModelAndView modelAndView) throws Exception {

		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
								@Nullable Exception ex) throws Exception {

		this.interceptor.afterCompletion(request, response, handler, ex);
	}


	/**
	 * 包含解析后的 {@link PathPattern} 和原始字符串模式，并在缓存路径为 {@link PathContainer} 时使用前者，
	 * 否则使用后者。
	 * 如果由于不支持的语法而无法解析模式，则对所有请求使用 {@link PathMatcher}。
	 *
	 * @since 5.3.6
	 */
	private static class PatternAdapter {

		/**
		 * 模式字符串
		 */
		private final String patternString;
		/**
		 * 路径模式
		 */
		@Nullable
		private final PathPattern pathPattern;


		public PatternAdapter(String pattern, @Nullable PathPatternParser parser) {
			this.patternString = pattern;
			this.pathPattern = initPathPattern(pattern, parser);
		}

		@Nullable
		private static PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
			try {
				// 如果路径模式解析器存在，使用解析器解析路径模式
				return (parser != null ? parser : PathPatternParser.defaultInstance).parse(pattern);
			} catch (PatternParseException ex) {
				return null;
			}
		}

		public String getPatternString() {
			return this.patternString;
		}

		public boolean match(Object path, boolean isPathContainer, PathMatcher pathMatcher) {
			if (isPathContainer) {
				// 如果是路径容器
				// 将路径强制转换为PathContainer类型
				PathContainer pathContainer = (PathContainer) path;
				// 如果路径模式不为空，则使用路径模式匹配路径容器
				if (this.pathPattern != null) {
					return this.pathPattern.matches(pathContainer);
				}
				// 获取路径容器中的路径
				String lookupPath = pathContainer.value();
				// 移除寻找路径中的分号内容
				path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
			}
			// 使用路径匹配器 匹配 路径模式 和 路径
			return pathMatcher.match(this.patternString, (String) path);
		}

		@Nullable
		public static PatternAdapter[] initPatterns(
				@Nullable String[] patterns, @Nullable PathPatternParser parser) {

			if (ObjectUtils.isEmpty(patterns)) {
				// 如果模式字符串数组为空则返回null
				return null;
			}
			// 如果模式字符串数组为空不为空，对每个元素转换为PatternAdapter对象
			// 最后形成PatternAdapter数组中并返回
			return Arrays.stream(patterns)
					.map(pattern -> new PatternAdapter(pattern, parser))
					.toArray(PatternAdapter[]::new);
		}
	}

}
