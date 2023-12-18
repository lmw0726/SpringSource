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

package org.springframework.web.reactive.socket.server.upgrade;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * 用于 Jetty 的 RequestUpgradeStrategy 实现。
 * <p>
 * 实现了 {@link RequestUpgradeStrategy} 接口，并且是一个生命周期（Lifecycle）组件。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, Lifecycle {

	/**
	 * 用于持有 JettyWebSocketHandlerAdapter 的 ThreadLocal 对象。
	 * <p>
	 * 使用 NamedThreadLocal，确保在多线程环境中具有唯一的名称。
	 */
	private static final ThreadLocal<WebSocketHandlerContainer> adapterHolder =
			new NamedThreadLocal<>("JettyWebSocketHandlerAdapter");

	/**
	 * WebSocket 策略对象，用于配置 WebSocket 连接的一些属性。
	 */
	@Nullable
	private WebSocketPolicy webSocketPolicy;

	/**
	 * WebSocketServerFactory 对象，用于创建 WebSocket 服务器。
	 */
	@Nullable
	private WebSocketServerFactory factory;

	/**
	 * ServletContext 对象，用于与底层 Servlet 容器进行交互。
	 */
	@Nullable
	private volatile ServletContext servletContext;

	/**
	 * 标识 JettyRequestUpgradeStrategy 是否处于运行状态。
	 */
	private volatile boolean running;

	/**
	 * 生命周期监视器对象，用于同步管理组件的生命周期。
	 */
	private final Object lifecycleMonitor = new Object();


	/**
	 * 配置用于初始化 WebSocketServerFactory 的 WebSocketPolicy。
	 *
	 * @param webSocketPolicy WebSocket 的设置信息
	 */
	public void setWebSocketPolicy(WebSocketPolicy webSocketPolicy) {
		this.webSocketPolicy = webSocketPolicy;
	}

	/**
	 * 返回配置的 WebSocketPolicy，如果有的话。
	 *
	 * @return 配置的 WebSocketPolicy，可能为 null
	 */
	@Nullable
	public WebSocketPolicy getWebSocketPolicy() {
		return this.webSocketPolicy;
	}

	/**
	 * 启动 WebSocket 服务器工厂。
	 * <p>
	 * 使用 synchronized 来确保线程安全，并在确保未运行且 ServletContext 不为 null 时启动。
	 */
	@Override
	public void start() {
		// 使用生命周期监视器对象进行同步
		synchronized (this.lifecycleMonitor) {
			ServletContext servletContext = this.servletContext;
			// 如果未运行且存在 ServletContext
			if (!isRunning() && servletContext != null) {
				try {
					// 根据是否存在 WebSocketPolicy 对象创建 WebSocketServerFactory
					this.factory = (this.webSocketPolicy != null ?
							new WebSocketServerFactory(servletContext, this.webSocketPolicy) :
							new WebSocketServerFactory(servletContext));

					// 设置 WebSocketServerFactory 的创建器
					this.factory.setCreator((request, response) -> {
						//获取WebSocket处理容器
						WebSocketHandlerContainer container = adapterHolder.get();
						String protocol = container.getProtocol();
						if (protocol != null) {
							//设置可接受的子协议
							response.setAcceptedSubProtocol(protocol);
						}
						// 返回相应的适配器
						return container.getAdapter();
					});

					// 启动 WebSocketServerFactory
					this.factory.start();
					this.running = true;
				} catch (Throwable ex) {
					// 发生异常时抛出 IllegalStateException
					throw new IllegalStateException("Unable to start WebSocketServerFactory", ex);
				}
			}
		}
	}

	/**
	 * 停止 WebSocket 服务器工厂。
	 * <p>
	 * 使用 synchronized 来确保线程安全，并在当前处于运行状态时执行停止操作。
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			// 如果当前处于运行状态
			if (isRunning()) {
				if (this.factory != null) {
					try {
						// 停止 WebSocket 服务器工厂
						this.factory.stop();
						// 设置状态为非运行状态
						this.running = false;
					} catch (Throwable ex) {
						// 发生异常时抛出 IllegalStateException
						throw new IllegalStateException("Failed to stop WebSocketServerFactory", ex);
					}
				}
			}
		}
	}


	/**
	 * 检查 WebSocket 服务器工厂是否正在运行。
	 *
	 * @return 如果 WebSocket 服务器工厂正在运行，则返回 true；否则返回 false
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 升级 HTTP 请求为 WebSocket 连接的方法。
	 *
	 * @param exchange             ServerWebExchange 对象，包含 HTTP 请求和响应信息
	 * @param handler              WebSocketHandler 对象，处理 WebSocket 连接的逻辑
	 * @param subProtocol          可选的子协议
	 * @param handshakeInfoFactory 用于创建 HandshakeInfo 对象的 Supplier
	 * @return 代表 WebSocket 连接的 Mono
	 */
	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
							  @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		// 获取 HTTP 请求和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		// 将请求和响应对象转换为原生 Servlet 请求和响应对象
		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);

		// 获取握手信息和数据缓冲工厂
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory factory = response.bufferFactory();

		// 懒启动 WebSocket
		startLazily(servletRequest);

		// 断言确保 WebSocketServerFactory 不为 null
		Assert.state(this.factory != null, "No WebSocketServerFactory available");

		// 检查是否为 WebSocket 升级请求
		boolean isUpgrade = this.factory.isUpgradeRequest(servletRequest, servletResponse);
		Assert.isTrue(isUpgrade, "Not a WebSocket handshake");

		// 触发 WebFlux preCommit 操作并执行升级
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					// 创建 JettyWebSocketHandlerAdapter，并传入上下文视图和握手信息
					JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new JettyWebSocketSession(session, handshakeInfo, factory));

					try {
						// 将当前适配器和子协议设置到 ThreadLocal 中
						adapterHolder.set(new WebSocketHandlerContainer(adapter, subProtocol));
						// 接受 WebSocket 连接
						this.factory.acceptWebSocket(servletRequest, servletResponse);
					} catch (IOException ex) {
						// 发生异常时返回 Mono 错误
						return Mono.error(ex);
					} finally {
						// 移除 ThreadLocal 中的适配器信息
						adapterHolder.remove();
					}
					// 成功时返回空的 Mono
					return Mono.empty();
				}));
	}

	/**
	 * 懒启动 WebSocket 服务器工厂。
	 *
	 * @param request Servlet 请求对象
	 */
	private void startLazily(HttpServletRequest request) {
		// 如果当前已经在运行，则直接返回
		if (isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			// 再次检查状态，确保仅在未运行时执行启动操作
			if (!isRunning()) {
				// 设置 ServletContext 对象并启动 WebSocket 服务器工厂
				this.servletContext = request.getServletContext();
				start();
			}
		}
	}


	/**
	 * WebSocketHandlerContainer 内部类，用于包装 JettyWebSocketHandlerAdapter 和协议信息。
	 */
	private static class WebSocketHandlerContainer {

		/**
		 * JettyWebSocketHandlerAdapter 对象
		 */
		private final JettyWebSocketHandlerAdapter adapter;

		/**
		 * 可能的协议信息
		 */
		@Nullable
		private final String protocol;

		/**
		 * 构造方法，接收 JettyWebSocketHandlerAdapter 对象和可能的协议信息作为参数。
		 *
		 * @param adapter  JettyWebSocketHandlerAdapter 对象
		 * @param protocol 可能的协议信息
		 */
		public WebSocketHandlerContainer(JettyWebSocketHandlerAdapter adapter, @Nullable String protocol) {
			this.adapter = adapter;
			this.protocol = protocol;
		}

		/**
		 * 获取 JettyWebSocketHandlerAdapter 对象。
		 *
		 * @return JettyWebSocketHandlerAdapter 对象
		 */
		public JettyWebSocketHandlerAdapter getAdapter() {
			return this.adapter;
		}

		/**
		 * 获取可能的协议信息。
		 *
		 * @return 可能的协议信息，可能为 null
		 */
		@Nullable
		public String getProtocol() {
			return this.protocol;
		}
	}


}
