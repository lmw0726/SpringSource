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

package org.springframework.web.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MappingMatch;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.Properties;

/**
 * URL路径匹配的辅助类。提供对{@code RequestDispatcher}包含的URL路径的支持，并支持一致的URL解码。
 *
 * <p>由{@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping}
 * 和{@link org.springframework.web.servlet.support.RequestContext}用于路径匹配
 * 和/或URI确定。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @see #getLookupPathForRequest
 * @see javax.servlet.RequestDispatcher
 * @since 14.01.2004
 */
public class UrlPathHelper {

	/**
	 * 保存一个{@link #getLookupPathForRequest 解析}后的lookupPath的Servlet请求属性的名称。
	 *
	 * @since 5.3
	 */
	public static final String PATH_ATTRIBUTE = UrlPathHelper.class.getName() + ".PATH";
	/**
	 * Servlet 4是否存在
	 */
	static final boolean servlet4Present =
			ClassUtils.hasMethod(HttpServletRequest.class, "getHttpServletMapping");

	/**
	 * WebSphere请求属性，表示原始请求URI。
	 * 优先于WebSphere上的标准Servlet 2.4转发属性，
	 * 仅仅因为我们需要请求转发链中的第一个URI。
	 */
	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(UrlPathHelper.class);

	/**
	 * Websphere合规性标志
	 */
	@Nullable
	static volatile Boolean websphereComplianceFlag;

	/**
	 * 是否在查找URL时，应始终使用当前web应用程序上下文内的完整路径
	 */
	private boolean alwaysUseFullPath = false;

	/**
	 * 是否在确定查找路径时解码请求URI。
	 */
	private boolean urlDecode = true;

	/**
	 * 是否从请求URI中去除";"（分号）内容。
	 */
	private boolean removeSemicolonContent = true;

	/**
	 * 默认编码格式
	 */
	private String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	/**
	 * URI是否只读
	 */
	private boolean readOnly = false;


	/**
	 * 是否URL查找应始终使用当前web应用程序上下文内的完整路径，即在
	 * {@link javax.servlet.ServletContext#getContextPath()}内。
	 * <p>如果设置为{@literal false}，则使用当前servlet映射内的路径（如果适用），
	 * 例如在"/myServlet/*"这样的前缀servlet映射的情况下。
	 * <p>默认设置为"false"。
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		checkReadOnly();
		this.alwaysUseFullPath = alwaysUseFullPath;
	}

	/**
	 * 是否应解码上下文路径和请求URI——Servlet API返回的两者均为<i>未解码</i>，与servlet路径相反。
	 * <p>如果设置为“true”，则使用请求编码或默认Servlet规范编码（ISO-8859-1）。
	 * <p>默认设置为{@literal true}。
	 * <p><strong>注意：</strong> 请注意，servlet路径在与编码路径比较时不会匹配。
	 * 因此，使用{@code urlDecode=false}与基于前缀的Servlet映射不兼容，
	 * 并且同样意味着还需要设置{@code alwaysUseFullPath=true}。
	 *
	 * @see #getServletPath
	 * @see #getContextPath
	 * @see #getRequestUri
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		checkReadOnly();
		this.urlDecode = urlDecode;
	}

	/**
	 * 是否在确定查找路径时解码请求URI。
	 *
	 * @since 4.3.13
	 */
	public boolean isUrlDecode() {
		return this.urlDecode;
	}

	/**
	 * 设置是否从请求URI中去除";"（分号）内容。
	 * <p>默认是"true"。
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		checkReadOnly();
		this.removeSemicolonContent = removeSemicolonContent;
	}

	/**
	 * 是否配置为从请求URI中删除";"（分号）内容。
	 */
	public boolean shouldRemoveSemicolonContent() {
		return this.removeSemicolonContent;
	}

