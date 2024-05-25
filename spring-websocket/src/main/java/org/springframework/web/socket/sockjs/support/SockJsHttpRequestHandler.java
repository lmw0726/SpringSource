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

package org.springframework.web.socket.sockjs.support;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 允许将 {@link SockJsService} 映射到 Servlet 容器中的请求的 {@link HttpRequestHandler}。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class SockJsHttpRequestHandler
		implements HttpRequestHandler, CorsConfigurationSource, Lifecycle, ServletContextAware {

	// 没有日志记录：HTTP 传输太冗长，我们不知道足够多的信息来记录任何有价值的东西
	/**
	 * SockJS 服务
	 */
	private final SockJsService sockJsService;

	/**
	 * WebSocket 处理器
	 */
	private final WebSocketHandler webSocketHandler;

	/**
	 * SockJS是否在运行
	 */
	private volatile boolean running;


	/**
	 * 创建一个新的 SockJsHttpRequestHandler。
	 *
	 * @param sockJsService    SockJS 服务
	 * @param webSocketHandler WebSocket 处理程序
	 */
	public SockJsHttpRequestHandler(SockJsService sockJsService, WebSocketHandler webSocketHandler) {
		Assert.notNull(sockJsService, "SockJsService must not be null");
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		this.sockJsService = sockJsService;
		this.webSocketHandler =
				new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(webSocketHandler));
	}


	/**
	 * 返回 {@link SockJsService}。
	 */
	public SockJsService getSockJsService() {
		return this.sockJsService;
	}

	/**
	 * 返回 {@link WebSocketHandler}。
	 */
	public WebSocketHandler getWebSocketHandler() {
		return this.webSocketHandler;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.sockJsService instanceof ServletContextAware) {
			// 如果 SockJS 服务是 ServletContextAware 类型，设置 Servlet 上下文
			((ServletContextAware) this.sockJsService).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		// 如果SockJS服务尚未运行
		if (!isRunning()) {
			// 设置运行状态为true
			this.running = true;
			// 如果SockJS服务实现了Lifecycle接口
			if (this.sockJsService instanceof Lifecycle) {
				// 启动SockJS服务
				((Lifecycle) this.sockJsService).start();
			}
		}
	}

	@Override
	public void stop() {
		// 如果SockJS服务正在运行
		if (isRunning()) {
			// 设置运行状态为false
			this.running = false;
			// 如果SockJS服务实现了Lifecycle接口
			if (this.sockJsService instanceof Lifecycle) {
				// 停止SockJS服务
				((Lifecycle) this.sockJsService).stop();
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

		try {
			// 处理SockJS请求
			this.sockJsService.handleRequest(request, response, getSockJsPath(servletRequest), this.webSocketHandler);
		} catch (Exception ex) {
			// 捕获异常并抛出SockJS异常
			throw new SockJsException("Uncaught failure in SockJS request, uri=" + request.getURI(), ex);
		}
	}

	private String getSockJsPath(HttpServletRequest servletRequest) {
		// 获取路径属性名
		String attribute = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
		// 从ServletRequest中获取路径
		String path = (String) servletRequest.getAttribute(attribute);
		// 如果路径长度大于0且第一个字符不是斜杠，则在路径前添加斜杠
		return (path.length() > 0 && path.charAt(0) != '/' ? "/" + path : path);
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		// 如果SockJS服务实现了CorsConfigurationSource接口
		if (this.sockJsService instanceof CorsConfigurationSource) {
			// 获取Cors配置
			return ((CorsConfigurationSource) this.sockJsService).getCorsConfiguration(request);
		}
		// 否则返回null
		return null;
	}

}
