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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于Servlet容器（Servlet 3.0+）的 {@link ServerHttpAsyncRequestControl} 实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletServerHttpAsyncRequestControl implements ServerHttpAsyncRequestControl, AsyncListener {
	/**
	 * 没有超时的值
	 */
	private static final long NO_TIMEOUT_VALUE = Long.MIN_VALUE;

	/**
	 * Servlet服务器Http请求
	 */
	private final ServletServerHttpRequest request;

	/**
	 * Servlet服务器Http响应
	 */
	private final ServletServerHttpResponse response;

	/**
	 * 异步上下文
	 */
	@Nullable
	private AsyncContext asyncContext;

	/**
	 * 异步请求是否完成
	 */
	private AtomicBoolean asyncCompleted = new AtomicBoolean();


	/**
	 * 构造函数接受一个期望为 {@link ServletServerHttpRequest} 和
	 * {@link ServletServerHttpResponse} 类型的请求和响应对。
	 */
	public ServletServerHttpAsyncRequestControl(ServletServerHttpRequest request, ServletServerHttpResponse response) {
		Assert.notNull(request, "request is required");
		Assert.notNull(response, "response is required");

		Assert.isTrue(request.getServletRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
						"in async request processing. This is done in Java code using the Servlet API " +
						"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
						"filter declarations in web.xml. Also you must use a Servlet 3.0+ container");

		this.request = request;
		this.response = response;
	}


	@Override
	public boolean isStarted() {
		return (this.asyncContext != null && this.request.getServletRequest().isAsyncStarted());
	}

	@Override
	public boolean isCompleted() {
		return this.asyncCompleted.get();
	}

	@Override
	public void start() {
		start(NO_TIMEOUT_VALUE);
	}

	@Override
	public void start(long timeout) {
		Assert.state(!isCompleted(), "Async processing has already completed");
		// 如果异步上下文已经启动，则直接返回，避免重复启动
		if (isStarted()) {
			return;
		}

		// 从请求处理器中获取原始的 Servlet 请求和响应对象
		HttpServletRequest servletRequest = this.request.getServletRequest();
		HttpServletResponse servletResponse = this.response.getServletResponse();

		// 使用原始 Servlet 请求和响应对象启动异步上下文
		this.asyncContext = servletRequest.startAsync(servletRequest, servletResponse);

		// 向异步上下文添加监听器，通常是当前对象本身
		this.asyncContext.addListener(this);

		// 如果超时时间不是非超时时间值
		if (timeout != NO_TIMEOUT_VALUE) {
			// 设置异步上下文的超时时间
			this.asyncContext.setTimeout(timeout);
		}
	}

	@Override
	public void complete() {
		// 如果异步上下文不为 null，并且已经启动，并且尚未完成
		if (this.asyncContext != null && isStarted() && !isCompleted()) {
			// 完成异步上下文，标记异步处理完成
			this.asyncContext.complete();
		}
	}


	// ---------------------------------------------------------------------
	// AsyncListener方法的实现
	// ---------------------------------------------------------------------

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		this.asyncContext = null;
		this.asyncCompleted.set(true);
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
	}

}