	/**
	 * 设置用于URL解码的默认字符编码。
	 * 默认是ISO-8859-1，根据Servlet规范。
	 * <p>如果请求本身指定了字符编码，则请求编码将覆盖此设置。
	 * 这也允许在调用{@code ServletRequest.setCharacterEncoding}方法的过滤器中
	 * 通常覆盖字符编码。
	 *
	 * @param defaultEncoding 用于解码的字符编码
	 * @see #determineEncoding
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		checkReadOnly();
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于URL解码的默认字符编码。
	 */
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 切换到只读模式，不允许进一步的配置更改。
	 */
	private void setReadOnly() {
		this.readOnly = true;
	}

	private void checkReadOnly() {
		Assert.isTrue(!this.readOnly, "This instance cannot be modified");
	}


	/**
	 * {@link #getLookupPathForRequest 解析}lookupPath并将其缓存到
	 * 键为{@link #PATH_ATTRIBUTE}的请求属性中，以便后续
	 * 通过{@link #getResolvedLookupPath(ServletRequest)}访问。
	 *
	 * @param request 当前请求
	 * @return 解析的路径
	 * @since 5.3
	 */
	public String resolveAndCacheLookupPath(HttpServletRequest request) {
		// 获取请求的查找路径
		String lookupPath = getLookupPathForRequest(request);
		// 将查找路径设置为请求属性
		request.setAttribute(PATH_ATTRIBUTE, lookupPath);
		// 返回查找路径
		return lookupPath;
	}

