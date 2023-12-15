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

package org.springframework.web.reactive.socket.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.io.UpgradeListener;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.function.Function;


/**
 * 用于与Jetty的org.eclipse.jetty.websocket.client.WebSocketClient一起使用的WebSocketClient实现。
 * <p>
 * 注意：Jetty的WebSocketClient需要进行生命周期管理，必须启动和停止。
 * 当此类声明为Spring bean并使用默认构造函数创建时，这将自动管理。
 * 有关更多详情，请参阅构造函数注释。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketClient implements WebSocketClient, Lifecycle {

	// 用于加载类的类加载器
	private static ClassLoader loader = JettyWebSocketClient.class.getClassLoader();

	// 标志是否存在Jetty 10的类
	private static final boolean jetty10Present;

	static {
		// 检查是否存在Jetty 10的类
		jetty10Present = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.client.JettyUpgradeListener", loader);
	}

	// 日志记录器
	private static final Log logger = LogFactory.getLog(JettyWebSocketClient.class);

	// Jetty的WebSocket客户端
	private final org.eclipse.jetty.websocket.client.WebSocketClient jettyClient;

	// 标志是否外部管理Jetty的WebSocket客户端
	private final boolean externallyManaged;

	// 升级助手，根据Jetty版本选择不同的UpgradeHelper实现
	private final UpgradeHelper upgradeHelper =
			(jetty10Present ? new Jetty10UpgradeHelper() : new Jetty9UpgradeHelper());

	/**
	 * 默认构造函数，创建并管理Jetty的WebSocketClient实例。
	 * 可以通过getJettyClient()方法获取实例以进行进一步配置。
	 */
	public JettyWebSocketClient() {
		this.jettyClient = new org.eclipse.jetty.websocket.client.WebSocketClient();
		this.externallyManaged = false;
	}

	/**
	 * 接受Jetty WebSocketClient实例的构造函数。
	 *
	 * @param jettyClient Jetty WebSocketClient实例
	 */
	public JettyWebSocketClient(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient) {
		this.jettyClient = jettyClient;
		this.externallyManaged = true;
	}

	/**
	 * 返回底层的Jetty WebSocketClient。
	 *
	 * @return Jetty WebSocketClient实例
	 */
	public org.eclipse.jetty.websocket.client.WebSocketClient getJettyClient() {
		return this.jettyClient;
	}

	/**
	 * 启动WebSocket客户端。
	 */
	@Override
	public void start() {
		if (!this.externallyManaged) {
			try {
				this.jettyClient.start();
			} catch (Exception ex) {
				throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
			}
		}
	}

	/**
	 * 停止WebSocket客户端。
	 */
	@Override
	public void stop() {
		if (!this.externallyManaged) {
			try {
				this.jettyClient.stop();
			} catch (Exception ex) {
				throw new IllegalStateException("Error stopping Jetty WebSocketClient", ex);
			}
		}
	}


	/**
	 * 检查WebSocket客户端是否在运行。
	 *
	 * @return 如果客户端正在运行则返回true，否则返回false
	 */
	@Override
	public boolean isRunning() {
		return this.jettyClient.isRunning();
	}

	/**
	 * 执行WebSocket连接到指定的URI，并使用默认的头信息。
	 *
	 * @param url     WebSocket连接的URI
	 * @param handler WebSocket处理程序
	 * @return 返回表示连接完成的Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	/**
	 * 执行WebSocket连接到指定的URI，并使用给定的头信息。
	 *
	 * @param url     WebSocket连接的URI
	 * @param headers 头信息
	 * @param handler WebSocket处理程序
	 * @return 返回表示连接完成的Mono<Void>
	 */
	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	/**
	 * 执行WebSocket连接的内部方法，创建连接并进行协议升级。
	 *
	 * @param url     WebSocket连接的URI
	 * @param headers 头信息
	 * @param handler WebSocket处理程序
	 * @return 返回表示连接完成的Mono<Void>
	 */
	private Mono<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		// 创建一个空的Sinks
		Sinks.Empty<Void> completionSink = Sinks.empty();
		// 返回一个根据上下文延迟执行的Mono
		return Mono.deferContextual(contextView -> {
			// 如果日志级别为DEBUG，则记录连接到URL的调试信息
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to " + url);
			}
			// 创建WebSocket处理程序，并设置完成Sink
			Object jettyHandler = createHandler(
					url, ContextWebSocketHandler.decorate(handler, contextView), completionSink);
			// 创建升级请求对象，并设置子协议
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			request.setSubProtocols(handler.getSubProtocols());
			// 调用升级助手的升级方法进行WebSocket协议升级
			return this.upgradeHelper.upgrade(
					this.jettyClient, jettyHandler, url, request, headers, completionSink);
		});

	}

	/**
	 * 创建WebSocket处理程序。
	 *
	 * @param url        WebSocket连接的URI
	 * @param handler    WebSocket处理程序
	 * @param completion 完成Sink
	 * @return 返回创建的WebSocket处理程序对象
	 */
	private Object createHandler(URI url, WebSocketHandler handler, Sinks.Empty<Void> completion) {
		// 创建一个sessionFactory，用于根据会话创建JettyWebSocketSession对象
		Function<Session, JettyWebSocketSession> sessionFactory = session -> {
			// 创建握手信息
			HandshakeInfo info = createHandshakeInfo(url, session);
			// 返回一个新的JettyWebSocketSession对象
			return new JettyWebSocketSession(session, info, DefaultDataBufferFactory.sharedInstance, completion);
		};

		// 根据Jetty版本选择创建不同的WebSocket处理程序适配器
		return (jetty10Present ?
				// 如果是Jetty 10，返回Jetty10WebSocketHandlerAdapter
				new Jetty10WebSocketHandlerAdapter(handler, sessionFactory) :
				// 如果是Jetty 9，返回JettyWebSocketHandlerAdapter
				new JettyWebSocketHandlerAdapter(handler, sessionFactory));

	}

	/**
	 * 创建握手信息。
	 *
	 * @param url          WebSocket连接的URI
	 * @param jettySession Jetty的会话对象
	 * @return 返回创建的握手信息
	 */
	private HandshakeInfo createHandshakeInfo(URI url, Session jettySession) {
		// 创建新的HttpHeaders对象
		HttpHeaders headers = new HttpHeaders();
		// 将Jetty会话的升级响应头信息放入新的HttpHeaders对象中
		jettySession.getUpgradeResponse().getHeaders().forEach(headers::put);
		// 获取"Sec-WebSocket-Protocol"头信息的值
		String protocol = headers.getFirst("Sec-WebSocket-Protocol");
		// 返回一个包含握手信息的HandshakeInfo对象
		return new HandshakeInfo(url, headers, Mono.empty(), protocol);
	}


	/**
	 * UpgradeHelper接口封装了Jetty 9.4和10之间的不兼容变化。
	 */
	private interface UpgradeHelper {

		/**
		 * 进行WebSocket协议升级的方法
		 *
		 * @param jettyClient    Jetty的WebSocket客户端
		 * @param jettyHandler   Jetty的WebSocket处理程序
		 * @param url            WebSocket连接的URI
		 * @param request        升级请求
		 * @param headers        头信息
		 * @param completionSink 用于通知升级完成的Sink
		 * @return 返回Mono<Void>表示升级的完成状态
		 */
		Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
						   Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
						   Sinks.Empty<Void> completionSink);
	}


	/**
	 * Jetty9UpgradeHelper类实现UpgradeHelper接口，用于处理在Jetty 9中的升级逻辑。
	 */
	private static class Jetty9UpgradeHelper implements UpgradeHelper {

		/**
		 * 实现UpgradeHelper接口的升级方法，在Jetty 9中进行WebSocket协议升级。
		 *
		 * @param jettyClient    Jetty的WebSocket客户端
		 * @param jettyHandler   Jetty的WebSocket处理程序
		 * @param url            WebSocket连接的URI
		 * @param request        升级请求
		 * @param headers        头信息
		 * @param completionSink 用于通知升级完成的Sink
		 * @return 返回Mono<Void>表示升级的完成状态
		 */
		@Override
		public Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
								  Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
								  Sinks.Empty<Void> completionSink) {

			try {
				// 在Jetty 9中调用connect方法进行WebSocket协议升级，并传递DefaultUpgradeListener以处理升级过程中的事件
				jettyClient.connect(jettyHandler, url, request, new DefaultUpgradeListener(headers));
				return completionSink.asMono();
			} catch (IOException ex) {
				// 如果出现IOException则返回错误的Mono
				return Mono.error(ex);
			}
		}
	}


	/**
	 * DefaultUpgradeListener类实现UpgradeListener接口，用于监听升级事件。
	 */
	private static class DefaultUpgradeListener implements UpgradeListener {

		/**
		 * HTTP请求头
		 */
		private final HttpHeaders headers;

		/**
		 * 构造函数，接收HttpHeaders参数
		 *
		 * @param headers 头信息
		 */
		public DefaultUpgradeListener(HttpHeaders headers) {
			this.headers = headers;
		}

		/**
		 * 当握手请求事件发生时调用，将HttpHeaders中的头信息设置到UpgradeRequest中
		 *
		 * @param request 升级请求
		 */
		@Override
		public void onHandshakeRequest(UpgradeRequest request) {
			this.headers.forEach(request::setHeader);
		}

		/**
		 * 当握手响应事件发生时调用
		 *
		 * @param response 升级响应
		 */
		@Override
		public void onHandshakeResponse(UpgradeResponse response) {
			// 在握手响应事件中不进行操作
		}
	}


	/**
	 * Jetty10UpgradeHelper类实现UpgradeHelper接口，用于处理升级逻辑。
	 */
	private static class Jetty10UpgradeHelper implements UpgradeHelper {

		// 在Jetty 9上返回Future，在Jetty 10上返回CompletableFuture
		private static final Method connectMethod;

		static {
			try {
				// 加载WebSocketClient类并获取connect方法
				Class<?> type = loader.loadClass("org.eclipse.jetty.websocket.client.WebSocketClient");
				connectMethod = type.getMethod("connect", Object.class, URI.class, ClientUpgradeRequest.class);
			} catch (ClassNotFoundException | NoSuchMethodException ex) {
				// 如果未找到类或方法，则抛出异常
				throw new IllegalStateException("No compatible Jetty version found", ex);
			}
		}

		/**
		 * 实现UpgradeHelper接口的升级方法，用于在Jetty 10中进行WebSocket协议升级。
		 *
		 * @param jettyClient    Jetty的WebSocket客户端
		 * @param jettyHandler   Jetty的WebSocket处理程序
		 * @param url            WebSocket连接的URI
		 * @param request        升级请求
		 * @param headers        头信息
		 * @param completionSink 用于通知升级完成的Sink
		 * @return 返回Mono<Void>表示升级的完成状态
		 */
		@Override
		public Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
								  Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
								  Sinks.Empty<Void> completionSink) {

			// TODO: 传递JettyUpgradeListener参数以从HttpHeaders设置头信息（就像我们在Jetty 9中做的那样）
			// 这将需要使用JDK代理，因为它在Jetty 10中是新的

			// 调用Jetty的connect方法进行WebSocket协议升级
			ReflectionUtils.invokeMethod(connectMethod, jettyClient, jettyHandler, url, request);
			// 返回升级完成的Mono
			return completionSink.asMono();
		}
	}

}
