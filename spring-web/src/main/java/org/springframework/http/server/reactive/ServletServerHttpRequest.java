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

package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import reactor.core.publisher.Flux;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * 将 {@link ServerHttpRequest} 适配到 Servlet 的 {@link HttpServletRequest}。
 *
 * 该类实现了将 {@link ServerHttpRequest} 适配到 Servlet 的功能，通过继承 {@link AbstractServerHttpRequest} 类来实现具体的适配逻辑。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ServletServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * 表示一个HTTP请求的封装。
	 */
	static final DataBuffer EOF_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

	/**
	 * HTTP Servlet 请求对象。
	 */
	private final HttpServletRequest request;

	/**
	 * 请求体发布器。
	 */
	private final RequestBodyPublisher bodyPublisher;

	/**
	 * Cookie 锁对象。
	 */
	private final Object cookieLock = new Object();

	/**
	 * 数据缓冲区工厂。
	 */
	private final DataBufferFactory bufferFactory;

	/**
	 * 字节数组缓冲区。
	 */
	private final byte[] buffer;

	/**
	 * 异步监听器。
	 */
	private final AsyncListener asyncListener;


	public ServletServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
									String servletPath, DataBufferFactory bufferFactory, int bufferSize)
			throws IOException, URISyntaxException {

		this(createDefaultHttpHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
	}

	public ServletServerHttpRequest(MultiValueMap<String, String> headers, HttpServletRequest request,
									AsyncContext asyncContext, String servletPath, DataBufferFactory bufferFactory, int bufferSize)
			throws IOException, URISyntaxException {

		super(initUri(request), request.getContextPath() + servletPath, initHeaders(headers, request));

		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be higher than 0");

		// 将传入的请求对象赋值给实例变量
		this.request = request;

		// 将传入的数据缓冲区工厂赋值给实例变量
		this.bufferFactory = bufferFactory;

		// 初始化数据缓冲区，使用指定大小的字节数组
		this.buffer = new byte[bufferSize];

		// 创建一个RequestAsyncListener实例，用于处理异步请求事件
		this.asyncListener = new RequestAsyncListener();

		// Tomcat期望在初始线程上注册ReadListener
		// 获取ServletInputStream对象，用于从请求中读取数据
		ServletInputStream inputStream = request.getInputStream();

		// 创建RequestBodyPublisher实例，用于管理请求体的发布
		this.bodyPublisher = new RequestBodyPublisher(inputStream);

		// 注册ReadListener，使RequestBodyPublisher可以接收数据读取事件
		this.bodyPublisher.registerReadListener();
	}


	private static MultiValueMap<String, String> createDefaultHttpHeaders(HttpServletRequest request) {
		// 创建一个空的MultiValueMap对象，用于存储头部信息，不区分大小写，并且初始化大小为8，使用英语环境（Locale.ENGLISH）
		MultiValueMap<String, String> headers =
				CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));

		// 遍历所有的头部名称
		for (Enumeration<?> names = request.getHeaderNames(); names.hasMoreElements(); ) {
			// 获取头部名称
			String name = (String) names.nextElement();
			// 遍历该头部名称对应的所有值
			for (Enumeration<?> values = request.getHeaders(name); values.hasMoreElements(); ) {
				// 获取头部值，并将其添加到MultiValueMap中
				headers.add(name, (String) values.nextElement());
			}
		}

		// 返回包含所有头部信息的MultiValueMap对象
		return headers;
	}

	private static URI initUri(HttpServletRequest request) throws URISyntaxException {
		Assert.notNull(request, "'request' must not be null");
		StringBuffer url = request.getRequestURL();
		String query = request.getQueryString();
		if (StringUtils.hasText(query)) {
			url.append('?').append(query);
		}
		return new URI(url.toString());
	}

	private static MultiValueMap<String, String> initHeaders(
			MultiValueMap<String, String> headerValues, HttpServletRequest request) {

		// 初始化变量，用于存储HTTP头部和内容类型
		HttpHeaders headers = null;
		MediaType contentType = null;

		// 如果请求中的Content-Type头部不是一个非空的字符串
		if (!StringUtils.hasLength(headerValues.getFirst(HttpHeaders.CONTENT_TYPE))) {
			// 获取请求的Content-Type
			String requestContentType = request.getContentType();
			if (StringUtils.hasLength(requestContentType)) {
				// 解析请求的Content-Type为MediaType对象
				contentType = MediaType.parseMediaType(requestContentType);
				// 使用headerValues初始化HttpHeaders对象
				headers = new HttpHeaders(headerValues);
				// 设置HttpHeaders的Content-Type
				headers.setContentType(contentType);
			}
		}

		// 如果contentType不为null，且它的字符集为null
		if (contentType != null && contentType.getCharset() == null) {
			// 获取请求的字符编码
			String encoding = request.getCharacterEncoding();
			if (StringUtils.hasLength(encoding)) {
				// 创建一个包含contentType参数的新映射
				Map<String, String> params = new LinkedCaseInsensitiveMap<>();
				params.putAll(contentType.getParameters());
				// 将字符编码添加到params映射中
				params.put("charset", Charset.forName(encoding).toString());
				// 更新HttpHeaders的Content-Type，包含字符编码信息
				headers.setContentType(new MediaType(contentType, params));
			}
		}

		// 如果headerValues中的Content-Type头部为null
		if (headerValues.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
			// 获取请求的内容长度
			int contentLength = request.getContentLength();
			if (contentLength != -1) {
				// 如果headers不为null，则使用headers，否则使用headerValues创建一个新的HttpHeaders对象
				headers = (headers != null ? headers : new HttpHeaders(headerValues));
				// 设置HttpHeaders的Content-Length
				headers.setContentLength(contentLength);
			}
		}

		// 返回设置好的HttpHeaders对象，如果headers为null，则返回原始的headerValues
		return (headers != null ? headers : headerValues);
	}


	@Override
	public String getMethodValue() {
		return this.request.getMethod();
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		// 创建一个多值映射，用于存储转换后的HttpCookie对象
		MultiValueMap<String, HttpCookie> httpCookies = new LinkedMultiValueMap<>();

		Cookie[] cookies;
		// 使用cookieLock对象进行同步，获取请求中的Cookie数组
		synchronized (this.cookieLock) {
			cookies = this.request.getCookies();
		}

		// 如果获取到的Cookie数组不为null
		if (cookies != null) {
			// 遍历每个Cookie对象
			for (Cookie cookie : cookies) {
				// 获取Cookie的名称
				String name = cookie.getName();
				// 获取Cookie的值
				String value = cookie.getValue();
				// 创建一个HttpCookie对象，将Cookie对象转换为HttpCookie对象
				HttpCookie httpCookie = new HttpCookie(name, value);
				// 将HttpCookie对象添加到MultiValueMap中，使用Cookie的名称作为键
				httpCookies.add(name, httpCookie);
			}
		}

		// 返回转换后的HttpCookie对象的MultiValueMap
		return httpCookies;
	}

	@Override
	@NonNull
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(this.request.getLocalAddr(), this.request.getLocalPort());
	}

	@Override
	@NonNull
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(this.request.getRemoteHost(), this.request.getRemotePort());
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		// 获取X.509证书数组
		X509Certificate[] certificates = getX509Certificates();

		// 如果证书数组不为null，则创建一个新的DefaultSslInfo对象，包含SSL会话ID和证书数组
		return (certificates != null ? new DefaultSslInfo(getSslSessionId(), certificates) : null);
	}

	@Nullable
	private String getSslSessionId() {
		return (String) this.request.getAttribute("javax.servlet.request.ssl_session_id");
	}

	@Nullable
	private X509Certificate[] getX509Certificates() {
		return (X509Certificate[]) this.request.getAttribute("javax.servlet.request.X509Certificate");
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.bodyPublisher);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	/**
	 * 返回一个 {@link RequestAsyncListener}，用于在 Servlet 容器通知请求输入结束时完成请求体的发布。
	 * 实际上并未注册该监听器，而是在 {@link ServletHttpHandlerAdapter} 中暴露出来以确保事件被委托。
	 */
	AsyncListener getAsyncListener() {
		return this.asyncListener;
	}

	/**
	 * 从请求体的 InputStream 中读取并返回一个 DataBuffer。
	 * 仅在 {@link ServletInputStream#isReady()} 返回 "true" 时调用。
	 *
	 * @return 读取到的包含数据的 DataBuffer；
	 * 如果读取了 0 字节，则返回 {@link AbstractListenerReadPublisher#EMPTY_BUFFER}；
	 * 如果输入流返回 -1，则返回 {@link #EOF_BUFFER}。
	 * @throws IOException 如果发生 I/O 错误
	 */
	DataBuffer readFromInputStream() throws IOException {
		// 从请求的输入流中读取数据到缓冲区，并记录读取的字节数
		int read = this.request.getInputStream().read(this.buffer);
		logBytesRead(read);

		// 如果成功读取了数据（read > 0）
		if (read > 0) {
			// 分配一个大小为read的数据缓冲区
			DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(read);
			// 将从输入流读取的数据写入到数据缓冲区中
			dataBuffer.write(this.buffer, 0, read);
			// 返回数据缓冲区
			return dataBuffer;
		}

		// 如果读取到流的末尾（read == -1）
		if (read == -1) {
			// 返回EOF_BUFFER，表示已经到达流的末尾
			return EOF_BUFFER;
		}

		// 如果未成功读取任何数据，返回空的数据缓冲区（EMPTY_BUFFER）
		return AbstractListenerReadPublisher.EMPTY_BUFFER;
	}

	protected final void logBytesRead(int read) {
		// 获取日志记录器实例
		Log rsReadLogger = AbstractListenerReadPublisher.rsReadLogger;

		// 如果跟踪日志级别是启用的
		if (rsReadLogger.isTraceEnabled()) {
			// 记录跟踪级别日志，包括日志前缀和读取字节数信息
			rsReadLogger.trace(getLogPrefix() + "Read " + read + (read != -1 ? " bytes" : ""));
		}
	}


	private final class RequestAsyncListener implements AsyncListener {

		@Override
		public void onStartAsync(AsyncEvent event) {
		}

		@Override
		public void onTimeout(AsyncEvent event) {
			// 从事件中获取可能的异常
			Throwable ex = event.getThrowable();

			// 如果异常不为null，则使用获取到的异常；
			// 否则，抛出一个新的IllegalStateException异常，指示异步操作超时
			ex = ex != null ? ex : new IllegalStateException("Async operation timeout.");

			// 调用bodyPublisher的onError方法，传递异常对象ex，通知订阅者异步操作发生了错误
			bodyPublisher.onError(ex);
		}

		@Override
		public void onError(AsyncEvent event) {
			bodyPublisher.onError(event.getThrowable());
		}

		@Override
		public void onComplete(AsyncEvent event) {
			bodyPublisher.onAllDataRead();
		}
	}


	private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {
		/**
		 * Servlet输入流
		 */
		private final ServletInputStream inputStream;

		public RequestBodyPublisher(ServletInputStream inputStream) {
			super(ServletServerHttpRequest.this.getLogPrefix());
			this.inputStream = inputStream;
		}

		public void registerReadListener() throws IOException {
			this.inputStream.setReadListener(new RequestBodyPublisherReadListener());
		}

		@Override
		protected void checkOnDataAvailable() {
			// 如果输入流已经准备就绪并且尚未完成
			if (this.inputStream.isReady() && !this.inputStream.isFinished()) {
				// 调用处理数据可用的方法
				onDataAvailable();
			}
		}

		@Override
		@Nullable
		protected DataBuffer read() throws IOException {
			// 如果输入流准备就绪
			if (this.inputStream.isReady()) {
				// 从输入流中读取数据到DataBuffer中
				DataBuffer dataBuffer = readFromInputStream();

				// 如果读取到的数据缓冲区是EOF_BUFFER（表示已经到达流的末尾）
				if (dataBuffer == EOF_BUFFER) {
					// 不需要等待容器的回调，直接触发所有数据已读取的处理逻辑
					onAllDataRead();
					// 将数据缓冲区设为null
					dataBuffer = null;
				}

				// 返回读取到的数据缓冲区（可能为null）
				return dataBuffer;
			}

			// 如果输入流未准备就绪，返回null
			return null;
		}

		@Override
		protected void readingPaused() {
			// 无操作
		}

		@Override
		protected void discardData() {
			// 没有什么可丢弃的，因为我们立即传递数据缓冲区。
		}


		private class RequestBodyPublisherReadListener implements ReadListener {

			@Override
			public void onDataAvailable() throws IOException {
				RequestBodyPublisher.this.onDataAvailable();
			}

			@Override
			public void onAllDataRead() throws IOException {
				RequestBodyPublisher.this.onAllDataRead();
			}

			@Override
			public void onError(Throwable throwable) {
				RequestBodyPublisher.this.onError(throwable);

			}
		}
	}

}
