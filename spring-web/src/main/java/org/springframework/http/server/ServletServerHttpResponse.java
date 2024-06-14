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

package org.springframework.http.server;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 基于 {@link HttpServletResponse} 的 {@link ServerHttpResponse} 实现。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ServletServerHttpResponse implements ServerHttpResponse {
	/**
	 * Servlet响应
	 */
	private final HttpServletResponse servletResponse;

	/**
	 * 头部信息
	 */
	private final HttpHeaders headers;

	/**
	 * 是否将头部写入过输出流
	 */
	private boolean headersWritten = false;

	/**
	 * 是否使用过主体
	 */
	private boolean bodyUsed = false;

	/**
	 * 是否是只读头部
	 */
	@Nullable
	private HttpHeaders readOnlyHeaders;


	/**
	 * 使用给定的 {@link HttpServletResponse} 构造一个 ServletServerHttpResponse 的新实例。
	 *
	 * @param servletResponse Servlet响应对象
	 */
	public ServletServerHttpResponse(HttpServletResponse servletResponse) {
		Assert.notNull(servletResponse, "HttpServletResponse must not be null");
		this.servletResponse = servletResponse;
		this.headers = new ServletResponseHttpHeaders();
	}


	/**
	 * 返回此对象基于的 {@code HttpServletResponse}。
	 */
	public HttpServletResponse getServletResponse() {
		return this.servletResponse;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.servletResponse.setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		// 如果只读头部不为空，则直接返回只读头部
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		} else if (this.headersWritten) {
			// 如果头部已经被写入过，则创建只读头部并返回
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		} else {
			// 否则，直接返回当前的可写头部
			return this.headers;
		}
	}

	@Override
	public OutputStream getBody() throws IOException {
		// 设置响应体已被使用的标志为 true
		this.bodyUsed = true;

		// 调用写入响应头部的方法
		writeHeaders();

		// 返回 Servlet 响应对象的输出流
		return this.servletResponse.getOutputStream();
	}

	@Override
	public void flush() throws IOException {
		// 调用写入响应头部的方法
		writeHeaders();

		// 如果响应体已经被使用过
		if (this.bodyUsed) {
			// 刷新 Servlet 响应的缓冲区
			this.servletResponse.flushBuffer();
		}
	}

	@Override
	public void close() {
		writeHeaders();
	}

	private void writeHeaders() {
		// 如果头部信息尚未被写入
		if (!this.headersWritten) {
			// 遍历获取到的头部信息
			getHeaders().forEach((headerName, headerValues) -> {
				// 将每个头部信息写入到 Servlet 响应中
				for (String headerValue : headerValues) {
					this.servletResponse.addHeader(headerName, headerValue);
				}
			});

			// 如果 Servlet 响应的内容类型为空，但是头部信息中有内容类型
			if (this.servletResponse.getContentType() == null && this.headers.getContentType() != null) {
				// 设置 Servlet 响应的内容类型
				this.servletResponse.setContentType(this.headers.getContentType().toString());
			}

			// 如果 Servlet 响应的字符编码为空，但是头部信息中有内容类型且其字符集不为空
			if (this.servletResponse.getCharacterEncoding() == null && this.headers.getContentType() != null &&
					this.headers.getContentType().getCharset() != null) {
				// 设置 Servlet 响应的字符编码
				this.servletResponse.setCharacterEncoding(this.headers.getContentType().getCharset().name());
			}

			// 获取头部信息中的内容长度
			long contentLength = getHeaders().getContentLength();
			// 如果内容长度有效，则设置 Servlet 响应的内容长度
			if (contentLength != -1) {
				this.servletResponse.setContentLengthLong(contentLength);
			}

			// 标记头部信息已经被写入
			this.headersWritten = true;
		}
	}


	/**
	 * 通过扩展 HttpHeaders 来实现在底层的 HttpServletResponse 中查找已存在的头部的功能。
	 *
	 * <p>其意图仅仅是暴露通过 HttpServletResponse 可用的内容，即通过名称查找特定头部值的能力。所有其他的
	 * 与 Map 相关的操作（如迭代、移除等）仅适用于通过 HttpHeaders 方法直接添加的值。
	 *
	 * @since 4.0.3
	 */
	private class ServletResponseHttpHeaders extends HttpHeaders {

		private static final long serialVersionUID = 3410708522401046302L;

		@Override
		public boolean containsKey(Object key) {
			return (super.containsKey(key) || (get(key) != null));
		}

		@Override
		@Nullable
		public String getFirst(String headerName) {
			// 如果头部名称忽略大小写后等于CONTENT_TYPE
			if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
				// 先从父类获取Content-Type值
				String value = super.getFirst(headerName);
				// 如果父类返回的值不为null，则返回该值，否则从servletResponse获取Content-Type值
				return (value != null ? value : servletResponse.getHeader(headerName));
			} else {
				// 如果头部名称不是CONTENT_TYPE，则从servletResponse获取头部值
				String value = servletResponse.getHeader(headerName);
				// 如果servletResponse返回的值不为null，则返回该值，否则从父类获取头部值
				return (value != null ? value : super.getFirst(headerName));
			}
		}

		@Override
		public List<String> get(Object key) {
			Assert.isInstanceOf(String.class, key, "Key must be a String-based header name");

			// 将键转换为字符串类型
			String headerName = (String) key;

			// 如果键名为 CONTENT_TYPE（不区分大小写）
			if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
				// Content-Type 作为覆盖写入，因此不需要合并
				return Collections.singletonList(getFirst(headerName));
			}

			// 获取 Servlet 响应中指定键名的头信息值集合
			Collection<String> values1 = servletResponse.getHeaders(headerName);

			// 如果响应头已经被写入
			if (headersWritten) {
				// 返回响应头中指定键名的头信息值集合的副本
				return new ArrayList<>(values1);
			}

			// 检查响应头值集合是否为空
			boolean isEmpty1 = CollectionUtils.isEmpty(values1);

			// 获取当前对象中指定键名的头信息值集合
			List<String> values2 = super.get(key);

			// 检查当前对象中指定键名的头信息值集合是否为空
			boolean isEmpty2 = CollectionUtils.isEmpty(values2);

			// 如果两个头信息值集合都为空
			if (isEmpty1 && isEmpty2) {
				// 返回 null，表示不存在该键名的头信息值
				return null;
			}

			// 否则，合并两个头信息值集合
			List<String> values = new ArrayList<>();
			// 如果 Servlet 响应中的头信息值集合不为空，则将其添加到合并后的集合中
			if (!isEmpty1) {
				values.addAll(values1);
			}
			// 如果当前对象中的头信息值集合不为空，则将其添加到合并后的集合中
			if (!isEmpty2) {
				values.addAll(values2);
			}
			// 返回合并后的头信息值集合
			return values;
		}
	}

}
