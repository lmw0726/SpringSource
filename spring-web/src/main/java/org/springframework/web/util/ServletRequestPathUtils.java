/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.util;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MappingMatch;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 辅助准备和访问用于请求映射的查找路径的实用工具类。当启用 {@link org.springframework.web.util.pattern.PathPattern 解析模式} 时，
 * 这可以是路径的解析 {@link RequestPath} 表示，或者如果使用 {@link org.springframework.util.PathMatcher} 则可以是 String 路径。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public abstract class ServletRequestPathUtils {

	/**
	 * 持有解析的 {@link RequestPath} 的 Servlet 请求属性的名称。
	 */
	public static final String PATH_ATTRIBUTE = ServletRequestPathUtils.class.getName() + ".PATH";


	/**
	 * 将 {@link HttpServletRequest#getRequestURI() requestURI} 解析为
	 * {@link RequestPath} 并保存在请求属性 {@link #PATH_ATTRIBUTE} 中，以便后续与
	 * {@link org.springframework.web.util.pattern.PathPattern 解析模式} 一起使用。
	 * <p>返回的 {@code RequestPath} 将从其 {@link RequestPath#pathWithinApplication()
	 * pathWithinApplication} 中省略上下文路径和任何 servlet 路径前缀。
	 * <p>通常，此方法由 {@code DispatcherServlet} 调用以确定是否有任何 {@code HandlerMapping}
	 * 表示它使用了解析的模式。之后，预解析和缓存的 {@code RequestPath} 可以通过 {@link #getParsedRequestPath(ServletRequest)} 访问。
	 */
	public static RequestPath parseAndCache(HttpServletRequest request) {
		// 解析请求路径
		RequestPath requestPath = ServletRequestPath.parse(request);
		// 将请求路径设置为请求属性
		request.setAttribute(PATH_ATTRIBUTE, requestPath);
		// 返回请求路径
		return requestPath;
	}

	/**
	 * 返回 {@link #parseAndCache 之前} 解析并缓存的 {@code RequestPath}。
	 *
	 * @throws IllegalArgumentException 如果未找到
	 */
	public static RequestPath getParsedRequestPath(ServletRequest request) {
		RequestPath path = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(path, () -> "Expected parsed RequestPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		return path;
	}

	/**
	 * 将缓存的解析 {@code RequestPath} 设置为给定值。
	 *
	 * @param requestPath 要设置的值，或者如果 {@code null} 则清除缓存值
	 * @param request     当前请求
	 * @since 5.3.3
	 */
	public static void setParsedRequestPath(@Nullable RequestPath requestPath, ServletRequest request) {
		// 如果请求路径不为空，则设置请求属性为请求路径
		if (requestPath != null) {
			request.setAttribute(PATH_ATTRIBUTE, requestPath);
		} else {
			// 否则移除请求属性
			request.removeAttribute(PATH_ATTRIBUTE);
		}
	}

	/**
	 * 检查之前是否 {@link #parseAndCache 解析和缓存} 了 {@code RequestPath}。
	 */
	public static boolean hasParsedRequestPath(ServletRequest request) {
		return (request.getAttribute(PATH_ATTRIBUTE) != null);
	}

	/**
	 * 移除持有 {@link #parseAndCache 解析和缓存} 的 {@code RequestPath} 的请求属性 {@link #PATH_ATTRIBUTE}。
	 */
	public static void clearParsedRequestPath(ServletRequest request) {
		request.removeAttribute(PATH_ATTRIBUTE);
	}


	// 用于选择已解析的 RequestPath 或已解析的 String 查找路径的方法

	/**
	 * 返回 {@link UrlPathHelper#resolveAndCacheLookupPath 预解析的}
	 * String 查找路径或 {@link #parseAndCache(HttpServletRequest) 预解析的} {@code RequestPath}。
	 * <p>在 Spring MVC 中，当至少一个 {@code HandlerMapping} 启用了解析的
	 * {@code PathPatterns} 时，{@code DispatcherServlet} 会急切地解析和缓存
	 * {@code RequestPath}，并且可以在之前通过 {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 也执行相同的操作。在使用 {@code PathMatcher} 的情况下，
	 * 每个 {@code HandlerMapping} 都会单独解析 String 查找路径。
	 *
	 * @param request 当前请求
	 * @return String 查找路径或 {@code RequestPath}
	 * @throws IllegalArgumentException 如果两者都不可用
	 */
	public static Object getCachedPath(ServletRequest request) {

		// 如果任何 HandlerMapping 使用 PathPatterns，则 RequestPath 将被预解析。
		// lookupPath 会被每个 HandlerMapping 重新解析或清除。
		// 因此首先检查 lookupPath。

		// 从请求属性中获取查找路径
		String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
		// 如果查找路径不为空，则返回查找路径
		if (lookupPath != null) {
			return lookupPath;
		}
		// 从请求属性中获取请求路径
		RequestPath requestPath = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		// 如果请求路径不为空，则返回应用程序内的路径
		if (requestPath != null) {
			return requestPath.pathWithinApplication();
		}
		// 如果既没有预解析的请求路径也没有预解析的查找路径，则抛出异常
		throw new IllegalArgumentException(
				"Neither a pre-parsed RequestPath nor a pre-resolved String lookupPath is available.");
	}

	/**
	 * {@link #getCachedPath(ServletRequest)} 的变体，以 String 形式返回请求映射的路径。
	 * <p>如果缓存的路径是 {@link #parseAndCache(HttpServletRequest) 预解析的} {@code RequestPath}，
	 * 则返回的 String 路径值是编码的并且不带路径参数。
	 * <p>如果缓存的路径是 {@link UrlPathHelper#resolveAndCacheLookupPath 预解析的} String 查找路径，
	 * 则返回的 String 路径值取决于如何配置 {@link UrlPathHelper}。
	 *
	 * @param request 当前请求
	 * @return 完整的请求映射路径作为 String
	 */
	public static String getCachedPathValue(ServletRequest request) {
		// 获取缓存的路径
		Object path = getCachedPath(request);
		// 如果路径是 PathContainer 类型的实例
		if (path instanceof PathContainer) {
			// 获取路径的值
			String value = ((PathContainer) path).value();
			// 移除路径中的分号内容
			path = UrlPathHelper.defaultInstance.removeSemicolonContent(value);
		}
		// 将路径转换为字符串并返回
		return (String) path;
	}

	/**
	 * 检查以前是否已 {@link UrlPathHelper#resolveAndCacheLookupPath 解析} 了 String 查找路径，
	 * 或者以前是否已 {@link #parseAndCache 解析} 了 {@code RequestPath}。
	 *
	 * @param request 当前请求
	 * @return 是否可用预解析或预解析的路径
	 */
	public static boolean hasCachedPath(ServletRequest request) {
		// 如果请求属性中的 Servlet 请求属性的名称 不为空，或者 UrlPathHelper中的 Servlet请求属性的名称 不为空，则返回 true
		return (request.getAttribute(PATH_ATTRIBUTE) != null ||
				request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) != null);
	}


	/**
	 * 对默认的 {@link RequestPath} 实现的简单包装，支持将 servletPath 作为要从
	 * {@link #pathWithinApplication()} 中省略的附加前缀。
	 */
	private static final class ServletRequestPath implements RequestPath {
		/**
		 * 请求路径
		 */
		private final RequestPath requestPath;

		/**
		 * 路径容器
		 */
		private final PathContainer contextPath;

		private ServletRequestPath(String rawPath, @Nullable String contextPath, String servletPathPrefix) {
			Assert.notNull(servletPathPrefix, "`servletPathPrefix` is required");
			this.requestPath = RequestPath.parse(rawPath, contextPath + servletPathPrefix);
			this.contextPath = PathContainer.parsePath(StringUtils.hasText(contextPath) ? contextPath : "");
		}

		@Override
		public String value() {
			return this.requestPath.value();
		}

		@Override
		public List<Element> elements() {
			return this.requestPath.elements();
		}

		@Override
		public PathContainer contextPath() {
			return this.contextPath;
		}

		@Override
		public PathContainer pathWithinApplication() {
			return this.requestPath.pathWithinApplication();
		}

		@Override
		public RequestPath modifyContextPath(String contextPath) {
			throw new UnsupportedOperationException();
		}


		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			return (this.requestPath.equals(((ServletRequestPath) other).requestPath));
		}

		@Override
		public int hashCode() {
			return this.requestPath.hashCode();
		}

		@Override
		public String toString() {
			return this.requestPath.toString();
		}


		public static RequestPath parse(HttpServletRequest request) {
			// 从请求属性中获取包含请求 URI 的字符串
			String requestUri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
			// 如果请求 URI 为空，则从请求中获取请求 URI
			if (requestUri == null) {
				requestUri = request.getRequestURI();
			}
			// 如果存在 Servlet 4
			if (UrlPathHelper.servlet4Present) {
				// 获取 Servlet 的路径前缀
				String servletPathPrefix = Servlet4Delegate.getServletPathPrefix(request);
				// 如果 Servlet 的路径前缀 不为空
				if (StringUtils.hasText(servletPathPrefix)) {
					// 返回新的 Servlet请求路径 实例
					return new ServletRequestPath(requestUri, request.getContextPath(), servletPathPrefix);
				}
			}
			// 解析请求 URI，并返回请求路径实例
			return RequestPath.parse(requestUri, request.getContextPath());
		}
	}


	/**
	 * 避免在运行时对 Servlet 4 {@link HttpServletMapping} 和 {@link MappingMatch} 进行硬依赖的内部类。
	 */
	private static class Servlet4Delegate {

		@Nullable
		public static String getServletPathPrefix(HttpServletRequest request) {
			// 从请求属性中获取包含请求的 HttpServletMapping 对象
			HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
			// 如果映射为空，则从请求中获取 HttpServletMapping 对象
			if (mapping == null) {
				mapping = request.getHttpServletMapping();
			}
			// 获取映射匹配类型
			MappingMatch match = mapping.getMappingMatch();
			// 如果匹配类型不是 PATH，则返回 null
			if (!ObjectUtils.nullSafeEquals(match, MappingMatch.PATH)) {
				return null;
			}
			// 从请求属性中获取包含 Servlet 路径的字符串
			String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
			// 如果 Servlet 路径不为空，则使用其值；否则，使用请求的 Servlet 路径
			servletPath = (servletPath != null ? servletPath : request.getServletPath());
			// 使用 UTF-8 编码路径并返回
			return UriUtils.encodePath(servletPath, StandardCharsets.UTF_8);
		}
	}

}
