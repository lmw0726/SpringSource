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

package org.springframework.web.socket.server.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于处理 WebSocket 握手请求的 {@link HttpRequestHandler}。
 *
 * <p>这是在特定 URL 配置服务器 WebSocket 时使用的主要类。
 * 它是一个非常薄的包装器，包装了一个 {@link WebSocketHandler} 和一个 {@link HandshakeHandler}，
 * 同时将 {@link HttpServletRequest} 和 {@link HttpServletResponse} 适配为
 * {@link ServerHttpRequest} 和 {@link ServerHttpResponse}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHttpRequestHandler implements HttpRequestHandler, Lifecycle, ServletContextAware {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(WebSocketHttpRequestHandler.class);
	/**
	 * WebSocket处理器
	 */
	private final WebSocketHandler wsHandler;

	/**
	 * 握手处理器
	 */
	private final HandshakeHandler handshakeHandler;

	/**
	 * 握手拦截器列表
	 */
	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	/**
	 * WebSocket是否启动
	 */
	private volatile boolean running;


	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler) {
		this(wsHandler, new DefaultHandshakeHandler());
	}

	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler, HandshakeHandler handshakeHandler) {
		Assert.notNull(wsHandler, "wsHandler must not be null");
		Assert.notNull(handshakeHandler, "handshakeHandler must not be null");
		this.wsHandler = decorate(wsHandler);
		this.handshakeHandler = handshakeHandler;
	}

	/**
	 * 装饰传入构造函数的 {@code WebSocketHandler}。
	 * <p>默认情况下，添加了 {@link LoggingWebSocketHandlerDecorator} 和
	 * {@link ExceptionWebSocketHandlerDecorator}。
	 *
	 * @since 5.2.2
	 */
	protected WebSocketHandler decorate(WebSocketHandler handler) {
		return new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(handler));
	}


	/**
	 * 返回 WebSocketHandler。
	 */
	public WebSocketHandler getWebSocketHandler() {
		return this.wsHandler;
	}

	/**
	 * 返回 HandshakeHandler。
	 */
	public HandshakeHandler getHandshakeHandler() {
		return this.handshakeHandler;
	}

	/**
	 * 配置一个或多个 WebSocket 握手请求拦截器。
	 */
	public void setHandshakeInterceptors(@Nullable List<HandshakeInterceptor> interceptors) {
		// 清空现有的拦截器
		this.interceptors.clear();
		if (interceptors != null) {
			// 添加新的拦截器
			this.interceptors.addAll(interceptors);
		}
	}

	/**
	 * 返回配置的 WebSocket 握手请求拦截器。
	 */
	public List<HandshakeInterceptor> getHandshakeInterceptors() {
		return this.interceptors;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.handshakeHandler instanceof ServletContextAware) {
			// 如果握手处理器是 ServletContextAware 类型，设置Servlet上下文
			((ServletContextAware) this.handshakeHandler).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		// 如果WebSocket服务尚未运行
		if (!isRunning()) {
			// 设置运行状态为true
			this.running = true;
			// 如果握手处理程序实现了Lifecycle接口
			if (this.handshakeHandler instanceof Lifecycle) {
				// 启动握手处理程序
				((Lifecycle) this.handshakeHandler).start();
			}
		}
	}

	@Override
	public void stop() {
		// 如果WebSocket服务正在运行
		if (isRunning()) {
			// 设置运行状态为false
			this.running = false;
			// 如果握手处理程序实现了Lifecycle接口
			if (this.handshakeHandler instanceof Lifecycle) {
				// 停止握手处理程序
				((Lifecycle) this.handshakeHandler).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		// 创建ServletServerHttpRequest对象
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
// 创建ServletServerHttpResponse对象
		ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

// 创建握手拦截器链
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
// 握手失败异常
		HandshakeFailureException failure = null;

		try {
			// 如果日志级别为DEBUG
			if (logger.isDebugEnabled()) {
				// 记录请求方法和请求URI
				logger.debug(servletRequest.getMethod() + " " + servletRequest.getRequestURI());
			}
			// 创建一个空的属性Map
			Map<String, Object> attributes = new HashMap<>();
			// 如果握手之前的拦截器链应用成功
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				// 返回
				return;
			}
			// 进行握手
			this.handshakeHandler.doHandshake(request, response, this.wsHandler, attributes);
			// 应用握手之后的拦截器链
			chain.applyAfterHandshake(request, response, null);
		} catch (HandshakeFailureException ex) {
			// 捕获握手失败异常
			failure = ex;
		} catch (Exception ex) {
			// 捕获其他异常
			failure = new HandshakeFailureException("Uncaught failure for request " + request.getURI(), ex);
		} finally {
			// 如果发生了握手失败
			if (failure != null) {
				// 应用握手之后的拦截器链
				chain.applyAfterHandshake(request, response, failure);
				// 关闭响应
				response.close();
				// 抛出握手失败异常
				throw failure;
			}
			// 关闭响应
			response.close();
		}
	}

}
