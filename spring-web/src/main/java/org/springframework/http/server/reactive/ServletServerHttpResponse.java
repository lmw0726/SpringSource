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

package org.springframework.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 使 {@link ServerHttpResponse} 适配Servlet {@link HttpServletResponse}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ServletServerHttpResponse extends AbstractListenerServerHttpResponse {

	/**
	 * 封装了基础的 HTTP 响应相关内容，包括 Servlet 响应对象和输出流等。
	 */
	private final HttpServletResponse response;

	/**
	 * Servlet 输出流，用于写入响应内容。
	 */
	private final ServletOutputStream outputStream;

	/**
	 * 缓冲区大小。
	 */
	private final int bufferSize;

	/**
	 * 响应体刷新处理器。
	 */
	@Nullable
	private volatile ResponseBodyFlushProcessor bodyFlushProcessor;

	/**
	 * 响应体处理器。
	 */
	@Nullable
	private volatile ResponseBodyProcessor bodyProcessor;

	/**
	 * 下一个是否需要刷新。
	 */
	private volatile boolean flushOnNext;

	/**
	 * 包含了 Servlet 请求对象的服务器请求对象。
	 */
	private final ServletServerHttpRequest request;

	/**
	 * 异步监听器。
	 */
	private final ResponseAsyncListener asyncListener;


	public ServletServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
									 DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

		this(new HttpHeaders(), response, asyncContext, bufferFactory, bufferSize, request);
	}

	public ServletServerHttpResponse(HttpHeaders headers, HttpServletResponse response, AsyncContext asyncContext,
									 DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

		super(bufferFactory, headers);

		Assert.notNull(response, "HttpServletResponse must not be null");
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "Buffer size must be greater than 0");

		this.response = response;
		this.outputStream = response.getOutputStream();
		this.bufferSize = bufferSize;
		this.request = request;

		this.asyncListener = new ResponseAsyncListener();

		// Tomcat 期望在初始线程上注册 WriteListener
		response.getOutputStream().setWriteListener(new ResponseBodyWriteListener());
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	@Override
	public HttpStatus getStatusCode() {
		// 获取父类的 HttpStatus 状态码
		HttpStatus status = super.getStatusCode();

		// 如果父类的 HttpStatus 不为 null，则返回该状态码；
		// 否则，解析响应的状态码并返回
		return (status != null ? status : HttpStatus.resolve(this.response.getStatus()));
	}

	@Override
	public Integer getRawStatusCode() {
		// 获取父类的原始状态码
		Integer status = super.getRawStatusCode();

		// 如果父类的原始状态码不为 null，则返回该状态码；
		// 否则，返回响应的状态码
		return (status != null ? status : this.response.getStatus());
	}

	@Override
	protected void applyStatusCode() {
		// 获取父类的原始状态码
		Integer status = super.getRawStatusCode();

		// 如果父类的原始状态码不为 null
		if (status != null) {
			// 将响应的状态码设置为父类的原始状态码
			this.response.setStatus(status);
		}
	}

	@Override
	protected void applyHeaders() {
		// 遍历响应头的每个键值对
		getHeaders().forEach((headerName, headerValues) -> {
			// 遍历每个键对应的值
			for (String headerValue : headerValues) {
				// 将每个键值对添加到响应的头信息中
				this.response.addHeader(headerName, headerValue);
			}
		});

		// 尝试获取响应头的内容类型
		MediaType contentType = null;
		try {
			contentType = getHeaders().getContentType();
		} catch (Exception ex) {
			// 如果出现异常，尝试直接获取响应头的原始内容类型并设置到响应中
			String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
			this.response.setContentType(rawContentType);
		}

		// 如果响应中的内容类型为 null 且从头信息中解析出的内容类型不为 null，则设置响应中的内容类型
		if (this.response.getContentType() == null && contentType != null) {
			this.response.setContentType(contentType.toString());
		}

		// 获取内容类型的字符集
		Charset charset = (contentType != null ? contentType.getCharset() : null);

		// 如果响应中的字符编码为 null 且字符集不为 null，则设置响应中的字符编码
		if (this.response.getCharacterEncoding() == null && charset != null) {
			this.response.setCharacterEncoding(charset.name());
		}

		// 获取响应头的内容长度
		long contentLength = getHeaders().getContentLength();

		// 如果内容长度不为 -1，则设置响应的内容长度
		if (contentLength != -1) {
			this.response.setContentLengthLong(contentLength);
		}
	}

	@Override
	protected void applyCookies() {

		// Servlet Cookie 不支持 SameSite 属性：
		// https://github.com/eclipse-ee4j/servlet-api/issues/175
		// 对于 Jetty，在 9.4.21+ 版本中我们可以适应 HttpCookie：
		// https://github.com/eclipse/jetty.project/issues/3040
		// 对于 Tomcat，似乎只有全局选项：
		// https://tomcat.apache.org/tomcat-8.5-doc/config/cookie-processor.html

		// 遍历所有 Cookie 键对应的值（每个值都是一个 Cookie 列表）
		for (List<ResponseCookie> cookies : getCookies().values()) {
			// 遍历每个 Cookie 列表中的每个 ResponseCookie 对象
			for (ResponseCookie cookie : cookies) {
				// 将每个 ResponseCookie 对象转换为字符串并添加到响应头的 Set-Cookie 中
				this.response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
	}

	/**
	 * 返回一个 {@link ResponseAsyncListener}，用于通知 Servlet 容器事件的响应体发布者和订阅者。
	 * 该监听器实际上没有注册，而是暴露给 {@link ServletHttpHandlerAdapter}，以确保事件被委托。
	 */
	AsyncListener getAsyncListener() {
		return this.asyncListener;
	}

	@Override
	protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
		ResponseBodyFlushProcessor processor = new ResponseBodyFlushProcessor();
		this.bodyFlushProcessor = processor;
		return processor;
	}

	/**
	 * 将 DataBuffer 写入响应体的 OutputStream。
	 * 仅在 {@link ServletOutputStream#isReady()} 返回 "true" 且 DataBuffer 中的可读字节大于 0 时调用。
	 *
	 * @param dataBuffer 要写入的数据缓冲区
	 * @return 写入的字节数
	 * @throws IOException 如果在写入期间发生 I/O 错误
	 */
	protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
		// 获取 ServletOutputStream 对象
		ServletOutputStream outputStream = this.outputStream;

		// 将 DataBuffer 转换为 InputStream
		InputStream input = dataBuffer.asInputStream();

		// 初始化已写入的字节数
		int bytesWritten = 0;

		// 创建缓冲区
		byte[] buffer = new byte[this.bufferSize];

		// 读取数据并写入 ServletOutputStream
		int bytesRead;
		while (outputStream.isReady() && (bytesRead = input.read(buffer)) != -1) {
			// 写入数据到 ServletOutputStream
			outputStream.write(buffer, 0, bytesRead);
			// 更新已写入的字节数
			bytesWritten += bytesRead;
		}

		// 返回已写入的字节数
		return bytesWritten;
	}

	private void flush() throws IOException {
		// 获取 ServletOutputStream 对象
		ServletOutputStream outputStream = this.outputStream;

		// 如果 ServletOutputStream 准备好写入
		if (outputStream.isReady()) {
			try {
				// 刷新 ServletOutputStream
				outputStream.flush();
				// 设置下一次刷新标志为 false
				this.flushOnNext = false;
			} catch (IOException ex) {
				// 如果刷新失败，则设置下一次刷新标志为 true，并抛出异常
				this.flushOnNext = true;
				throw ex;
			}
		} else {
			// 如果 ServletOutputStream 没有准备好写入，则设置下一次刷新标志为 true
			this.flushOnNext = true;
		}
	}

	private boolean isWritePossible() {
		return this.outputStream.isReady();
	}


	private final class ResponseAsyncListener implements AsyncListener {

		@Override
		public void onStartAsync(AsyncEvent event) {
		}

		@Override
		public void onTimeout(AsyncEvent event) {
			// 获取事件中的异常信息
			Throwable ex = event.getThrowable();

			// 如果异常信息不为 null，则使用该异常信息；
			// 否则，创建一个 IllegalStateException 异常
			ex = (ex != null ? ex : new IllegalStateException("Async operation timeout."));

			// 处理异常
			handleError(ex);
		}

		@Override
		public void onError(AsyncEvent event) {
			handleError(event.getThrowable());
		}

		public void handleError(Throwable ex) {
			// 获取响应体刷新处理器和响应体处理器
			ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
			ResponseBodyProcessor processor = bodyProcessor;

			// 如果刷新处理器不为 null
			if (flushProcessor != null) {
				// 取消“写入”发布者的上游源
				flushProcessor.cancel();
				// 取消当前的“写入”发布者并向下游传播 onComplete
				if (processor != null) {
					processor.cancel();
					processor.onError(ex);
				}
				// 如果处理器已连接并且 onError 在全部传播到底层时是无操作的
				flushProcessor.onError(ex);
			}
		}

		@Override
		public void onComplete(AsyncEvent event) {
			// 获取响应体刷新处理器和响应体处理器
			ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
			ResponseBodyProcessor processor = bodyProcessor;

			// 如果刷新处理器不为 null
			if (flushProcessor != null) {
				// 取消“写入”发布者的上游源
				flushProcessor.cancel();
				// 取消当前的“写入”发布者并向下游传播 onComplete
				if (processor != null) {
					processor.cancel();
					processor.onComplete();
				}
				// 如果处理器已连接并且 onComplete 在全部传播到底层时是无操作的
				flushProcessor.onComplete();
			}
		}
	}


	private class ResponseBodyWriteListener implements WriteListener {

		@Override
		public void onWritePossible() {
			// 获取响应体处理器
			ResponseBodyProcessor processor = bodyProcessor;

			// 如果响应体处理器不为 null，则调用 onWritePossible 方法
			if (processor != null) {
				processor.onWritePossible();
			} else {
				// 否则，获取响应体刷新处理器
				ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
				// 如果响应体刷新处理器不为 null，则调用 onFlushPossible 方法
				if (flushProcessor != null) {
					flushProcessor.onFlushPossible();
				}
			}
		}

		@Override
		public void onError(Throwable ex) {
			ServletServerHttpResponse.this.asyncListener.handleError(ex);
		}
	}


	private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		public ResponseBodyFlushProcessor() {
			super(request.getLogPrefix());
		}

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			ResponseBodyProcessor processor = new ResponseBodyProcessor();
			bodyProcessor = processor;
			return processor;
		}

		@Override
		protected void flush() throws IOException {
			// 如果跟踪日志级别是 trace
			if (rsWriteFlushLogger.isTraceEnabled()) {
				// 输出日志前缀并记录“flushing”
				rsWriteFlushLogger.trace(getLogPrefix() + "flushing");
			}

			// 刷新 ServletServerHttpResponse
			ServletServerHttpResponse.this.flush();
		}

		@Override
		protected boolean isWritePossible() {
			return ServletServerHttpResponse.this.isWritePossible();
		}

		@Override
		protected boolean isFlushPending() {
			return flushOnNext;
		}
	}


	private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {


		public ResponseBodyProcessor() {
			super(request.getLogPrefix());
		}

		@Override
		protected boolean isWritePossible() {
			return ServletServerHttpResponse.this.isWritePossible();
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return dataBuffer.readableByteCount() == 0;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			// 如果在下一个操作上调用了 flush() 方法
			if (ServletServerHttpResponse.this.flushOnNext) {
				// 如果跟踪日志级别是 trace
				if (rsWriteLogger.isTraceEnabled()) {
					// 输出日志前缀并记录“flushing”
					rsWriteLogger.trace(getLogPrefix() + "flushing");
				}
				// 刷新 ServletServerHttpResponse
				flush();
			}

			// 检查是否可以进行写入操作
			boolean ready = ServletServerHttpResponse.this.isWritePossible();
			// 获取数据缓冲区中剩余可读字节数
			int remaining = dataBuffer.readableByteCount();

			// 如果可以进行写入且剩余可读字节数大于 0
			if (ready && remaining > 0) {
				// 在发生 IOException 的情况下，onError 处理应该调用 discardData(DataBuffer) 方法
				int written = writeToOutputStream(dataBuffer);
				// 如果跟踪日志级别是 trace
				if (rsWriteLogger.isTraceEnabled()) {
					// 输出日志前缀并记录写入的字节数和剩余的字节数
					rsWriteLogger.trace(getLogPrefix() + "Wrote " + written + " of " + remaining + " bytes");
				}
				// 如果成功写入了所有剩余字节
				if (written == remaining) {
					// 释放数据缓冲区并返回 true
					DataBufferUtils.release(dataBuffer);
					return true;
				}
			} else {
				// 如果跟踪日志级别是 trace
				if (rsWriteLogger.isTraceEnabled()) {
					// 输出日志前缀并记录是否可写和剩余字节数
					rsWriteLogger.trace(getLogPrefix() + "ready: " + ready + ", remaining: " + remaining);
				}
			}

			// 返回 false 表示未能写入所有剩余字节
			return false;
		}

		@Override
		protected void writingComplete() {
			bodyProcessor = null;
		}

		@Override
		protected void discardData(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}
	}

}
