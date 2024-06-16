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

import org.apache.commons.logging.Log;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用Servlet Async支持和Servlet 3.1非阻塞I/O，将 {@link HttpHandler} 适配为 {@link HttpServlet}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 * @since 5.0
 */
public class ServletHttpHandlerAdapter implements Servlet {

	/**
	 * 日志记录器。
	 */
	private static final Log logger = HttpLogging.forLogName(ServletHttpHandlerAdapter.class);

	/**
	 * 默认缓冲区大小。
	 */
	private static final int DEFAULT_BUFFER_SIZE = 8192;

	/**
	 * 写错误时的属性名称。
	 */
	private static final String WRITE_ERROR_ATTRIBUTE_NAME = ServletHttpHandlerAdapter.class.getName() + ".ERROR";

	/**
	 * HTTP 处理器。
	 */
	private final HttpHandler httpHandler;

	/**
	 * 缓冲区大小，初始值为默认缓冲区大小。
	 */
	private int bufferSize = DEFAULT_BUFFER_SIZE;

	/**
	 * Servlet 路径。
	 */
	@Nullable
	private String servletPath;

	/**
	 * 数据缓冲区工厂，默认使用共享的 默认数据缓冲区工厂。
	 */
	private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;


	public ServletHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	/**
	 * 设置用于读取的输入缓冲区的大小（以字节为单位）。
	 * <p>默认情况下设置为 8192 字节。
	 *
	 * @param bufferSize 缓冲区大小（字节数）
	 */
	public void setBufferSize(int bufferSize) {
		Assert.isTrue(bufferSize > 0, "Buffer size must be larger than zero");
		this.bufferSize = bufferSize;
	}

