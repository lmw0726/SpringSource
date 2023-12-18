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

package org.springframework.web.reactive.socket.server.upgrade;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * 用于 Jetty 10 的 {@link RequestUpgradeStrategy}。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class Jetty10RequestUpgradeStrategy implements RequestUpgradeStrategy {
	/**
	 * WebSocket 创建器的类引用
	 */
	private static final Class<?> webSocketCreatorClass;

	/**
	 * 获取容器的方法引用
	 */
	private static final Method getContainerMethod;

	/**
	 * 升级方法的引用
	 */
	private static final Method upgradeMethod;

	/**
	 * 设置接受的子协议的方法引用
	 */
	private static final Method setAcceptedSubProtocol;


	static {
		// 获取当前类的类加载器
		ClassLoader loader = Jetty10RequestUpgradeStrategy.class.getClassLoader();
		try {
			// 加载并设置 WebSocket 创建器的类引用
			webSocketCreatorClass = loader.loadClass("org.eclipse.jetty.websocket.server.JettyWebSocketCreator");

			// 加载特定类并获取对应的方法引用
			Class<?> type = loader.loadClass("org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer");
			getContainerMethod = type.getMethod("getContainer", ServletContext.class);

			// 使用反射工具类获取升级方法的引用
			Method upgrade = ReflectionUtils.findMethod(type, "upgrade", (Class<?>[]) null);
			Assert.state(upgrade != null, "Upgrade method not found");
			upgradeMethod = upgrade;

			// 加载另一个特定类并获取对应的方法引用
			type = loader.loadClass("org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse");
			setAcceptedSubProtocol = type.getMethod("setAcceptedSubProtocol", String.class);
		} catch (Exception ex) {
			// 捕获异常并抛出 IllegalStateException
			throw new IllegalStateException("No compatible Jetty version found", ex);
		}
	}

	/**
	 * 将请求升级到WebSocket会话并使用给定的处理程序处理它。
	 *
	 * @param exchange             当前交换信息
	 * @param handler              WebSocket会话的处理程序
	 * @param subProtocol          处理程序选择的子协议
	 * @param handshakeInfoFactory 用于为WebSocket会话创建HandshakeInfo的工厂
	 * @return 完成的{@code Mono<Void>}，表示WebSocket会话处理的结果。
	 * @since 5.1
	 */
	@Override
	public Mono<Void> upgrade(
			ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		// 获取请求和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		// 将请求和响应对象转换为原生 Servlet 请求和响应对象
		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		ServletContext servletContext = servletRequest.getServletContext();

		// 获取握手信息和数据缓冲工厂
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory factory = response.bufferFactory();

		// 触发 WebFlux 的 preCommit 操作并执行升级
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					// 创建 Jetty10WebSocketHandlerAdapter，并传入上下文视图和握手信息
					Jetty10WebSocketHandlerAdapter adapter = new Jetty10WebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new JettyWebSocketSession(session, handshakeInfo, factory));

					try {
						// 创建 Jetty WebSocketCreator 对象并获取容器对象
						Object creator = createJettyWebSocketCreator(adapter, subProtocol);
						// 通过反射获取容器方法
						Object container = ReflectionUtils.invokeMethod(getContainerMethod, null, servletContext);

						// 调用升级方法进行 WebSocket 连接升级
						ReflectionUtils.invokeMethod(upgradeMethod, container, creator, servletRequest, servletResponse);
					} catch (Exception ex) {
						// 发生异常时返回 Mono 错误
						return Mono.error(ex);
					}
					// 成功时返回空的 Mono
					return Mono.empty();
				}));
	}

	/**
	 * 创建 Jetty WebSocket Creator 对象。
	 *
	 * @param adapter  Jetty10WebSocketHandlerAdapter 对象
	 * @param protocol 可能的协议
	 * @return Jetty WebSocket Creator 对象
	 */
	private static Object createJettyWebSocketCreator(
			Jetty10WebSocketHandlerAdapter adapter, @Nullable String protocol) {

		// 创建代理工厂对象
		ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);

		// 添加接口信息
		factory.addInterface(webSocketCreatorClass);

		// 添加 WebSocketCreatorInterceptor 切面
		factory.addAdvice(new WebSocketCreatorInterceptor(adapter, protocol));

		// 获取代理对象并返回
		return factory.getProxy();
	}


	/**
	 * WebSocketCreatorInterceptor 类，用作 JettyWebSocketCreator 的代理，提供 WebSocket 处理程序并设置子协议。
	 */
	private static class WebSocketCreatorInterceptor implements MethodInterceptor {

		/**
		 * Jetty10WebSocketHandlerAdapter 对象
		 */
		private final Jetty10WebSocketHandlerAdapter adapter;

		/**
		 * 可能的子协议
		 */
		@Nullable
		private final String protocol;

		/**
		 * 构造方法，接收 Jetty10WebSocketHandlerAdapter 对象和可能的子协议作为参数。
		 *
		 * @param adapter  Jetty10WebSocketHandlerAdapter 对象
		 * @param protocol 可能的子协议
		 */
		public WebSocketCreatorInterceptor(
				Jetty10WebSocketHandlerAdapter adapter, @Nullable String protocol) {

			this.adapter = adapter;
			this.protocol = protocol;
		}

		/**
		 * 实现 MethodInterceptor 接口的 invoke 方法。
		 *
		 * @param invocation MethodInvocation 对象
		 * @return Jetty10WebSocketHandlerAdapter 对象或 null
		 */
		@Nullable
		@Override
		public Object invoke(@NonNull MethodInvocation invocation) {
			// 如果存在子协议，则通过反射设置接受的子协议
			if (this.protocol != null) {
				ReflectionUtils.invokeMethod(
						setAcceptedSubProtocol, invocation.getArguments()[1], this.protocol);
			}
			// 返回 Jetty10WebSocketHandlerAdapter 对象
			return this.adapter;
		}
	}


}
