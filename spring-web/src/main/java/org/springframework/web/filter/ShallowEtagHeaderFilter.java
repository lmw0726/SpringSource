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

package org.springframework.web.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * {@link javax.servlet.Filter} 根据响应内容生成一个 {@code ETag} 值。
 * 该 ETag 值与请求的 {@code If-None-Match} 头进行比较。
 * 如果这些头相等，则不发送响应内容，而是发送 {@code 304 "Not Modified"} 状态。
 *
 * <p>由于 ETag 是基于响应内容的，因此响应（例如 {@link org.springframework.web.servlet.View}）仍然会被渲染。
 * 因此，该过滤器只节省带宽，而不节省服务器性能。
 *
 * <p><b>注意：</b> 从 Spring Framework 5.0 开始，此过滤器使用基于 Servlet 3.1 API 的请求/响应装饰器。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {
	/**
	 * 不存储指定
	 */
	private static final String DIRECTIVE_NO_STORE = "no-store";

	/**
	 * 流属性
	 */
	private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";

	/**
	 * 写入响应的 ETag 值是否应该是弱 ETag
	 */
	private boolean writeWeakETag = false;


	/**
	 * 设置写入响应的 ETag 值是否应该是弱 ETag，按照 RFC 7232 规定。
	 * <p>应在 {@code web.xml} 中过滤器定义的 {@code <init-param>} 中使用参数名 "writeWeakETag" 进行配置。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 section 2.3</a>
	 * @since 4.3
	 */
	public void setWriteWeakETag(boolean writeWeakETag) {
		this.writeWeakETag = writeWeakETag;
	}

	/**
	 * 返回写入响应的 ETag 值是否应该是弱 ETag，按照 RFC 7232 规定。
	 *
	 * @since 4.3
	 */
	public boolean isWriteWeakETag() {
		return this.writeWeakETag;
	}


	/**
	 * 默认值为 {@code false}，因此过滤器可以延迟生成 ETag 直到最后一个异步调度线程。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 初始化一个用于处理的响应对象，默认为传入的响应对象
		HttpServletResponse responseToUse = response;

		// 如果请求不是异步分派并且响应对象，不是 ConditionalContentCachingResponseWrapper 的实例
		if (!isAsyncDispatch(request) && !(response instanceof ConditionalContentCachingResponseWrapper)) {
			// 用 ConditionalContentCachingResponseWrapper 包装响应对象
			responseToUse = new ConditionalContentCachingResponseWrapper(response, request);
		}

		// 调用过滤链，传递请求和处理后的响应对象
		filterChain.doFilter(request, responseToUse);

		// 如果请求没有开始异步处理并且没有禁用内容缓存
		if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
			// 更新响应对象
			updateResponse(request, responseToUse);
		}
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 从原生响应中获取 ConditionalContentCachingResponseWrapper 对象
		ConditionalContentCachingResponseWrapper wrapper =
				WebUtils.getNativeResponse(response, ConditionalContentCachingResponseWrapper.class);

		Assert.notNull(wrapper, "ContentCachingResponseWrapper not found");

		// 获取原生的 HttpServletResponse 对象
		HttpServletResponse rawResponse = (HttpServletResponse) wrapper.getResponse();

		// 如果请求和响应满足生成 ETag 的条件
		if (isEligibleForEtag(request, wrapper, wrapper.getStatus(), wrapper.getContentInputStream())) {
			// 获取响应头中的 ETag
			String eTag = wrapper.getHeader(HttpHeaders.ETAG);
			// 如果响应头中没有 ETag
			if (!StringUtils.hasText(eTag)) {
				// 生成 ETag 的值并设置到响应头中
				eTag = generateETagHeaderValue(wrapper.getContentInputStream(), this.writeWeakETag);
				rawResponse.setHeader(HttpHeaders.ETAG, eTag);
			}
			// 检查客户端的请求是否与当前资源的 ETag 匹配，如果匹配则返回 304 Not Modified
			if (new ServletWebRequest(request, rawResponse).checkNotModified(eTag)) {
				return;
			}
		}

		// 将响应体复制到原生响应中
		wrapper.copyBodyToResponse();
	}

	/**
	 * 是否应为给定的请求和响应交换计算 ETag。默认情况下，如果以下所有条件都匹配，则为 {@code true}：
	 * <ul>
	 * <li>响应未提交。</li>
	 * <li>响应状态代码在 {@code 2xx} 系列中。</li>
	 * <li>请求方法是 GET。</li>
	 * <li>响应的 Cache-Control 头不包含 "no-store"（或根本不存在）。</li>
	 * </ul>
	 *
	 * @param request            HTTP 请求
	 * @param response           HTTP 响应
	 * @param responseStatusCode HTTP 响应状态代码
	 * @param inputStream        响应体
	 * @return 如果符合 ETag 生成条件，则为 {@code true}，否则为 {@code false}
	 */
	protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
										int responseStatusCode, InputStream inputStream) {

		// 如果响应未提交且响应状态码在 200 到 299 之间，且请求方法为 GET
		if (!response.isCommitted() &&
				responseStatusCode >= 200 && responseStatusCode < 300 &&
				HttpMethod.GET.matches(request.getMethod())) {
			// 检查响应头中是否包含 Cache-Control，并且不包含 no-store 指令
			String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
			return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
		}

		// 否则返回 false
		return false;
	}

	/**
	 * 从给定的响应体字节数组生成 ETag 头值。
	 * <p>默认实现生成一个 MD5 哈希。
	 *
	 * @param inputStream 响应体作为 InputStream
	 * @param isWeak      是否生成的 ETag 应该是弱 ETag
	 * @return ETag 头值
	 * @throws IOException 如果发生 I/O 异常
	 * @see org.springframework.util.DigestUtils
	 */
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		// 创建一个 StringBuilder 对象，初始容量为 37（W/ + " + 0 + 32bits md5 hash + "）
		StringBuilder builder = new StringBuilder(37);
		// 如果是弱 ETag，则在 builder 中添加 "W/"
		if (isWeak) {
			builder.append("W/");
		}
		// 添加 "0"
		builder.append("\"0");
		// 使用 DigestUtils 将 inputStream 的 MD5 哈希值追加到 builder 中
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		// 添加 '"'
		builder.append('"');
		// 返回构建好的 ETag 字符串
		return builder.toString();
	}


	/**
	 * 此方法可用于禁用 ShallowEtagHeaderFilter 的内容缓存响应包装器。
	 * 主要原因是不需要缓存的流式场景，也不需要 ETag。
	 * <p><strong>注意：</strong> 必须在写入响应之前调用此方法，以便完整的响应内容写入时无需缓存。
	 *
	 * @since 4.2
	 */
	public static void disableContentCaching(ServletRequest request) {
		Assert.notNull(request, "ServletRequest must not be null");
		request.setAttribute(STREAMING_ATTRIBUTE, true);
	}

	private static boolean isContentCachingDisabled(HttpServletRequest request) {
		return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
	}


	/**
	 * 如果 {@link #isContentCachingDisabled}，则返回原始的 OutputStream，
	 * 而不是执行缓存的 OutputStream。
	 */
	private static class ConditionalContentCachingResponseWrapper extends ContentCachingResponseWrapper {
		/**
		 * Http请求
		 */
		private final HttpServletRequest request;

		ConditionalContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return (isContentCachingDisabled(this.request) || hasETag() ?
					getResponse().getOutputStream() : super.getOutputStream());
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return (isContentCachingDisabled(this.request) || hasETag() ?
					getResponse().getWriter() : super.getWriter());
		}

		private boolean hasETag() {
			return StringUtils.hasText(getHeader(HttpHeaders.ETAG));
		}
	}

}