	/**
	 * 返回之前{@link #getLookupPathForRequest 解析}的lookupPath。
	 *
	 * @param request 当前请求
	 * @return 之前解析的lookupPath
	 * @throws IllegalArgumentException 如果未找到
	 * @since 5.3
	 */
	public static String getResolvedLookupPath(ServletRequest request) {
		// 获取请求的查找路径
		String lookupPath = (String) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(lookupPath, "Expected lookupPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		// 返回查找路径
		return lookupPath;
	}

	/**
	 * {@link #getLookupPathForRequest(HttpServletRequest)}的变体，
	 * 自动检查作为请求属性保存的先前计算的lookupPath。属性仅用于查找目的。
	 *
	 * @param request 当前HTTP请求
	 * @param name    保存lookupPath的请求属性
	 * @return 查找路径
	 * @since 5.2
	 * @deprecated 自5.3起，建议使用
	 * {@link #resolveAndCacheLookupPath(HttpServletRequest)}和
	 * {@link #getResolvedLookupPath(ServletRequest)}。
	 */
	@Deprecated
	public String getLookupPathForRequest(HttpServletRequest request, @Nullable String name) {
		// 定义返回结果变量
		String result = null;

		// 如果名称不为空
		if (name != null) {
			// 从请求中获取名称对应的属性值
			result = (String) request.getAttribute(name);
		}

		// 返回结果，如果结果不为空则返回结果，否则调用 getLookupPathForRequest 方法获取查找路径并返回
		return (result != null ? result : getLookupPathForRequest(request));
	}

	/**
	 * 返回给定请求的映射查找路径，在当前servlet映射内（如果适用），否则在web应用程序内。
	 * <p>如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 *
	 * @param request 当前HTTP请求
	 * @return 查找路径
	 * @see #getPathWithinServletMapping
	 * @see #getPathWithinApplication
	 */
	public String getLookupPathForRequest(HttpServletRequest request) {
		// 获取应用上下文内的路径
		String pathWithinApp = getPathWithinApplication(request);
		// 始终使用当前servlet上下文内的完整路径？
		if (this.alwaysUseFullPath || skipServletPathDetermination(request)) {
			// 如果使用使用全路径，或者跳过确定Servlet路径，返回应用上下文内的路径
			return pathWithinApp;
		}
		// 否则，使用当前servlet映射内的路径（如果适用）
		// 获取Servlet映射内的路径
		String rest = getPathWithinServletMapping(request, pathWithinApp);
		if (StringUtils.hasLength(rest)) {
			// 如果存在Servlet映射内的路径，则返回该路径
			return rest;
		} else {
			// 否则返回应用上下文内的路径
			return pathWithinApp;
		}
	}

	/**
	 * 检查是否可以跳过给定请求的servlet路径确定。
	 *
	 * @param request 当前HTTP请求
	 * @return 如果请求映射不是通过路径实现的或servlet映射到根目录，则返回{@code true}；否则返回{@code false}
	 */
	private boolean skipServletPathDetermination(HttpServletRequest request) {
		if (servlet4Present) {
			// 如果存在Servlet 4，则使用Servlet4代理跳过确定Servlet路径
			return Servlet4Delegate.skipServletPathDetermination(request);
		}
		return false;
	}

	/**
	 * 返回给定请求在servlet映射中的路径，
	 * 即请求URL中调用servlet的部分之外的部分，如果整个URL已被用来标识servlet，则返回""。
	 *
	 * @param request 当前HTTP请求
	 * @return servlet映射中的路径，或""
	 * @see #getPathWithinServletMapping(HttpServletRequest, String)
	 */
	public String getPathWithinServletMapping(HttpServletRequest request) {
		return getPathWithinServletMapping(request, getPathWithinApplication(request));
	}

	/**
	 * 返回给定请求在servlet映射中的路径，
	 * 即请求URL中调用servlet的部分之外的部分，如果整个URL已被用来标识servlet，则返回""。
	 * <p>如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 * <p>例如：servlet映射 = "/*"; 请求URI = "/test/a" → "/test/a"。
	 * <p>例如：servlet映射 = "/"; 请求URI = "/test/a" → "/test/a"。
	 * <p>例如：servlet映射 = "/test/*"; 请求URI = "/test/a" → "/a"。
	 * <p>例如：servlet映射 = "/test"; 请求URI = "/test" → ""。
	 * <p>例如：servlet映射 = "/*.test"; 请求URI = "/a.test" → ""。
	 *
	 * @param request       当前HTTP请求
	 * @param pathWithinApp 预计算的应用程序内路径
	 * @return servlet映射中的路径，或""
	 * @see #getLookupPathForRequest
	 * @since 5.2.9
	 */
	protected String getPathWithinServletMapping(HttpServletRequest request, String pathWithinApp) {
		// 获取Servlet路径
		String servletPath = getServletPath(request);
		// 获取清理后的路径
		String sanitizedPathWithinApp = getSanitizedPath(pathWithinApp);
		String path;

		// 如果应用容器对servletPath进行了清理，则检查清理后的版本
		if (servletPath.contains(sanitizedPathWithinApp)) {
			// 如果Servlet路径含有清理后的路径，则使用 清理后的路径 获取 剩余路径
			path = getRemainingPath(sanitizedPathWithinApp, servletPath, false);
		} else {
			// 否则使用 应用内的路径 获取 剩余路径
			path = getRemainingPath(pathWithinApp, servletPath, false);
		}

		if (path != null) {
			// 正常情况：URI包含servlet路径。
			return path;
		} else {
			// 特殊情况：URI与servlet路径不同。
			// 剩余路径不存在，从请求中获取路径信息
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				// 如果有可用的路径信息。表示servlet映射中的索引页？
				// 例如带有索引页：URI="/"，servletPath="/index.html"
				return pathInfo;
			}
			if (!this.urlDecode) {
				// 没有路径信息...(不通过前缀映射，也不通过扩展映射，也不是"/*")
				// 对于默认的servlet映射（即"/"），urlDecode=false可能
				// 会导致问题，因为getServletPath()返回的是已解码路径。
				// 如果解码后的pathWithinApp匹配，则仅使用pathWithinApp。
				// 如果不需要解码URL，则使用 解码后的路径 获取剩余路径
				path = getRemainingPath(decodeInternal(request, pathWithinApp), servletPath, false);
				if (path != null) {
					// 剩余路径不存在，则返回应用内的路径。
					return pathWithinApp;
				}
			}
			// 否则，使用完整的servlet路径。
			return servletPath;
		}
	}

