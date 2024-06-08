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

package org.springframework.web.context.request.async;

import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * {@link AsyncWebRequest} 的 Servlet 3.0 实现。
 *
 * <p>参与异步请求的 servlet 和所有过滤器必须使用 Servlet API 启用异步支持，或者通过在 {@code web.xml} 中的 servlet 和过滤器声明中添加 <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code> 元素来实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	/**
	 * 表示异步操作的超时时间。
	 */
	private Long timeout;

	/**
	 * 异步上下文对象，用于异步请求处理。
	 */
	private AsyncContext asyncContext;

	/**
	 * 用于跟踪异步操作是否已经完成的原子布尔值。
	 */
	private AtomicBoolean asyncCompleted = new AtomicBoolean();

	/**
	 * 用于存储超时处理程序的列表。
	 */
	private final List<Runnable> timeoutHandlers = new ArrayList<>();

	/**
	 * 用于存储异常处理程序的列表。
	 */
	private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

	/**
	 * 用于存储完成处理程序的列表。
	 */
	private final List<Runnable> completionHandlers = new ArrayList<>();


	/**
	 * 为给定的请求/响应对创建一个新实例。
	 *
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 */
	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}


	/**
	 * 在 Servlet 3 异步处理中，超时期限在容器处理线程退出后开始。
	 */
	@Override
	public void setTimeout(Long timeout) {
		Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
		this.timeout = timeout;
	}

	@Override
	public void addTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandlers.add(timeoutHandler);
	}

	@Override
	public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
		this.exceptionHandlers.add(exceptionHandler);
	}

	@Override
	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	@Override
	public boolean isAsyncStarted() {
		return (this.asyncContext != null && getRequest().isAsyncStarted());
	}

	/**
	 * 异步请求处理是否已完成。
	 * <p>在异步处理完成后，重要的是避免在之后使用请求和响应对象。Servlet 容器通常会重新使用它们。
	 */
	@Override
	public boolean isAsyncComplete() {
		return this.asyncCompleted.get();
	}

	@Override
	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
						"in async request processing. This is done in Java code using the Servlet API " +
						"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
						"filter declarations in web.xml.");
		Assert.state(!isAsyncComplete(), "Async processing has already completed");

		// 如果异步已启动，则直接返回
		if (isAsyncStarted()) {
			return;
		}
		// 启动异步上下文
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		// 添加监听器
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			// 如果设置了超时时间，则设置异步上下文的超时时间
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	@Override
	public void dispatch() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		this.asyncContext.dispatch();
	}


	// ---------------------------------------------------------------------
	// AsyncListener 方法的实现
	// ---------------------------------------------------------------------

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		this.exceptionHandlers.forEach(consumer -> consumer.accept(event.getThrowable()));
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		this.timeoutHandlers.forEach(Runnable::run);
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		// 对于每个完成处理器，执行run方法
		this.completionHandlers.forEach(Runnable::run);
		// 将异步上下文置空
		this.asyncContext = null;
		// 将异步完成标志设置为true
		this.asyncCompleted.set(true);
	}

}
