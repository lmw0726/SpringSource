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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * 一个过滤器，包装了{@link HttpServletResponse}，并覆盖其{@link HttpServletResponse#encodeURL(String) encodeURL}方法，
 * 以便将内部资源请求URL转换为用于外部使用的公共URL路径。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public class ResourceUrlEncodingFilter extends GenericFilterBean {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ResourceUrlEncodingFilter.class);


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 确保request和response都是HttpServletRequest和HttpServletResponse的实例，否则抛出ServletException
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("ResourceUrlEncodingFilter only supports HTTP requests");
		}

		// 创建包装请求
		ResourceUrlEncodingRequestWrapper wrappedRequest =
				new ResourceUrlEncodingRequestWrapper((HttpServletRequest) request);
		// 创建包装响应
		ResourceUrlEncodingResponseWrapper wrappedResponse =
				new ResourceUrlEncodingResponseWrapper(wrappedRequest, (HttpServletResponse) response);

		// 调用过滤器链处理包装后的请求和响应对象
		filterChain.doFilter(wrappedRequest, wrappedResponse);
	}


	private static class ResourceUrlEncodingRequestWrapper extends HttpServletRequestWrapper {

		/**
		 * 资源URL提供者
		 */
		@Nullable
		private ResourceUrlProvider resourceUrlProvider;

		/**
		 * 寻找路径的索引
		 */
		@Nullable
		private Integer indexLookupPath;

		/**
		 * 查找路径前缀
		 */
		private String prefixLookupPath = "";

		ResourceUrlEncodingRequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
			// 调用父类方法设置属性
			super.setAttribute(name, value);

			// 如果设置的属性是 ResourceUrlProvider，则进行处理
			if (ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR.equals(name)) {
				// 如果属性值是 ResourceUrlProvider 类型
				if (value instanceof ResourceUrlProvider) {
					// 初始化查找路径
					initLookupPath((ResourceUrlProvider) value);
				}
			}
		}

		private void initLookupPath(ResourceUrlProvider urlProvider) {
			// 设置资源URL提供者
			this.resourceUrlProvider = urlProvider;

			// 如果查找路径索引为null，则初始化
			if (this.indexLookupPath == null) {
				// 获取URL路径助手
				UrlPathHelper pathHelper = this.resourceUrlProvider.getUrlPathHelper();
				// 获取请求URI
				String requestUri = pathHelper.getRequestUri(this);
				// 获取查找路径
				String lookupPath = pathHelper.getLookupPathForRequest(this);

				// 获取查找路径在请求URI中的索引位置
				this.indexLookupPath = requestUri.lastIndexOf(lookupPath);
				// 如果未找到查找路径，则抛出异常
				if (this.indexLookupPath == -1) {
					throw new LookupPathIndexException(lookupPath, requestUri);
				}

				// 设置前缀查找路径为请求URI中查找路径之前的部分
				this.prefixLookupPath = requestUri.substring(0, this.indexLookupPath);

				// 如果查找路径包含斜杠，但请求URI不包含斜杠，则进行额外处理
				if (StringUtils.matchesCharacter(lookupPath, '/') && !StringUtils.matchesCharacter(requestUri, '/')) {
					// 获取上下文路径
					String contextPath = pathHelper.getContextPath(this);
					// 如果请求URI等于上下文路径
					if (requestUri.equals(contextPath)) {
						// 将查找路径索引设置为请求URI的长度
						this.indexLookupPath = requestUri.length();
						// 将查找路径前缀设置为请求URI
						this.prefixLookupPath = requestUri;
					}
				}
			}
		}

		@Nullable
		public String resolveUrlPath(String url) {
			// 如果资源URL提供者为null，则返回null
			if (this.resourceUrlProvider == null) {
				logger.trace("ResourceUrlProvider not available via request attribute " +
						ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
				return null;
			}

			// 如果索引查找路径不为null，且URL以前缀查找路径开头，则进行进一步处理
			if (this.indexLookupPath != null && url.startsWith(this.prefixLookupPath)) {
				// 获取URL后缀的索引位置
				int suffixIndex = getEndPathIndex(url);
				// 获取URL的后缀部分
				String suffix = url.substring(suffixIndex);
				// 获取URL中查找路径的部分
				String lookupPath = url.substring(this.indexLookupPath, suffixIndex);
				// 使用资源URL提供者获取查找路径
				lookupPath = this.resourceUrlProvider.getForLookupPath(lookupPath);
				if (lookupPath != null) {
					// 如果查找路径不为null，则返回查找路径前缀、查找路径、后缀拼接的结果
					return this.prefixLookupPath + lookupPath + suffix;
				}
			}

			// 如果未找到可用的查找路径替代，则返回null
			return null;
		}

		private int getEndPathIndex(String path) {
			// 获取路径中问号的位置
			int end = path.indexOf('?');
			// 获取路径中井号的位置
			int fragmentIndex = path.indexOf('#');
			// 如果路径中存在井号，并且问号不存在或者井号的位置在问号之前
			if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
				// 更新结束位置为井号的位置
				end = fragmentIndex;
			}
			// 如果 结束位置 为-1，则将其更新为路径的长度
			if (end == -1) {
				end = path.length();
			}
			return end;
		}
	}


	private static class ResourceUrlEncodingResponseWrapper extends HttpServletResponseWrapper {
		/**
		 * 包装的请求
		 */
		private final ResourceUrlEncodingRequestWrapper request;

		ResourceUrlEncodingResponseWrapper(ResourceUrlEncodingRequestWrapper request, HttpServletResponse wrapped) {
			super(wrapped);
			this.request = request;
		}

		@Override
		public String encodeURL(String url) {
			// 使用请求对象解析URL路径
			String urlPath = this.request.resolveUrlPath(url);
			if (urlPath != null) {
				// 如果解析得到的URL路径不为空，则调用父类的encodeURL方法对其进行编码
				return super.encodeURL(urlPath);
			}
			// 如果解析得到的URL路径为空，则直接调用父类的encodeURL方法对原始URL进行编码
			return super.encodeURL(url);
		}
	}


	/**
	 * 运行时异常，可进一步获取（至ResourceUrlProviderExposingInterceptor），
	 * 并将其重新抛出为ServletRequestBindingException，以导致400响应。
	 */
	@SuppressWarnings("serial")
	static class LookupPathIndexException extends IllegalArgumentException {

		LookupPathIndexException(String lookupPath, String requestUri) {
			super("Failed to find lookupPath '" + lookupPath + "' within requestUri '" + requestUri + "'. " +
					"This could be because the path has invalid encoded characters or isn't normalized.");
		}
	}

}
