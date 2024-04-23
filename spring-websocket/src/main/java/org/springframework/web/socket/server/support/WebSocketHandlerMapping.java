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

package org.springframework.web.socket.server.support;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link SimpleUrlHandlerMapping} 的扩展，支持将 WebSocket 握手请求更精确地映射到 {@link WebSocketHttpRequestHandler} 类型的处理器。
 * 还将 {@link Lifecycle} 方法委托给 {@link #getUrlMap()} 中实现它的处理器。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketHandlerMapping extends SimpleUrlHandlerMapping implements SmartLifecycle {

	/**
	 * 是否匹配升级WebSocket
	 */
	private boolean webSocketUpgradeMatch;

	/**
	 * 是否在运行中
	 */
	private volatile boolean running;


	/**
	 * 当设置了此标志时，如果匹配的处理器是 {@link WebSocketHttpRequestHandler}，
	 * 确保请求是 WebSocket 握手，即使用带有头部 {@code "Upgrade:websocket"} 的 HTTP GET 请求，
	 * 否则抑制匹配并返回 {@code null}，允许另一个 {@link org.springframework.web.servlet.HandlerMapping} 来匹配相同的 URL 路径。
	 *
	 * @param match 是否启用 {@code "Upgrade: websocket"} 匹配
	 * @since 5.3.5
	 */
	public void setWebSocketUpgradeMatch(boolean match) {
		this.webSocketUpgradeMatch = match;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		for (Object handler : getUrlMap().values()) {
			if (handler instanceof ServletContextAware) {
				// 如果处理器是 ServletContextAware 子类，则设置Servlet上下文
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					// 如果处理器是 Lifecycle 子类，则启动生命周期
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					// 如果处理器是 Lifecycle 子类，则关闭生命周期
					((Lifecycle) handler).stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		// 获取内部处理器
		Object handler = super.getHandlerInternal(request);
		// 如果匹配升级WebSocket，则返回处理器，否则返回null
		return (matchWebSocketUpgrade(handler, request) ? handler : null);
	}

	private boolean matchWebSocketUpgrade(@Nullable Object handler, HttpServletRequest request) {
		// 如果处理器是 HandlerExecutionChain 实例，则获取内部处理器
		handler = (handler instanceof HandlerExecutionChain ?
				((HandlerExecutionChain) handler).getHandler() : handler);

		// 如果启用了 WebSocket 升级匹配，并且处理器是 WebSocketHttpRequestHandler，则检查请求是否是 WebSocket 升级请求
		if (this.webSocketUpgradeMatch && handler instanceof WebSocketHttpRequestHandler) {
			// 获取请求头中的升级属性
			String header = request.getHeader(HttpHeaders.UPGRADE);
			// 如果是Get方法，并且升级为websocket，则返回true
			return (request.getMethod().equals("GET") &&
					header != null && header.equalsIgnoreCase("websocket"));
		}

		// 默认情况下返回 true
		return true;
	}

}