	/**
	 * 返回配置的输入缓冲区大小。
	 *
	 * @return 缓冲区大小（字节数）
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}

	/**
	 * 通过检查来自 {@link #init(ServletConfig)} 的 Servlet 注册信息，
	 * 返回 Servlet 部署的 Servlet 路径。
	 *
	 * @return Servlet 路径；如果 Servlet 是在没有前缀的情况下部署的（即 "/" 或 "/*"），则返回空字符串；
	 * 如果在 {@link #init(ServletConfig)} Servlet 容器回调之前调用此方法，则返回 {@code null}。
	 */
	@Nullable
	public String getServletPath() {
		return this.servletPath;
	}

	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.dataBufferFactory;
	}


	// Servlet 方法...

	@Override
	public void init(ServletConfig config) {
		this.servletPath = getServletPath(config);
	}

	private String getServletPath(ServletConfig config) {
		// 获取 Servlet 的名称
		String name = config.getServletName();

		// 根据 Servlet 的名称获取 Servlet 的注册信息
		ServletRegistration registration = config.getServletContext().getServletRegistration(name);

		// 如果未找到对应的 Servlet注册
		if (registration == null) {
			// 抛出 IllegalStateException 异常，指示未找到指定 Servlet 的注册信息
			throw new IllegalStateException("ServletRegistration not found for Servlet '" + name + "'");
		}

		// 获取 Servlet 的映射路径集合
		Collection<String> mappings = registration.getMappings();

		// 如果映射路径集合的大小为1
		if (mappings.size() == 1) {
			// 获取集合中唯一的映射路径
			String mapping = mappings.iterator().next();

			// 如果映射路径为 "/"
			if (mapping.equals("/")) {
				// 返回空字符串，表示默认根路径
				return "";
			}

			// 如果映射路径以 "/*" 结尾
			if (mapping.endsWith("/*")) {
				// 获取去掉末尾 "/*" 后的映射前缀
				String path = mapping.substring(0, mapping.length() - 2);

				// 如果路径不为空且日志级别为调试模式，则记录日志
				if (!path.isEmpty() && logger.isDebugEnabled()) {
					logger.debug("Found servlet mapping prefix '" + path + "' for '" + name + "'");
				}

				// 返回映射前缀
				return path;
			}
		}

		// 如果以上条件都不满足，则抛出 IllegalArgumentException 异常，指示映射路径不符合预期
		throw new IllegalArgumentException("Expected a single Servlet mapping: " +
				"either the default Servlet mapping (i.e. '/'), " +
				"or a path based mapping (e.g. '/*', '/foo/*'). " +
				"Actual mappings: " + mappings + " for Servlet '" + name + "'");
	}


	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		// 首先检查是否存在错误属性
		if (DispatcherType.ASYNC == request.getDispatcherType()) {
			// 如果是异步调度类型，则获取请求中的异常属性
			Throwable ex = (Throwable) request.getAttribute(WRITE_ERROR_ATTRIBUTE_NAME);
			// 抛出 ServletException 异常
			throw new ServletException("Failed to create response content", ex);
		}

		// 在注册 Read/WriteListener 之前开始异步上下文
		AsyncContext asyncContext = request.startAsync();
		// 设置异步上下文的超时时间为无限期
		asyncContext.setTimeout(-1);

		// 创建 ServletServerHttpRequest 对象和相关的请求监听器、日志前缀
		ServletServerHttpRequest httpRequest;
		AsyncListener requestListener;
		String logPrefix;
		try {
			// 创建请求对象，并从中获取相关的异步监听器和日志前缀
			httpRequest = createRequest((HttpServletRequest) request, asyncContext);
			requestListener = httpRequest.getAsyncListener();
			logPrefix = httpRequest.getLogPrefix();
		} catch (URISyntaxException ex) {
			// 捕获 URISyntaxException 异常
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to get request URL: " + ex.getMessage());
			}
			// 设置响应状态为 400 Bad Request
			((HttpServletResponse) response).setStatus(400);
			// 完成异步上下文
			asyncContext.complete();
			return;
		}

		// 创建响应对象
		ServerHttpResponse httpResponse = createResponse((HttpServletResponse) response, asyncContext, httpRequest);
		// 获取响应监听器
		AsyncListener responseListener = ((ServletServerHttpResponse) httpResponse).getAsyncListener();

		// 如果请求的方法为 HEAD
		if (httpRequest.getMethod() == HttpMethod.HEAD) {
			// 使用 HttpHeadResponseDecorator 对象包装响应对象
			httpResponse = new HttpHeadResponseDecorator(httpResponse);
		}

		// 创建原子布尔标志用于完成状态追踪
		AtomicBoolean completionFlag = new AtomicBoolean();
		// 创建 HandlerResultSubscriber 订阅者对象
		HandlerResultSubscriber subscriber = new HandlerResultSubscriber(asyncContext, completionFlag, logPrefix);

		// 添加 HttpHandlerAsyncListener 异步监听器到异步上下文
		asyncContext.addListener(new HttpHandlerAsyncListener(
				requestListener, responseListener, subscriber, completionFlag, logPrefix));

		// 使用 HttpHandler 处理请求，并订阅 HandlerResultSubscriber 订阅者对象
		this.httpHandler.handle(httpRequest, httpResponse).subscribe(subscriber);
	}

	protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext context)
			throws IOException, URISyntaxException {

		Assert.notNull(this.servletPath, "Servlet path is not initialized");
		return new ServletServerHttpRequest(
				request, context, this.servletPath, getDataBufferFactory(), getBufferSize());
	}

	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
													   AsyncContext context, ServletServerHttpRequest request) throws IOException {

		return new ServletServerHttpResponse(response, context, getDataBufferFactory(), getBufferSize(), request);
	}

	@Override
	public String getServletInfo() {
		return "";
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public void destroy() {
	}


	private static void runIfAsyncNotComplete(AsyncContext asyncContext, AtomicBoolean isCompleted, Runnable task) {
		try {
			// 如果异步上下文的请求已经开始，并且成功将完成标志 isCompleted 设置为 true
			if (asyncContext.getRequest().isAsyncStarted() && isCompleted.compareAndSet(false, true)) {
				// 执行任务 task 的运行
				task.run();
			}
		} catch (IllegalStateException ex) {
			// 忽略: 回收异步上下文，不应使用
			// e.g. TIMEOUT_LISTENER (上面) 可能已经完成了异步上下文
		}
	}


	/**
	 * AsyncListener 用于在容器发出错误或超时通知时完成 {@link AsyncContext}。
	 * <p>{@link AsyncListener}s 还在 {@link ServletServerHttpRequest} 中注册，用于向请求体 Subscriber 发送 onError/onComplete 信号，
	 * 并在 {@link ServletServerHttpResponse} 中注册，用于取消写入 Publisher 并向写入结果 Subscriber 发送 onError/onComplete 信号。
	 */
	private static class HttpHandlerAsyncListener implements AsyncListener {
		/**
		 * 请求异步监听器
		 */
		private final AsyncListener requestAsyncListener;

		/**
		 * 响应异步监听器
		 */
		private final AsyncListener responseAsyncListener;

		/**
		 * 在 WildFly 12+ 之前，我们不能同时拥有 AsyncListener 和 HandlerResultSubscriber：
		 * https://issues.jboss.org/browse/WFLY-8515
		 */
		private final Runnable handlerDisposeTask;

		/**
		 * 完成标志
		 */
		private final AtomicBoolean completionFlag;

		/**
		 * 日志前缀
		 */
		private final String logPrefix;


		public HttpHandlerAsyncListener(
				AsyncListener requestAsyncListener, AsyncListener responseAsyncListener,
				Runnable handlerDisposeTask, AtomicBoolean completionFlag, String logPrefix) {

			this.requestAsyncListener = requestAsyncListener;
			this.responseAsyncListener = responseAsyncListener;
			this.handlerDisposeTask = handlerDisposeTask;
			this.completionFlag = completionFlag;
			this.logPrefix = logPrefix;
		}


		@Override
		public void onTimeout(AsyncEvent event) {
			// 理论上不应该发生的情况，因为我们调用了 asyncContext.setTimeout(-1)
			if (logger.isDebugEnabled()) {
				logger.debug(this.logPrefix + "AsyncEvent onTimeout");
			}

			// 委托处理超时事件给请求和响应的异步监听器
			delegateTimeout(this.requestAsyncListener, event);
			delegateTimeout(this.responseAsyncListener, event);

			// 处理超时或错误事件
			handleTimeoutOrError(event);
		}

		@Override
		public void onError(AsyncEvent event) {
			// 获取事件中的异常信息
			Throwable ex = event.getThrowable();

			// 如果日志级别为调试模式
			if (logger.isDebugEnabled()) {
				// 记录调试级别的日志，指示异步事件触发了错误事件，并输出异常信息（或无异常信息）
				logger.debug(this.logPrefix + "AsyncEvent onError: " + (ex != null ? ex : "<no Throwable>"));
			}

			// 委托处理错误事件给请求的异步监听器
			delegateError(this.requestAsyncListener, event);

			// 委托处理错误事件给响应的异步监听器
			delegateError(this.responseAsyncListener, event);

			// 处理超时或错误事件
			handleTimeoutOrError(event);
		}

		@Override
		public void onComplete(AsyncEvent event) {
			// 委托处理完成事件给请求的异步监听器
			delegateComplete(this.requestAsyncListener, event);

			// 委托处理完成事件给响应的异步监听器
			delegateComplete(this.responseAsyncListener, event);
		}

		private static void delegateTimeout(AsyncListener listener, AsyncEvent event) {
			try {
				// 调用监听器的 onTimeout 方法处理超时事件
				listener.onTimeout(event);
			} catch (Exception ex) {
				// 发生异常时，忽略处理
			}
		}

		private static void delegateError(AsyncListener listener, AsyncEvent event) {
			try {
				// 调用监听器的 onError 方法处理错误事件
				listener.onError(event);
			} catch (Exception ex) {
				// 忽略
			}
		}

		private static void delegateComplete(AsyncListener listener, AsyncEvent event) {
			try {
				// 调用监听器的 onComplete 方法处理完成事件
				listener.onComplete(event);
			} catch (Exception ex) {
				// 忽略
			}
		}

		private void handleTimeoutOrError(AsyncEvent event) {
			// 获取异步事件的上下文
			AsyncContext context = event.getAsyncContext();

			// 如果异步上下文尚未完成，则运行指定任务
			runIfAsyncNotComplete(context, this.completionFlag, () -> {
				try {
					// 执行处理器的销毁任务
					this.handlerDisposeTask.run();
				} finally {
					// 最终完成异步上下文
					context.complete();
				}
			});
		}

		@Override
		public void onStartAsync(AsyncEvent event) {
			// 无操作
		}
	}


	private static class HandlerResultSubscriber implements Subscriber<Void>, Runnable {
		/**
		 * 异步上下文
		 */
		private final AsyncContext asyncContext;

		/**
		 * 完成标志
		 */
		private final AtomicBoolean completionFlag;

		/**
		 * 日志前缀
		 */
		private final String logPrefix;

		/**
		 * 订阅对象
		 */
		@Nullable
		private volatile Subscription subscription;

		public HandlerResultSubscriber(
				AsyncContext asyncContext, AtomicBoolean completionFlag, String logPrefix) {

			this.asyncContext = asyncContext;
			this.completionFlag = completionFlag;
			this.logPrefix = logPrefix;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			// 设置订阅对象
			this.subscription = subscription;
			// 将订阅对象的请求数量设置为最大数量
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// 无操作
		}

		@Override
		public void onError(Throwable ex) {
			// 如果日志级别为跟踪模式
			if (logger.isTraceEnabled()) {
				// 记录跟踪级别的日志，指示发生错误
				logger.trace(this.logPrefix + "onError: " + ex);
			}

			// 调用自定义方法，如果异步操作尚未完成，则执行指定的任务
			runIfAsyncNotComplete(this.asyncContext, this.completionFlag, () -> {
				// 如果响应已经提交到客户端
				if (this.asyncContext.getResponse().isCommitted()) {
					logger.trace(this.logPrefix + "Dispatch to container, to raise the error on servlet thread");
					// 将错误信息设置为请求的属性，并通过容器线程引发错误
					this.asyncContext.getRequest().setAttribute(WRITE_ERROR_ATTRIBUTE_NAME, ex);
					// 分发请求
					this.asyncContext.dispatch();
				} else {
					try {
						// 记录日志
						logger.trace(this.logPrefix + "Setting ServletResponse status to 500 Server Error");
						// 重置缓冲区
						this.asyncContext.getResponse().resetBuffer();
						// 设置响应状态码为 500
						((HttpServletResponse) this.asyncContext.getResponse()).setStatus(500);
					} finally {
						// 最终完成异步操作
						this.asyncContext.complete();
					}
				}
			});
		}

		@Override
		public void onComplete() {
			// 如果日志级别为跟踪模式
			if (logger.isTraceEnabled()) {
				// 记录跟踪级别的日志，指示异步操作完成
				logger.trace(this.logPrefix + "onComplete");
			}

			// 调用自定义方法，如果异步操作尚未完成，则完成异步上下文
			runIfAsyncNotComplete(this.asyncContext, this.completionFlag, this.asyncContext::complete);
		}

		@Override
		public void run() {
			// 获取当前的订阅对象
			Subscription s = this.subscription;

			// 如果订阅对象不为空
			if (s != null) {
				// 取消订阅
				s.cancel();
			}
		}
	}

}
