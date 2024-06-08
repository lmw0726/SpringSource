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

package org.springframework.web.socket.server.jetty;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.adapter.jetty.WebSocketToJettyExtensionConfigAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

/**
 * 用于 Jetty 9.4 的 {@link RequestUpgradeStrategy}。基于 Jetty 的内部 {@code org.eclipse.jetty.websocket.server.WebSocketHandler} 类。
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, ServletContextAware, Lifecycle {
	/**
	 * 容器持有者
	 */
	private static final ThreadLocal<WebSocketHandlerContainer> containerHolder =
			new NamedThreadLocal<>("WebSocketHandlerContainer");

	/**
	 * WebSocket策略
	 */
	@Nullable
	private WebSocketPolicy policy;

	/**
	 * WebSocket服务器工厂
	 */
	@Nullable
	private volatile WebSocketServerFactory factory;

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * 是否运行
	 */
	private volatile boolean running;

	/**
	 * 支持扩展的WebSocket扩展列表
	 */
	@Nullable
	private volatile List<WebSocketExtension> supportedExtensions;


	/**
	 * 默认构造函数通过其默认构造函数创建 {@link WebSocketServerFactory}，因此使用默认的 {@link WebSocketPolicy}。
	 */
	public JettyRequestUpgradeStrategy() {
		this.policy = WebSocketPolicy.newServerPolicy();
	}

	/**
	 * 接受 {@link WebSocketPolicy} 的构造函数，在创建 {@link WebSocketServerFactory} 实例时使用。
	 *
	 * @param policy 要使用的策略
	 * @since 4.3.5
	 */
	public JettyRequestUpgradeStrategy(WebSocketPolicy policy) {
		Assert.notNull(policy, "WebSocketPolicy must not be null");
		this.policy = policy;
	}

	/**
	 * 接受 {@link WebSocketServerFactory} 的构造函数。
	 *
	 * @param factory 要使用的预配置工厂
	 */
	public JettyRequestUpgradeStrategy(WebSocketServerFactory factory) {
		Assert.notNull(factory, "WebSocketServerFactory must not be null");
		this.factory = factory;
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void start() {
		// 如果尚未运行，则开始运行
		if (!isRunning()) {
			this.running = true;
			try {
				// 获取 WebSocket服务器工厂
				WebSocketServerFactory factory = this.factory;
				if (factory == null) {
					// 如果 WebSocket服务器工厂 不存在
					Assert.state(this.servletContext != null, "No ServletContext set");
					// 使用 Servlet上下文 和策略创建 WebSocket服务器工厂
					factory = new WebSocketServerFactory(this.servletContext, this.policy);
					this.factory = factory;
				}
				// 设置 WebSocket服务器工厂 的创建器，用于处理 WebSocket 请求
				factory.setCreator((request, response) -> {
					// 获取 WebSocket处理器容器
					WebSocketHandlerContainer container = containerHolder.get();
					Assert.state(container != null, "Expected WebSocketHandlerContainer");
					// 设置响应的接受子协议和扩展
					response.setAcceptedSubProtocol(container.getSelectedProtocol());
					// 设置扩展
					response.setExtensions(container.getExtensionConfigs());
					// 返回 WebSocket 处理器
					return container.getHandler();
				});
				// 启动 WebSocket服务器工厂
				factory.start();
			} catch (Throwable ex) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException("Unable to start Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public void stop() {
		// 如果正在运行，则停止运行
		if (isRunning()) {
			this.running = false;
			WebSocketServerFactory factory = this.factory;
			if (factory != null) {
				try {
					// 停止 WebSocket服务器工厂
					factory.stop();
				} catch (Throwable ex) {
					// 抛出 IllegalStateException 异常
					throw new IllegalStateException("Unable to stop Jetty WebSocketServerFactory", ex);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[]{String.valueOf(13)};
	}

	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		// 获取已支持的 WebSocket 扩展
		List<WebSocketExtension> extensions = this.supportedExtensions;
		if (extensions == null) {
			// 如果尚未初始化，则构建 WebSocket 扩展列表并保存
			extensions = buildWebSocketExtensions();
			this.supportedExtensions = extensions;
		}
		return extensions;
	}

	private List<WebSocketExtension> buildWebSocketExtensions() {
		// 获取已注册的 WebSocket 扩展名称集合
		Set<String> names = getExtensionNames();
		// 创建一个 WebSocketExtension 对象的列表，大小为已注册扩展名称的数量
		List<WebSocketExtension> result = new ArrayList<>(names.size());
		// 遍历已注册扩展名称集合
		for (String name : names) {
			// 创建相应的 WebSocketExtension 对象并添加到列表中
			result.add(new WebSocketExtension(name));
		}
		return result;
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	private Set<String> getExtensionNames() {
		// 获取 WebSocket服务器工厂 实例
		WebSocketServerFactory factory = this.factory;
		Assert.state(factory != null, "No WebSocketServerFactory available");
		try {
			// 调用 WebSocket服务器工厂 的 getAvailableExtensionNames() 方法获取可用扩展名称集合
			return factory.getAvailableExtensionNames();
		} catch (IncompatibleClassChangeError ex) {
			// 版本兼容性处理：在 Jetty 9.4.21 之前的版本中，ExtensionFactory 类从抽象类变为接口
			// Jetty 9.4.20.v20190813：ExtensionFactory（抽象类 -> 接口）
			// Jetty 9.4.21.v20190926：ExtensionFactory（接口 -> 抽象类） + 弃用
			// 在此版本之前需要使用反射调用 ExtensionFactory 的 getExtensionNames() 方法获取扩展名称集合
			// 获取 ExtensionFactory 类
			Class<?> clazz = org.eclipse.jetty.websocket.api.extensions.ExtensionFactory.class;
			// 获取 getExtensionNames() 方法
			Method method = ClassUtils.getMethod(clazz, "getExtensionNames");
			// 使用反射调用 getExtensionNames() 方法获取扩展名称集合
			Set<String> result = (Set<String>) ReflectionUtils.invokeMethod(method, factory.getExtensionFactory());
			// 返回扩展名称集合，如果结果为 null，则返回空集合
			return (result != null ? result : Collections.emptySet());
		}
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
						@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions, @Nullable Principal user,
						WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
		// 获取 HttpServlet请求 对象
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
		// 获取 HttpServlet响应 对象
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		// 获取 WebSocket服务器工厂 实例
		WebSocketServerFactory factory = this.factory;
		Assert.state(factory != null, "No WebSocketServerFactory available");
		Assert.isTrue(factory.isUpgradeRequest(servletRequest, servletResponse), "Not a WebSocket handshake");

		// 创建 JettyWebSocket会话 实例
		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
		// 创建 JettyWebSocket处理器适配器 实例
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(wsHandler, session);

		// 创建 WebSocket处理器容器 实例
		WebSocketHandlerContainer container =
				new WebSocketHandlerContainer(handlerAdapter, selectedProtocol, selectedExtensions);

		try {
			// 将 WebSocket处理器容器 实例设置到容器持有者中
			containerHolder.set(container);
			// 接受 WebSocket 连接请求
			factory.acceptWebSocket(servletRequest, servletResponse);
		} catch (IOException ex) {
			// 发生异常时抛出 HandshakeFailureException
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		} finally {
			// 清除容器持有者中的实例
			containerHolder.remove();
		}
	}


	private static class WebSocketHandlerContainer {
		/**
		 * Jetty WebSocket处理器适配器
		 */
		private final JettyWebSocketHandlerAdapter handler;

		/**
		 * 选中的协议
		 */
		@Nullable
		private final String selectedProtocol;

		/**
		 * 扩展配置列表
		 */
		private final List<ExtensionConfig> extensionConfigs;

		public WebSocketHandlerContainer(JettyWebSocketHandlerAdapter handler,
										 @Nullable String protocol, List<WebSocketExtension> extensions) {

			// 设置 WebSocket处理器 实例
			this.handler = handler;
			// 设置选定的协议
			this.selectedProtocol = protocol;

			// 如果扩展列表为空，则创建一个空列表
			if (CollectionUtils.isEmpty(extensions)) {
				this.extensionConfigs = new ArrayList<>(0);
			} else {
				// 否则，根据扩展列表的大小创建相应大小的列表，并逐个转换为对应的 Jetty 扩展配置
				this.extensionConfigs = new ArrayList<>(extensions.size());
				for (WebSocketExtension extension : extensions) {
					this.extensionConfigs.add(new WebSocketToJettyExtensionConfigAdapter(extension));
				}
			}
		}

		public JettyWebSocketHandlerAdapter getHandler() {
			return this.handler;
		}

		@Nullable
		public String getSelectedProtocol() {
			return this.selectedProtocol;
		}

		public List<ExtensionConfig> getExtensionConfigs() {
			return this.extensionConfigs;
		}
	}

}
