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

package org.springframework.web.servlet.function.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 支持{@link HandlerFunction}的{@code HandlerAdapter}实现。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public class HandlerFunctionAdapter implements HandlerAdapter, Ordered {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(HandlerFunctionAdapter.class);

	/**
	 * 排序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * 异步请求超时时间，单位：毫秒
	 */
	@Nullable
	private Long asyncRequestTimeout;

	/**
	 * 指定此HandlerAdapter bean的排序值。
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}，表示非排序。
	 *
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定并发处理超时前的时间量（以毫秒为单位）。
	 * 在Servlet 3中，超时开始于主请求处理线程退出之后，并在再次为并发产生的结果进一步处理的请求再次调度时结束。
	 * <p>如果未设置此值，则使用底层实现的默认超时。
	 *
	 * @param timeout 以毫秒为单位的超时值
	 */
	public void setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
	}

	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerFunction;
	}

	@Nullable
	@Override
	public ModelAndView handle(HttpServletRequest servletRequest,
							   HttpServletResponse servletResponse,
							   Object handler) throws Exception {

		// 获取 WebAsyncManager 实例，用于处理异步请求
		WebAsyncManager asyncManager = getWebAsyncManager(servletRequest, servletResponse);

		// 获取 ServerRequest 实例，用于表示 HTTP 请求
		ServerRequest serverRequest = getServerRequest(servletRequest);
		ServerResponse serverResponse;

		if (asyncManager.hasConcurrentResult()) {
			// 如果存在并发结果，则处理异步请求
			serverResponse = handleAsync(asyncManager);
		} else {
			// 否则，将处理委托给处理函数
			HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;
			serverResponse = handlerFunction.handle(serverRequest);
		}

		if (serverResponse != null) {
			// 如果有响应，则将其写入到 Servlet 响应中，并返回处理结果
			return serverResponse.writeTo(servletRequest, servletResponse, new ServerRequestContext(serverRequest));
		} else {
			// 否则，返回 null
			return null;
		}
	}

	private WebAsyncManager getWebAsyncManager(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		// 创建 AsyncWebRequest 实例，用于异步 Web 请求处理
		AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(servletRequest, servletResponse);

		// 设置异步请求的超时时间
		asyncWebRequest.setTimeout(this.asyncRequestTimeout);

		// 获取 WebAsyncManager 实例，用于处理异步请求
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);

		// 将 AsyncWebRequest 实例设置到 WebAsyncManager 中
		asyncManager.setAsyncWebRequest(asyncWebRequest);

		// 返回 WebAsyncManager 实例，用于异步请求处理
		return asyncManager;
	}

	private ServerRequest getServerRequest(HttpServletRequest servletRequest) {
		ServerRequest serverRequest =
				(ServerRequest) servletRequest.getAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
		Assert.state(serverRequest != null, () -> "Required attribute '" +
				RouterFunctions.REQUEST_ATTRIBUTE + "' is missing");
		return serverRequest;
	}

	@Nullable
	private ServerResponse handleAsync(WebAsyncManager asyncManager) throws Exception {
		// 获取并发请求结果
		Object result = asyncManager.getConcurrentResult();
		// 清除Web异步管理器中的并发结果
		asyncManager.clearConcurrentResult();
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String formatted = LogFormatUtils.formatValue(result, !traceOn);
			return "Resume with async result [" + formatted + "]";
		});
		if (result instanceof ServerResponse) {
			// 如果并发请求的结果是 ServerResponse，则直接返回
			return (ServerResponse) result;
		} else if (result instanceof Exception) {
			// 如果并发请求的结果是异常，则返回该异常
			throw (Exception) result;
		} else if (result instanceof Throwable) {
			// 如果并发请求的结果是 Throwable，则包装成 ServletException 抛出
			throw new ServletException("Async processing failed", (Throwable) result);
		} else if (result == null) {
			// 并发请求结果为空，则返回null。
			return null;
		} else {
			// 否则抛出 IllegalArgumentException 异常
			throw new IllegalArgumentException("Unknown result from WebAsyncManager: [" + result + "]");
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1L;
	}


	private static class ServerRequestContext implements ServerResponse.Context {
		/**
		 * 服务端请求
		 */
		private final ServerRequest serverRequest;


		public ServerRequestContext(ServerRequest serverRequest) {
			this.serverRequest = serverRequest;
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.serverRequest.messageConverters();
		}
	}
}
