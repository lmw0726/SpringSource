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

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.*;

/**
 * 将从{@linkplain #getInputStream()输入流}和{@linkplain #getReader()读取器}中读取的所有内容缓存起来，
 * 并允许通过{@link #getContentAsByteArray()}字节数组检索此内容的{@link HttpServletRequest}包装器。
 *
 * <p>此类作为一个拦截器，只在内容被读取时缓存内容，但不会导致内容被读取。这意味着如果请求内容没有被消耗，
 * 则不会缓存内容，也无法通过{@link #getContentAsByteArray()}检索到内容。
 *
 * <p>例如，被{@link org.springframework.web.filter.AbstractRequestLoggingFilter}使用。
 * 注意：从Spring Framework 5.0开始，此包装器建立在Servlet 3.1 API之上。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @see ContentCachingResponseWrapper
 * @since 4.1.3
 */
public class ContentCachingRequestWrapper extends HttpServletRequestWrapper {

	/**
	 * 表示表单内容类型的常量。
	 */
	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

	/**
	 * 缓存请求内容的字节数组输出流。
	 */
	private final ByteArrayOutputStream cachedContent;

	/**
	 * 内容缓存限制的最大字节数。
	 */
	@Nullable
	private final Integer contentCacheLimit;

	/**
	 * 请求的Servlet输入流。
	 */
	@Nullable
	private ServletInputStream inputStream;

	/**
	 * 请求的缓冲读取器。
	 */
	@Nullable
	private BufferedReader reader;


	/**
	 * 为给定的servlet请求创建一个新的ContentCachingRequestWrapper。
	 *
	 * @param request 原始servlet请求
	 */
	public ContentCachingRequestWrapper(HttpServletRequest request) {
		super(request);
		int contentLength = request.getContentLength();
		this.cachedContent = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
		this.contentCacheLimit = null;
	}

