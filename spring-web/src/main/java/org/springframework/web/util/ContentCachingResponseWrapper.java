/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.FastByteArrayOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

/**
 * 将所有写入{@linkplain #getOutputStream()输出流}和{@linkplain #getWriter()写入器}的内容缓存起来，
 * 并允许通过{@link #getContentAsByteArray()}字节数组检索此内容的{@link HttpServletResponse}包装器。
 *
 * <p>例如，被{@link org.springframework.web.filter.ShallowEtagHeaderFilter}使用。
 * 注意：从Spring Framework 5.0开始，此包装器建立在Servlet 3.1 API之上。
 *
 * @author Juergen Hoeller
 * @see ContentCachingRequestWrapper
 * @since 4.1.3
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	/**
	 * 存储内容的快速字节数组输出流
	 */
	private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);

	/**
	 * 输出流
	 */
	@Nullable
	private ServletOutputStream outputStream;

	/**
	 * 打印写入器
	 */
	@Nullable
	private PrintWriter writer;

	/**
	 * 内容长度
	 */
	@Nullable
	private Integer contentLength;


	/**
	 * 为给定的servlet响应创建一个新的ContentCachingResponseWrapper。
	 *
	 * @param response 原始servlet响应
	 */
	public ContentCachingResponseWrapper(HttpServletResponse response) {
		super(response);
	}


	@Override
	public void sendError(int sc) throws IOException {
		// 将响应体复制到响应中（如果标志为 false 则不复制）
		copyBodyToResponse(false);
		try {
			// 发送错误状态码给客户端
			super.sendError(sc);
		} catch (IllegalStateException ex) {
			// 当在调用过晚时可能发生在 Tomcat 上: 回退到静默的 setStatus 方法
			super.setStatus(sc);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void sendError(int sc, String msg) throws IOException {
		// 将响应体复制到响应中（如果标志为 false 则不复制）
		copyBodyToResponse(false);
		try {
			// 发送带有错误消息的错误状态码给客户端
			super.sendError(sc, msg);
		} catch (IllegalStateException ex) {
			// 当在调用过晚时可能发生异常（例如在 Tomcat 上）: 回退到静默的 setStatus 方法设置状态码和消息
			super.setStatus(sc, msg);
		}
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		// 将响应体复制到响应中（如果标志为 false 则不复制）
		copyBodyToResponse(false);
		// 发送重定向给客户端
		super.sendRedirect(location);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// 如果输出流为空，则初始化输出流为 ResponseServletOutputStream
		if (this.outputStream == null) {
			this.outputStream = new ResponseServletOutputStream(getResponse().getOutputStream());
		}
		// 返回输出流
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		// 如果写入器为空，则初始化写入器
		if (this.writer == null) {
			// 获取字符编码
			String characterEncoding = getCharacterEncoding();
			// 根据字符编码初始化 ResponsePrintWriter
			this.writer = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding) :
					new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING));
		}
		// 返回写入器
		return this.writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		// 不要刷新底层响应，因为内容尚未复制到它
	}

	@Override
	public void setContentLength(int len) {
		// 如果 len 大于当前内容的大小，则调整内容的大小为 len
		if (len > this.content.size()) {
			this.content.resize(len);
		}
		// 设置内容长度为 len
		this.contentLength = len;
	}

	// 在运行时覆盖3.1 setContentLengthLong(long) 的Servlet
	@Override
	public void setContentLengthLong(long len) {
		// 如果 len 大于 Integer.MAX_VALUE，则抛出 IllegalArgumentException 异常
		if (len > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum (" +
					Integer.MAX_VALUE + "): " + len);
		}
		// 将 len 转换为 int 类型
		int lenInt = (int) len;
		// 如果 lenInt 大于当前内容的大小，则调整内容的大小为 lenInt
		if (lenInt > this.content.size()) {
			this.content.resize(lenInt);
		}
		// 设置内容长度为 lenInt
		this.contentLength = lenInt;
	}

	@Override
	public void setBufferSize(int size) {
		// 如果 size 大于当前内容的大小，则调整内容的大小为 size
		if (size > this.content.size()) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	/**
	 * 返回响应中指定的状态码。
	 *
	 * @deprecated 自5.2起，建议使用{@link HttpServletResponse#getStatus()}
	 */
	@Deprecated
	public int getStatusCode() {
		return getStatus();
	}

	/**
	 * 将缓存的响应内容作为字节数组返回。
	 */
	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * 返回缓存内容的{@link InputStream}。
	 *
	 * @since 4.2
	 */
	public InputStream getContentInputStream() {
		return this.content.getInputStream();
	}

	/**
	 * 返回缓存内容的当前大小。
	 *
	 * @since 4.2
	 */
	public int getContentSize() {
		return this.content.size();
	}

	/**
	 * 将完整的缓存主体内容复制到响应中。
	 *
	 * @since 4.2
	 */
	public void copyBodyToResponse() throws IOException {
		copyBodyToResponse(true);
	}

	/**
	 * 将缓存的主体内容复制到响应中。
	 *
	 * @param complete 是否为完整的缓存主体内容设置相应的内容长度
	 * @since 4.2
	 */
	protected void copyBodyToResponse(boolean complete) throws IOException {
		// 如果内容大小大于 0
		if (this.content.size() > 0) {
			// 获取原始的 HttpServletResponse
			HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
			// 如果 complete 为 true 或内容长度不为空，并且响应未提交
			if ((complete || this.contentLength != null) && !rawResponse.isCommitted()) {
				// 如果响应头中没有设置 Transfer-Encoding，则设置 Content-Length
				if (rawResponse.getHeader(HttpHeaders.TRANSFER_ENCODING) == null) {
					rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
				}
				// 将内容长度重置为 null
				this.contentLength = null;
			}
			// 将内容写入原始响应的输出流中
			this.content.writeTo(rawResponse.getOutputStream());
			// 重置内容
			this.content.reset();
			// 如果 complete 为 true，则刷新缓冲区
			if (complete) {
				super.flushBuffer();
			}
		}
	}


	private class ResponseServletOutputStream extends ServletOutputStream {
		/**
		 * Servlet输出流
		 */
		private final ServletOutputStream os;

		public ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			content.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			content.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return this.os.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.os.setWriteListener(writeListener);
		}
	}


	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
			super(new OutputStreamWriter(content, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}