	/**
	 * 返回给定请求在web应用程序中的路径。
	 * <p>如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 *
	 * @param request 当前HTTP请求
	 * @return web应用程序中的路径
	 * @see #getLookupPathForRequest
	 */
	public String getPathWithinApplication(HttpServletRequest request) {
		// 获取请求的上下文路径
		String contextPath = getContextPath(request);
		// 获取请求的URI
		String requestUri = getRequestUri(request);

		// 获取剩余路径，上下文路径 是 URI 的前缀部分
		String path = getRemainingPath(requestUri, contextPath, true);

		// 判断剩余路径是否为空
		if (path != null) {
			// 正常情况：URI包含上下文路径。
			// 如果剩余路径存在且有文本内容，返回它；否则返回根路径 "/"
			return (StringUtils.hasText(path) ? path : "/");
		} else {
			// 如果剩余路径为空，直接返回请求的URI
			return requestUri;
		}
	}

	/**
	 * 将给定的"映射"与"请求URI"的开头匹配，如果匹配则返回多余的部分。
	 * 此方法是必要的，因为HttpServletRequest返回的上下文路径和servlet路径
	 * 被去除了分号内容，而 请求URI 没有。
	 */
	@Nullable
	private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
		// 初始化索引
		int index1 = 0;
		int index2 = 0;