	/**
	 * 为给定的servlet请求创建一个新的ContentCachingRequestWrapper。
	 *
	 * @param request           原始servlet请求
	 * @param contentCacheLimit 每个请求最大缓存字节数
	 * @see #handleContentOverflow(int)
	 * @since 4.3.6
	 */
	public ContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
		super(request);
		this.cachedContent = new ByteArrayOutputStream(contentCacheLimit);
		this.contentCacheLimit = contentCacheLimit;
	}


	@Override
	public ServletInputStream getInputStream() throws IOException {
		// 如果输入流为空，则初始化输入流为 ContentCachingInputStream
		if (this.inputStream == null) {
			this.inputStream = new ContentCachingInputStream(getRequest().getInputStream());
		}
		// 返回输入流
		return this.inputStream;
	}

	@Override
	public String getCharacterEncoding() {
		// 调用父类的 getCharacterEncoding() 方法获取字符编码
		String enc = super.getCharacterEncoding();
		// 如果字符编码不为 null，则返回字符编码；否则返回默认字符编码
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// 如果读取器为空，则初始化读取器
		if (this.reader == null) {
			// 使用请求的输入流和字符编码初始化 BufferedReader
			this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
		}
		// 返回读取器
		return this.reader;
	}

	@Override
	public String getParameter(String name) {
		// 如果缓存的内容大小为 0 并且是表单提交
		if (this.cachedContent.size() == 0 && isFormPost()) {
			// 将请求参数写入缓存的内容
			writeRequestParametersToCachedContent();
		}
		// 调用父类的 getParameter(name) 方法获取参数值
		return super.getParameter(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		// 如果缓存的内容大小为 0 并且是表单提交
		if (this.cachedContent.size() == 0 && isFormPost()) {
			// 将请求参数写入缓存的内容
			writeRequestParametersToCachedContent();
		}
		// 调用父类的 getParameterMap() 方法获取参数映射
		return super.getParameterMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		// 如果缓存的内容大小为 0 并且是表单提交
		if (this.cachedContent.size() == 0 && isFormPost()) {
			// 将请求参数写入缓存的内容
			writeRequestParametersToCachedContent();
		}
		// 调用父类的 getParameterNames() 方法获取参数名枚举
		return super.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		// 如果缓存的内容大小为 0 并且是表单提交
		if (this.cachedContent.size() == 0 && isFormPost()) {
			// 将请求参数写入缓存的内容
			writeRequestParametersToCachedContent();
		}
		// 调用父类的 getParameterValues(name) 方法获取参数值数组
		return super.getParameterValues(name);
	}


	private boolean isFormPost() {
		// 获取请求的内容类型
		String contentType = getContentType();
		// 返回是否为表单提交（内容类型不为空且包含表单类型且请求方法为 POST）
		return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) &&
				HttpMethod.POST.matches(getMethod()));
	}

	private void writeRequestParametersToCachedContent() {
		try {
			// 如果缓存的内容大小为 0
			if (this.cachedContent.size() == 0) {
				// 获取请求的字符编码
				String requestEncoding = getCharacterEncoding();
				// 获取请求参数映射
				Map<String, String[]> form = super.getParameterMap();
				// 遍历请求参数映射
				for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
					String name = nameIterator.next();
					List<String> values = Arrays.asList(form.get(name));
					// 遍历参数值列表
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						// 将参数名进行 URL 编码写入缓存内容
						this.cachedContent.write(URLEncoder.encode(name, requestEncoding).getBytes());
						// 如果参数值不为空
						if (value != null) {
							this.cachedContent.write('=');
							// 将参数值进行 URL 编码写入缓存内容
							this.cachedContent.write(URLEncoder.encode(value, requestEncoding).getBytes());
							// 如果还有下一个参数值，则写入 '&'
							if (valueIterator.hasNext()) {
								this.cachedContent.write('&');
							}
						}
					}
					// 如果还有下一个参数名，则写入 '&'
					if (nameIterator.hasNext()) {
						this.cachedContent.write('&');
					}
				}
			}
		} catch (IOException ex) {
			// 如果发生 IO 异常，则抛出 IllegalStateException 异常
			throw new IllegalStateException("Failed to write request parameters to cached content", ex);
		}
	}

	/**
	 * 将缓存的请求内容作为字节数组返回。
	 * <p>返回的数组大小永远不会超过内容缓存限制。
	 * <p><strong>注意：</strong>从该方法返回的字节数组反映了在调用时已读取的内容量。
	 * 如果应用程序未读取内容，则此方法将返回一个空数组。
	 *
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 */
	public byte[] getContentAsByteArray() {
		return this.cachedContent.toByteArray();
	}

	/**
	 * 处理内容溢出的模板方法：特别是，读取超过指定内容缓存限制的请求体。
	 * <p>默认实现为空。子类可以重写此方法以抛出负载过大异常或类似异常。
	 *
	 * @param contentCacheLimit 刚刚超过的每个请求的最大缓存字节数
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 * @since 4.3.6
	 */
	protected void handleContentOverflow(int contentCacheLimit) {
	}


	private class ContentCachingInputStream extends ServletInputStream {
		/**
		 * Servlet输入流
		 */
		private final ServletInputStream is;

		/**
		 * 是否溢出
		 */
		private boolean overflow = false;

		public ContentCachingInputStream(ServletInputStream is) {
			this.is = is;
		}

		@Override
		public int read() throws IOException {
			// 从输入流中读取一个字节
			int ch = this.is.read();
			// 如果读取到的字节不为 -1 并且未发生溢出
			if (ch != -1 && !this.overflow) {
				// 如果设置了内容缓存限制并且缓存的内容大小达到了限制
				if (contentCacheLimit != null && cachedContent.size() == contentCacheLimit) {
					// 标记发生溢出
					this.overflow = true;
					// 处理内容溢出
					handleContentOverflow(contentCacheLimit);
				} else {
					// 将读取到的字节写入缓存内容
					cachedContent.write(ch);
				}
			}
			// 返回读取到的字节
			return ch;
		}

		@Override
		public int read(byte[] b) throws IOException {
			// 从输入流中读取到缓冲区 b 中，返回读取的字节数
			int count = this.is.read(b);
			// 将读取到的内容写入缓存
			writeToCache(b, 0, count);
			// 返回读取的字节数
			return count;
		}

		private void writeToCache(final byte[] b, final int off, int count) {
			// 如果未发生溢出且读取的字节数大于 0
			if (!this.overflow && count > 0) {
				// 如果设置了内容缓存限制，并且读取的内容加上已有内容的大小大于限制
				if (contentCacheLimit != null &&
						count + cachedContent.size() > contentCacheLimit) {
					// 标记发生溢出
					this.overflow = true;
					// 将缓冲区中的内容写入缓存，保证缓存的内容大小不超过限制
					cachedContent.write(b, off, contentCacheLimit - cachedContent.size());
					// 处理内容溢出
					handleContentOverflow(contentCacheLimit);
					return;
				}
				// 将缓冲区中的内容写入缓存
				cachedContent.write(b, off, count);
			}
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			// 从输入流中读取到缓冲区 b 中，从 off 索引开始，最多读取 len 个字节，返回读取的字节数
			int count = this.is.read(b, off, len);
			// 将读取到的内容写入缓存
			writeToCache(b, off, count);
			// 返回读取的字节数
			return count;
		}

		@Override
		public int readLine(final byte[] b, final int off, final int len) throws IOException {
			// 从输入流中读取一行数据到缓冲区 b 中，从 off 索引开始，最多读取 len 个字节，返回读取的字节数
			int count = this.is.readLine(b, off, len);
			// 将读取到的内容写入缓存
			writeToCache(b, off, count);
			// 返回读取的字节数
			return count;
		}

		@Override
		public boolean isFinished() {
			return this.is.isFinished();
		}

		@Override
		public boolean isReady() {
			return this.is.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			this.is.setReadListener(readListener);
		}
	}

}
