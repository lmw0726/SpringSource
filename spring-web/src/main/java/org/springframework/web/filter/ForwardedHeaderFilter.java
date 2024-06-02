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

package org.springframework.web.filter;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Supplier;

/**
 * 从 "Forwarded" 和 "X-Forwarded-*" 头中提取值，包装请求和响应，并在以下方法中使它们反映客户端发起的协议和地址：
 * <ul>
 * <li>{@link HttpServletRequest#getServerName() getServerName()}</li>
 * <li>{@link HttpServletRequest#getServerPort() getServerPort()}</li>
 * <li>{@link HttpServletRequest#getScheme() getScheme()}</li>
 * <li>{@link HttpServletRequest#isSecure() isSecure()}</li>
 * <li>{@link HttpServletResponse#sendRedirect(String) sendRedirect(String)}</li>
 * </ul>
 *
 * <p>由于应用程序无法知道头部是由代理添加的（按照预期的方式）还是由恶意客户端添加的，因此对于转发的头部存在安全性考虑。
 * 这就是为什么应该配置一个信任边界的代理，以删除来自外部的不受信任的转发头部。
 *
 * <p>您还可以使用 {@link #setRemoveOnly removeOnly} 配置 ForwardedHeaderFilter，
 * 在这种情况下，它将删除但不使用头部。
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 4.3
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	/**
	 * 转发标头名称
	 */
	private static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
	}

	/**
	 * 是否丢弃并忽略转发头
	 */
	private boolean removeOnly;

	/**
	 * 是否使用相对重定向
	 */
	private boolean relativeRedirects;


	/**
	 * 启用仅删除 "Forwarded" 或 "X-Forwarded-*" 头的模式，并忽略其中的信息。
	 *
	 * @param removeOnly 是否丢弃并忽略转发头
	 * @since 4.3.9
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * 使用此属性来启用相对重定向，如 {@link RelativeRedirectFilter} 中所述，并且还使用与该过滤器相同的响应包装器，
	 * 或者如果两者都配置，则只有一个将被包装。
	 * <p>默认情况下，如果将此属性设置为 false，则 {@link HttpServletResponse#sendRedirect(String)} 的调用将被覆盖，
	 * 以便将相对 URL 转换为绝对 URL，还考虑了转发的头部。
	 *
	 * @param relativeRedirects 是否使用相对重定向
	 * @since 4.3.10
	 */
	public void setRelativeRedirects(boolean relativeRedirects) {
		this.relativeRedirects = relativeRedirects;
	}


	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// 遍历所有可能的转发头名称
		for (String headerName : FORWARDED_HEADER_NAMES) {
			if (request.getHeader(headerName) != null) {
				// 如果请求头中存在当前遍历的转发头名称，则返回 false
				return false;
			}
		}

		// 如果遍历完所有转发头名称都没有找到匹配的，则返回 true
		return true;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		// 如果设置了仅移除转发头
		if (this.removeOnly) {
			// 创建一个 ForwardedHeaderRemovingRequest 包装器，用于移除请求中的转发头信息
			ForwardedHeaderRemovingRequest wrappedRequest = new ForwardedHeaderRemovingRequest(request);

			// 将包装后的请求和原始响应传递给过滤器链
			filterChain.doFilter(wrappedRequest, response);
		} else {
			// 如果没有设置仅移除转发头
			// 创建一个 ForwardedHeaderExtractingRequest 包装器，用于提取请求中的转发头信息
			HttpServletRequest wrappedRequest = new ForwardedHeaderExtractingRequest(request);

			// 如果设置了相对重定向，则创建一个相对重定向响应包装器，否则创建一个 ForwardedHeaderExtractingResponse 包装器
			HttpServletResponse wrappedResponse = this.relativeRedirects ?
					RelativeRedirectResponseWrapper.wrapIfNecessary(response, HttpStatus.SEE_OTHER) :
					new ForwardedHeaderExtractingResponse(response, wrappedRequest);

			// 将包装后的请求和响应传递给过滤器链
			filterChain.doFilter(wrappedRequest, wrappedResponse);
		}
	}

	@Override
	protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
											   FilterChain filterChain) throws ServletException, IOException {

		doFilterInternal(request, response, filterChain);
	}

	/**
	 * 隐藏 "Forwarded" 或 "X-Forwarded-*" 头。
	 */
	private static class ForwardedHeaderRemovingRequest extends HttpServletRequestWrapper {
		/**
		 * 隐藏转发请求头后的请求头名称 —— 请求头值映射
		 */
		private final Map<String, List<String>> headers;

		public ForwardedHeaderRemovingRequest(HttpServletRequest request) {
			super(request);
			this.headers = initHeaders(request);
		}

		private static Map<String, List<String>> initHeaders(HttpServletRequest request) {
			// 创建一个不区分大小写的 LinkedHashMap，用于存储请求头信息
			Map<String, List<String>> headers = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);

			// 获取请求头名称的枚举
			Enumeration<String> names = request.getHeaderNames();

			// 遍历所有请求头名称
			while (names.hasMoreElements()) {
				// 获取当前请求头名称
				String name = names.nextElement();

				// 如果当前请求头名称不在转发头名称列表中，则将其添加到 headers 中
				if (!FORWARDED_HEADER_NAMES.contains(name)) {
					headers.put(name, Collections.list(request.getHeaders(name)));
				}
			}

			// 返回包含请求头信息的 headers 对象
			return headers;
		}

		// 重写标头访问器以不公开转发的标头

		@Override
		@Nullable
		public String getHeader(String name) {
			List<String> value = this.headers.get(name);
			return (CollectionUtils.isEmpty(value) ? null : value.get(0));
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> value = this.headers.get(name);
			return (Collections.enumeration(value != null ? value : Collections.emptySet()));
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(this.headers.keySet());
		}
	}


	/**
	 * 提取并使用 "Forwarded" 或 "X-Forwarded-*" 头。
	 */
	private static class ForwardedHeaderExtractingRequest extends ForwardedHeaderRemovingRequest {
		/**
		 * 方案名称
		 */
		@Nullable
		private final String scheme;

		/**
		 * 是否是安全协议
		 */
		private final boolean secure;

		/**
		 * 地址
		 */
		@Nullable
		private final String host;

		/**
		 * 端口号
		 */
		private final int port;

		/**
		 * 远程地址
		 */
		@Nullable
		private final InetSocketAddress remoteAddress;

		/**
		 * 转发前缀提取器
		 */
		private final ForwardedPrefixExtractor forwardedPrefixExtractor;


		ForwardedHeaderExtractingRequest(HttpServletRequest servletRequest) {
			super(servletRequest);

			// 创建一个 ServerHttpRequest 对象，用于封装 Servlet 请求
			ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

			// 使用请求构建器从 ServerHttpRequest 构建 UriComponents 对象
			UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request).build();

			// 获取请求的端口号
			int port = uriComponents.getPort();

			// 获取请求的协议、主机和端口信息，并设置到当前对象的相应字段中
			this.scheme = uriComponents.getScheme();
			this.secure = "https".equals(this.scheme) || "wss".equals(this.scheme);
			this.host = uriComponents.getHost();
			this.port = (port == -1 ? (this.secure ? 443 : 80) : port);

			// 解析请求的远程地址
			this.remoteAddress = UriComponentsBuilder.parseForwardedFor(request, request.getRemoteAddress());

			// 构建基本的URL字符串，供 ForwardedPrefixExtractor 使用
			String baseUrl = this.scheme + "://" + this.host + (port == -1 ? "" : ":" + port);

			// 创建 ForwardedPrefixExtractor 对象，用于提取请求的转发前缀信息
			Supplier<HttpServletRequest> delegateRequest = () -> (HttpServletRequest) getRequest();
			this.forwardedPrefixExtractor = new ForwardedPrefixExtractor(delegateRequest, baseUrl);
		}


		@Override
		@Nullable
		public String getScheme() {
			return this.scheme;
		}

		@Override
		@Nullable
		public String getServerName() {
			return this.host;
		}

		@Override
		public int getServerPort() {
			return this.port;
		}

		@Override
		public boolean isSecure() {
			return this.secure;
		}

		@Override
		public String getContextPath() {
			return this.forwardedPrefixExtractor.getContextPath();
		}

		@Override
		public String getRequestURI() {
			return this.forwardedPrefixExtractor.getRequestUri();
		}

		@Override
		public StringBuffer getRequestURL() {
			return this.forwardedPrefixExtractor.getRequestUrl();
		}

		@Override
		@Nullable
		public String getRemoteHost() {
			return (this.remoteAddress != null ? this.remoteAddress.getHostString() : super.getRemoteHost());
		}

		@Override
		@Nullable
		public String getRemoteAddr() {
			return (this.remoteAddress != null ? this.remoteAddress.getHostString() : super.getRemoteAddr());
		}

		@Override
		public int getRemotePort() {
			return (this.remoteAddress != null ? this.remoteAddress.getPort() : super.getRemotePort());
		}
	}


	/**
	 * 负责处理contextPath、requestURI和requestURL，并考虑到转发头的影响，还考虑到底层代理请求路径的更改（例如在Servlet FORWARD时）。
	 */
	private static class ForwardedPrefixExtractor {
		/**
		 * 请求提供者
		 */
		private final Supplier<HttpServletRequest> delegate;

		/**
		 * 基础URL
		 */
		private final String baseUrl;

		/**
		 * 实际请求URI
		 */
		private String actualRequestUri;

		/**
		 * 转发后的前缀
		 */
		@Nullable
		private final String forwardedPrefix;

		/**
		 * 请求URI
		 */
		@Nullable
		private String requestUri;

		/**
		 * 请求URL
		 */
		private String requestUrl;


		/**
		 * 使用所需的信息构造函数。
		 *
		 * @param delegateRequest 当前HttpServletRequestWrapper的请求的提供者，可能在转发期间更改（例如Tomcat）。
		 * @param baseUrl         基于转发头的主机、方案和端口
		 */
		public ForwardedPrefixExtractor(Supplier<HttpServletRequest> delegateRequest, String baseUrl) {
			this.delegate = delegateRequest;
			this.baseUrl = baseUrl;
			this.actualRequestUri = delegateRequest.get().getRequestURI();
			// 初始化转换后的前缀
			this.forwardedPrefix = initForwardedPrefix(delegateRequest.get());
			this.requestUri = initRequestUri();
			// 保持顺序：依赖于requestUri
			this.requestUrl = initRequestUrl();
		}

		@Nullable
		private static String initForwardedPrefix(HttpServletRequest request) {
			// 初始化结果字符串为 null
			String result = null;

			// 获取所有请求头的枚举
			Enumeration<String> names = request.getHeaderNames();

			// 遍历所有请求头名称
			while (names.hasMoreElements()) {
				// 获取当前请求头名称
				String name = names.nextElement();

				// 如果当前请求头名称为 X-Forwarded-Prefix（不区分大小写），则将其值赋给 result 变量
				if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
					result = request.getHeader(name);
				}
			}

			// 如果 result 不为 null，则处理转发前缀信息
			if (result != null) {
				// 创建 StringBuilder 对象，用于拼接处理后的转发前缀
				StringBuilder prefix = new StringBuilder(result.length());

				// 将原始转发前缀字符串分割为数组，并处理每个元素
				String[] rawPrefixes = StringUtils.tokenizeToStringArray(result, ",");
				for (String rawPrefix : rawPrefixes) {
					int endIndex = rawPrefix.length();
					// 去除每个前缀字符串末尾的斜杠字符
					while (endIndex > 0 && rawPrefix.charAt(endIndex - 1) == '/') {
						endIndex--;
					}
					// 拼接处理后的前缀字符串
					prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
				}
				// 返回处理后的前缀字符串
				return prefix.toString();
			}

			// 如果没有找到 X-Forwarded-Prefix 请求头，则返回 null
			return null;
		}

		@Nullable
		private String initRequestUri() {
			// 如果存在转发前缀信息
			if (this.forwardedPrefix != null) {
				// 构建完整的 URL 路径，包括转发前缀和委托请求的路径
				return this.forwardedPrefix +
						UrlPathHelper.rawPathInstance.getPathWithinApplication(this.delegate.get());
			}
			// 如果转发前缀信息为 null，则返回 null
			return null;
		}

		private String initRequestUrl() {
			return this.baseUrl + (this.requestUri != null ? this.requestUri : this.delegate.get().getRequestURI());
		}


		public String getContextPath() {
			return (this.forwardedPrefix != null ? this.forwardedPrefix : this.delegate.get().getContextPath());
		}

		public String getRequestUri() {
			// 如果请求 URI 为空，则返回委托请求的 URI
			if (this.requestUri == null) {
				return this.delegate.get().getRequestURI();
			}

			// 如果需要重新计算路径，则重新计算路径信息
			recalculatePathsIfNecessary();
			// 返回请求 URI
			return this.requestUri;
		}

		public StringBuffer getRequestUrl() {
			// 如果需要重新计算路径，则重新计算路径信息
			recalculatePathsIfNecessary();
			return new StringBuffer(this.requestUrl);
		}

		private void recalculatePathsIfNecessary() {
			// 如果实际请求 URI 和委托请求的 URI 不相等
			if (!this.actualRequestUri.equals(this.delegate.get().getRequestURI())) {
				// 表示底层路径发生了变化（例如 Servlet FORWARD）。
				// 更新实际请求 URI 为委托请求的 URI
				this.actualRequestUri = this.delegate.get().getRequestURI();
				// 重新初始化请求 URI、请求 URL（保持顺序：依赖于请求 URI）
				this.requestUri = initRequestUri();
				this.requestUrl = initRequestUrl();
			}
		}
	}


	private static class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {
		/**
		 * 文件夹分隔符
		 */
		private static final String FOLDER_SEPARATOR = "/";

		/**
		 * 请求
		 */
		private final HttpServletRequest request;


		ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}


		@Override
		public void sendRedirect(String location) throws IOException {

			// 创建 UriComponentsBuilder 对象，用于处理重定向的目标地址
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);
			UriComponents uriComponents = builder.build();

			// 如果目标地址是绝对地址，则直接进行重定向
			if (uriComponents.getScheme() != null) {
				super.sendRedirect(location);
				return;
			}

			// 如果目标地址是网络路径引用，则补充协议信息后进行重定向
			if (location.startsWith("//")) {
				String scheme = this.request.getScheme();
				super.sendRedirect(builder.scheme(scheme).toUriString());
				return;
			}

			// 如果目标地址是相对路径，则根据情况进行处理后再进行重定向
			String path = uriComponents.getPath();
			if (path != null) {
				// 相对于 Servlet 容器根路径或当前请求路径的相对路径
				path = (path.startsWith(FOLDER_SEPARATOR) ? path :
						StringUtils.applyRelativePath(this.request.getRequestURI(), path));
			}

			// 构建重定向的最终地址，并进行重定向
			String result = UriComponentsBuilder
					.fromHttpRequest(new ServletServerHttpRequest(this.request))
					.replacePath(path)
					.replaceQuery(uriComponents.getQuery())
					.fragment(uriComponents.getFragment())
					.build().normalize().toUriString();

			super.sendRedirect(result);
		}
	}

}