		// 遍历 请求URI 和 映射
		for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);

			// 如果当前字符是分号 ';'
			if (c1 == ';') {
				// 在 请求URI 中查找下一个斜杠 '/'
				index1 = requestUri.indexOf('/', index1);
				// 如果没有找到斜杠，则返回 null
				if (index1 == -1) {
					return null;
				}
				// 更新 c1 为找到的斜杠对应的字符
				c1 = requestUri.charAt(index1);
			}

			if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
				// 如果当前字符相同，或者忽略大小写且对应字符的小写相同，则继续比较下一个字符
				continue;
			}
			// 如果当前字符不同，则返回 null
			return null;
		}

		// 如果 映射 的索引不等于 映射 的长度，则返回 null
		if (index2 != mapping.length()) {
			return null;
		} else if (index1 == requestUri.length()) {
			// 如果 请求URI 的索引等于 请求URI 的长度，则返回空字符串
			return "";
		} else if (requestUri.charAt(index1) == ';') {
			// 如果 请求URI 的索引对应的字符是分号，则在 请求URI 中查找下一个斜杠 '/'
			index1 = requestUri.indexOf('/', index1);
		}

		// 如果找到斜杠，则返回从该斜杠开始到字符串末尾的子字符串；否则返回空字符串
		return (index1 != -1 ? requestUri.substring(index1) : "");
	}

	/**
	 * 清理给定的路径。使用以下规则:
	 * <ul>
	 * <li>将所有“//”替换为“/”</li>
	 * </ul>
	 */
	private static String getSanitizedPath(final String path) {
		// 查找路径中的双斜杠的起始位置
		int start = path.indexOf("//");

		// 如果未找到双斜杠，则直接返回路径
		if (start == -1) {
			return path;
		}

		// 将路径转换为字符数组
		char[] content = path.toCharArray();

		// 定义慢指针和快指针，从双斜杠的位置开始遍历路径字符数组
		int slowIndex = start;
		for (int fastIndex = start + 1; fastIndex < content.length; fastIndex++) {
			// 如果当前字符不是斜杠，或者前一个字符不是斜杠，则将当前字符移到慢指针位置
			if (content[fastIndex] != '/' || content[slowIndex] != '/') {
				content[++slowIndex] = content[fastIndex];
			}
		}

		// 返回去除多余双斜杠后的字符串
		return new String(content, 0, slowIndex + 1);
	}

	/**
	 * 返回给定请求的请求URI，如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 * <p>由于{@code request.getRequestURI()}返回的值<i>未</i>由servlet容器解码，因此此方法将对其进行解码。
	 * <p>web容器解析的URI<i>应该</i>是正确的，但某些容器如JBoss/Jetty会错误地在URI中包含“;”字符串，如“;jsessionid”。
	 * 此方法将切断这些不正确的附加部分。
	 *
	 * @param request 当前HTTP请求
	 * @return 请求URI
	 */
	public String getRequestUri(HttpServletRequest request) {
		// 从请求属性中获取包含的请求URI
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);

		// 如果未找到包含的请求URI，则使用请求的URI
		if (uri == null) {
			uri = request.getRequestURI();
		}

		// 解码和清理URI字符串
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径，如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 * <p>由于{@code request.getContextPath()}返回的值<i>未</i>由servlet容器解码，因此此方法将对其进行解码。
	 *
	 * @param request 当前HTTP请求
	 * @return 上下文路径
	 */
	public String getContextPath(HttpServletRequest request) {
		// 从请求属性中获取包含的上下文路径
		String contextPath = (String) request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);

		// 如果未找到包含的上下文路径，则使用请求的上下文路径
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}

		// 如果上下文路径以斜杠开头，则将其设为空字符串
		if (StringUtils.matchesCharacter(contextPath, '/')) {
			// 无效情况，但在Jetty的包含中会发生：静默适应它。
			contextPath = "";
		}

		// 解码请求字符串，并返回
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 返回给定请求的servlet路径，如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 * <p>由于{@code request.getServletPath()}返回的值已由servlet容器解码，因此此方法不会尝试解码它。
	 *
	 * @param request 当前HTTP请求
	 * @return servlet路径
	 */
	public String getServletPath(HttpServletRequest request) {
		// 从请求属性中获取包含的Servlet路径
		String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);

		// 如果未找到包含的Servlet路径，则使用请求的Servlet路径
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}

		// 如果Servlet路径长度大于1，且以斜杠结尾，并且应该移除尾随的Servlet路径斜杠
		if (servletPath.length() > 1 && servletPath.endsWith("/") && shouldRemoveTrailingServletPathSlash(request)) {
			// 在WebSphere上，在非兼容模式下，对于"/foo/"的情况，在所有其他servlet容器上将是"/foo"：
			// 删除尾随斜杠，继续使用剩余的斜杠作为最终查找路径...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}

		// 返回Servlet路径
		return servletPath;
	}


	/**
	 * 返回给定请求的请求URI。如果这是一个转发请求，则正确解析为原始请求的请求URI。
	 *
	 * @param request 当前HTTP请求
	 * @return 请求URI
	 */
	public String getOriginatingRequestUri(HttpServletRequest request) {
		// 从请求属性中获取WebSphere特定的URI属性
		String uri = (String) request.getAttribute(WEBSPHERE_URI_ATTRIBUTE);

		// 如果未找到WebSphere特定的URI属性，则尝试获取转发请求的URI属性
		if (uri == null) {
			uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);

			// 如果转发请求的URI属性也不存在，则使用请求的URI
			if (uri == null) {
				uri = request.getRequestURI();
			}
		}

		// 解码和清理URI字符串
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径，如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 * <p>由于{@code request.getContextPath()}返回的值<i>未</i>由servlet容器解码，因此此方法将对其进行解码。
	 *
	 * @param request 当前HTTP请求
	 * @return 上下文路径
	 */
	public String getOriginatingContextPath(HttpServletRequest request) {
		// 从请求属性中获取转发上下文路径属性
		String contextPath = (String) request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE);

		// 如果转发上下文路径属性不存在，则使用请求的上下文路径
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}

		// 解码请求字符串，并返回
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 返回给定请求的servlet路径，如果在RequestDispatcher包含中调用，则检测包含请求URL。
	 *
	 * @param request 当前HTTP请求
	 * @return servlet路径
	 */
	public String getOriginatingServletPath(HttpServletRequest request) {
		// 从请求属性中获取转发Servlet路径属性
		String servletPath = (String) request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE);

		// 如果转发Servlet路径属性不存在，则使用请求的Servlet路径
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}

		// 返回Servlet路径
		return servletPath;
	}

	/**
	 * 返回给定请求的URL的查询字符串部分。如果这是一个转发请求，则正确解析为原始请求的查询字符串。
	 *
	 * @param request 当前HTTP请求
	 * @return 查询字符串
	 */
	public String getOriginatingQueryString(HttpServletRequest request) {
		// 检查是否存在转发请求URI属性或错误请求URI属性
		if ((request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) ||
				(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null)) {
			// 返回转发查询字符串属性
			return (String) request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE);
		} else {
			// 返回请求的查询字符串
			return request.getQueryString();
		}
	}

	/**
	 * 解码提供的URI字符串并删除“;”之后的任何多余部分。
	 */
	private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
		// 移除URI中的分号内容
		uri = removeSemicolonContent(uri);

		// 解码请求字符串
		uri = decodeRequestString(request, uri);

		// 获取清理后的路径
		uri = getSanitizedPath(uri);

		// 返回处理后的URI
		return uri;
	}

	/**
	 * 使用URLDecoder解码给定的源字符串。编码将从请求中获取，默认为"ISO-8859-1"。
	 * <p>默认实现使用{@code URLDecoder.decode(input, enc)}。
	 *
	 * @param request 当前HTTP请求
	 * @param source  要解码的字符串
	 * @return 解码后的字符串
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see java.net.URLDecoder#decode(String, String)
	 * @see java.net.URLDecoder#decode(String)
	 */
	public String decodeRequestString(HttpServletRequest request, String source) {
		if (this.urlDecode) {
			// 如果需要解码URI，则调用内部解码方法
			return decodeInternal(request, source);
		}
		return source;
	}

	@SuppressWarnings("deprecation")
	private String decodeInternal(HttpServletRequest request, String source) {
		// 确定编码
		String enc = determineEncoding(request);

		try {
			// 使用指定编码解码源字符串
			return UriUtils.decode(source, enc);
		} catch (UnsupportedCharsetException ex) {
			// 如果指定编码不支持，则使用平台默认编码解码
			if (logger.isDebugEnabled()) {
				logger.debug("Could not decode request string [" + source + "] with encoding '" + enc +
						"': falling back to platform default encoding; exception message: " + ex.getMessage());
			}
			return URLDecoder.decode(source);
		}
	}

	/**
	 * 确定给定请求的编码。可以在子类中重写。
	 * <p>默认实现检查请求编码，如果没有则使用为此解析器指定的默认编码。
	 *
	 * @param request 当前HTTP请求
	 * @return 请求的编码（永不为{@code null}）
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		// 获取请求的字符编码
		String enc = request.getCharacterEncoding();

		// 如果字符编码为空，则使用默认编码
		if (enc == null) {
			enc = getDefaultEncoding();
		}

		// 返回字符编码
		return enc;
	}

	/**
	 * 如果{@linkplain #setRemoveSemicolonContent removeSemicolonContent}属性设置为"true"，则从给定请求URI中删除“;”内容。
	 * 请注意，“jsessionid”始终会被删除。
	 *
	 * @param requestUri 要从中删除“;”内容的请求URI字符串
	 * @return 更新后的URI字符串
	 */
	public String removeSemicolonContent(String requestUri) {
		// 如果需要移除分号内容，则调用内部方法移除分号内容，否则移除会话ID
		return (this.removeSemicolonContent ?
				removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
	}

	private static String removeSemicolonContentInternal(String requestUri) {
		// 查找分号的索引
		int semicolonIndex = requestUri.indexOf(';');

		// 如果没有找到分号，则返回原始URI
		if (semicolonIndex == -1) {
			return requestUri;
		}
		// 构建处理 请求URI 的 StringBuilder
		StringBuilder sb = new StringBuilder(requestUri);
		while (semicolonIndex != -1) {
			// 查找下一个斜杠的索引
			int slashIndex = sb.indexOf("/", semicolonIndex + 1);
			// 如果没有找到斜杠，则返回分号之前的部分
			if (slashIndex == -1) {
				return sb.substring(0, semicolonIndex);
			}
			// 删除分号到下一个斜杠之间的内容
			sb.delete(semicolonIndex, slashIndex);
			// 查找下一个分号的索引
			semicolonIndex = sb.indexOf(";", semicolonIndex);
		}

		// 返回处理后的 URI
		return sb.toString();
	}

	private String removeJsessionid(String requestUri) {
		// 查找字符串 ";jsessionid=" 在 URI 中的索引
		String key = ";jsessionid=";
		int index = requestUri.toLowerCase().indexOf(key);

		// 如果没有找到该字符串，则返回原始 URI
		if (index == -1) {
			return requestUri;
		}

		// 提取分号之前的部分
		String start = requestUri.substring(0, index);

		// 从 ";jsessionid=" 之后开始遍历 URI
		for (int i = index + key.length(); i < requestUri.length(); i++) {
			char c = requestUri.charAt(i);
			// 如果遇到分号或斜杠，则返回分号之后的部分
			if (c == ';' || c == '/') {
				return start + requestUri.substring(i);
			}
		}

		// 如果没有找到分号或斜杠，则返回分号之后的部分
		return start;
	}

	/**
	 * 通过{@link #decodeRequestString}解码给定的URI路径变量，除非{@link #setUrlDecode}设置为{@code true}，
	 * 在这种情况下，假定提取变量的URL路径已经通过调用{@link #getLookupPathForRequest(HttpServletRequest)}进行了解码。
	 *
	 * @param request 当前HTTP请求
	 * @param vars    从URL路径中提取的URI变量
	 * @return 相同的Map或新的Map实例
	 */
	public Map<String, String> decodePathVariables(HttpServletRequest request, Map<String, String> vars) {
		// 如果需要 URL 解码，则直接返回变量
		if (this.urlDecode) {
			return vars;
		} else {
			// 否则，创建一个新的映射来存储已解码的变量
			Map<String, String> decodedVars = CollectionUtils.newLinkedHashMap(vars.size());
			// 遍历原始变量映射，并对每个值进行解码，然后存储到新的映射中
			vars.forEach((key, value) -> decodedVars.put(key, decodeInternal(request, value)));
			return decodedVars;
		}
	}

	/**
	 * 通过{@link #decodeRequestString}解码给定的矩阵变量，除非{@link #setUrlDecode}设置为{@code true}，
	 * 在这种情况下，假定提取变量的URL路径已经通过调用{@link #getLookupPathForRequest(HttpServletRequest)}进行了解码。
	 *
	 * @param request 当前HTTP请求
	 * @param vars    从URL路径中提取的URI变量
	 * @return 相同的Map或新的Map实例
	 */
	public MultiValueMap<String, String> decodeMatrixVariables(
			HttpServletRequest request, MultiValueMap<String, String> vars) {

		// 如果需要 URL 解码，则直接返回变量
		if (this.urlDecode) {
			return vars;
		} else {
			// 否则，创建一个新的 MultiValueMap 来存储已解码的变量
			MultiValueMap<String, String> decodedVars = new LinkedMultiValueMap<>(vars.size());
			// 遍历原始变量映射，并对每个值进行解码，然后存储到新的 MultiValueMap 中
			vars.forEach((key, values) -> {
				for (String value : values) {
					decodedVars.add(key, decodeInternal(request, value));
				}
			});
			return decodedVars;
		}
	}

	/**
	 * 确定是否应删除请求中Servlet路径末尾的斜杠。
	 *
	 * @param request 当前HTTP请求
	 * @return {@code true} 如果需要删除末尾的斜杠，{@code false} 否则
	 */
	private boolean shouldRemoveTrailingServletPathSlash(HttpServletRequest request) {
		if (request.getAttribute(WEBSPHERE_URI_ATTRIBUTE) == null) {
			// 常规Servlet容器：在任何情况下都表现如预期，
			// 因此末尾的斜杠是"/" url-pattern映射的结果。
			// 不要删除这个斜杠。
			return false;
		}
		// WebSphere合规性标志
		Boolean flagToUse = websphereComplianceFlag;
		if (flagToUse == null) {
			// 如果WebSphere合规标志为空，获取类加载器
			ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
			String className = "com.ibm.ws.webcontainer.WebContainer";
			String methodName = "getWebContainerProperties";
			String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
			boolean flag = false;
			try {
				// 通过反射获取WebSphere容器
				Class<?> cl = classLoader.loadClass(className);
				// 通过反射获取WebSphere容器属性
				Properties prop = (Properties) cl.getMethod(methodName).invoke(null);
				// 获取WebSphere合规标志
				flag = Boolean.parseBoolean(prop.getProperty(propName));
			} catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not introspect WebSphere web container properties: " + ex);
				}
			}
			// 设置合规标志
			flagToUse = flag;
			websphereComplianceFlag = flag;
		}
		// 如果WebSphere配置为完全符合Servlet规范，则不需要操作。
		// 但是，如果它不符合规范，则删除不正确的末尾斜杠！
		return !flagToUse;
	}


	/**
	 * 共享的只读实例，带有默认设置。以下设置适用：
	 * <ul>
	 * <li>{@code alwaysUseFullPath=false}
	 * <li>{@code urlDecode=true}
	 * <li>{@code removeSemicolon=true}
	 * <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING}
	 * </ul>
	 */
	public static final UrlPathHelper defaultInstance = new UrlPathHelper();

	static {
		// 设置为仅读，不允许改变其中的属性
		defaultInstance.setReadOnly();
	}


	/**
	 * 共享的只读实例，用于完整的编码路径。以下设置适用：
	 * <ul>
	 * <li>{@code alwaysUseFullPath=true}
	 * <li>{@code urlDecode=false}
	 * <li>{@code removeSemicolon=false}
	 * <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING}
	 * </ul>
	 */
	public static final UrlPathHelper rawPathInstance = new UrlPathHelper() {

		@Override
		public String removeSemicolonContent(String requestUri) {
			return requestUri;
		}
	};

	static {
		// 设置始终使用完整路径，不进行路径裁剪
		rawPathInstance.setAlwaysUseFullPath(true);

		// 设置不进行 URL 解码
		rawPathInstance.setUrlDecode(false);

		// 设置不移除分号内容
		rawPathInstance.setRemoveSemicolonContent(false);

		// 设置为只读模式，防止修改
		rawPathInstance.setReadOnly();
	}


	/**
	 * 内部类以避免在运行时对Servlet 4的{@link HttpServletMapping}和{@link MappingMatch}的硬依赖。
	 */
	private static class Servlet4Delegate {

		/**
		 * 检查是否可以跳过Servlet路径的确定。
		 *
		 * @param request 当前HTTP请求
		 * @return {@code true} 如果可以跳过Servlet路径的确定，{@code false} 否则
		 */
		public static boolean skipServletPathDetermination(HttpServletRequest request) {
			// 获取包含请求中的 Servlet 映射
			HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
			// 如果映射为空，则获取当前请求的 Servlet 映射
			if (mapping == null) {
				mapping = request.getHttpServletMapping();
			}
			// 获取映射的匹配类型
			MappingMatch match = mapping.getMappingMatch();
			// 返回映射是否匹配，且匹配类型不为路径，或者映射模式为 "/*"
			return (match != null && (!match.equals(MappingMatch.PATH) || mapping.getPattern().equals("/*")));
		}
	}

}
