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

package org.springframework.web.reactive.socket.server.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code WebSocketService}的实现，通过委派给一个{@link RequestUpgradeStrategy}来处理WebSocket HTTP握手请求，
 * 该策略可以自动从类路径中检测（无参构造函数）但也可以显式配置。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HandshakeWebSocketService implements WebSocketService, Lifecycle {
	/**
	 * WebSocket握手时用于标识WebSocket密钥的HTTP标头键。
	 */
	private static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

	/**
	 * WebSocket握手时用于表示选定子协议的HTTP标头键。
	 */
	private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

	/**
	 * 用于表示空属性的Mono，即一个空的Map。
	 */
	private static final Mono<Map<String, Object>> EMPTY_ATTRIBUTES = Mono.just(Collections.emptyMap());


	/**
	 * 检测是否存在Tomcat容器。
	 */
	private static final boolean tomcatPresent;

	/**
	 * 检测是否存在Jetty容器。
	 */
	private static final boolean jettyPresent;

	/**
	 * 检测是否存在Jetty 10容器。
	 */
	private static final boolean jetty10Present;

	/**
	 * 检测是否存在Undertow容器。
	 */
	private static final boolean undertowPresent;

	/**
	 * 检测是否存在Reactor Netty容器。
	 */
	private static final boolean reactorNettyPresent;

	static {
		ClassLoader loader = HandshakeWebSocketService.class.getClassLoader();
		// 检测Tomcat容器是否存在
		tomcatPresent = ClassUtils.isPresent("org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", loader);
		// 检测Jetty容器是否存在
		jettyPresent = ClassUtils.isPresent("org.eclipse.jetty.websocket.server.WebSocketServerFactory", loader);
		// 检测Jetty 10容器是否存在
		jetty10Present = ClassUtils.isPresent("org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", loader);
		// 检测Undertow容器是否存在
		undertowPresent = ClassUtils.isPresent("io.undertow.websockets.WebSocketProtocolHandshakeHandler", loader);
		// 检测Reactor Netty容器是否存在
		reactorNettyPresent = ClassUtils.isPresent("reactor.netty.http.server.HttpServerResponse", loader);
	}

	/**
	 * 用于记录日志的静态日志记录器。
	 */
	protected static final Log logger = LogFactory.getLog(HandshakeWebSocketService.class);

	/**
	 * 升级策略。
	 */
	private final RequestUpgradeStrategy upgradeStrategy;

	/**
	 * 会话属性断言（可为null）。
	 */
	@Nullable
	private Predicate<String> sessionAttributePredicate;

	/**
	 * 标志服务是否正在运行。
	 */
	private volatile boolean running;


	/**
	 * 默认构造函数，自动基于类路径检测发现要使用的{@link RequestUpgradeStrategy}。
	 */
	public HandshakeWebSocketService() {
		this(initUpgradeStrategy());
	}

	/**
	 * 带有要使用的{@link RequestUpgradeStrategy}的替代构造函数。
	 *
	 * @param upgradeStrategy 要使用的策略
	 */
	public HandshakeWebSocketService(RequestUpgradeStrategy upgradeStrategy) {
		Assert.notNull(upgradeStrategy, "RequestUpgradeStrategy is required");
		this.upgradeStrategy = upgradeStrategy;
	}

	/**
	 * 初始化WebSocket请求升级策略
	 *
	 * @return WebSocket请求升级策略
	 */
	private static RequestUpgradeStrategy initUpgradeStrategy() {
		String className;
		// 检查当前环境下是否存在 Tomcat
		if (tomcatPresent) {
			className = "TomcatRequestUpgradeStrategy";
		} else if (jettyPresent) {
			// 如果没有 Tomcat，检查是否存在 Jetty
			className = "JettyRequestUpgradeStrategy";
		} else if (jetty10Present) {
			// 如果没有 Jetty，检查是否存在 Jetty 10
			className = "Jetty10RequestUpgradeStrategy";
		} else if (undertowPresent) {
			// 如果没有 Jetty 10，检查是否存在 Undertow
			className = "UndertowRequestUpgradeStrategy";
		} else if (reactorNettyPresent) {
			// 如果没有 Undertow，检查是否存在 Reactor Netty（常用于 WebClient）
			// 尽可能晚地使用 Reactor Netty
			className = "ReactorNettyRequestUpgradeStrategy";
		} else {
			// 如果没有任何适合的策略，则抛出异常
			throw new IllegalStateException("No suitable default RequestUpgradeStrategy found");
		}

		try {
			className = "org.springframework.web.reactive.socket.server.upgrade." + className;
			// 尝试动态加载类并实例化对应的 RequestUpgradeStrategy
			Class<?> clazz = ClassUtils.forName(className, HandshakeWebSocketService.class.getClassLoader());
			return (RequestUpgradeStrategy) ReflectionUtils.accessibleConstructor(clazz).newInstance();
		} catch (Throwable ex) {
			// 如果加载或实例化失败，则抛出异常
			throw new IllegalStateException(
					"Failed to instantiate RequestUpgradeStrategy: " + className, ex);
		}
	}


	/**
	 * 返回用于 WebSocket 请求的 {@link RequestUpgradeStrategy}。
	 *
	 * @return WebSocket请求升级策略
	 */
	public RequestUpgradeStrategy getUpgradeStrategy() {
		return this.upgradeStrategy;
	}

	/**
	 * 配置用于提取{@link org.springframework.web.server.WebSession WebSession}属性并用它们初始化WebSocket会话的断言。
	 * <p>默认情况下，未设置此项，因此不会传递任何属性。
	 *
	 * @param predicate 断言
	 * @since 5.1
	 */
	public void setSessionAttributePredicate(@Nullable Predicate<String> predicate) {
		this.sessionAttributePredicate = predicate;
	}

	/**
	 * 返回从{@code WebSession}属性初始化WebSocket会话的配置断言。
	 *
	 * @return WebSocket会话的配置断言
	 * @since 5.1
	 */
	@Nullable
	public Predicate<String> getSessionAttributePredicate() {
		return this.sessionAttributePredicate;
	}


	/**
	 * 重写父类的 start 方法
	 */
	@Override
	public void start() {
		// 检查当前状态是否已经在运行
		if (!isRunning()) {
			// 将运行状态设置为 true
			this.running = true;
			// 调用 doStart 方法执行启动操作
			doStart();
		}
	}

	/**
	 * 执行具体的启动操作，这里针对升级策略进行了处理
	 */
	protected void doStart() {
		// 判断升级策略是否实现了 Lifecycle 接口
		if (getUpgradeStrategy() instanceof Lifecycle) {
			// 如果是，则调用其 start 方法
			((Lifecycle) getUpgradeStrategy()).start();
		}
	}

	/**
	 * 重写父类的 stop 方法
	 */
	@Override
	public void stop() {
		// 检查当前状态是否在运行
		if (isRunning()) {
			// 将运行状态设置为 false
			this.running = false;
			// 调用 doStop 方法执行停止操作
			doStop();
		}
	}

	/**
	 * 执行具体的停止操作，这里同样针对升级策略进行了处理
	 */
	protected void doStop() {
		// 判断升级策略是否实现了 Lifecycle 接口
		if (getUpgradeStrategy() instanceof Lifecycle) {
			// 如果是，则调用其 stop 方法
			((Lifecycle) getUpgradeStrategy()).stop();
		}
	}

	/**
	 * 获取当前对象的运行状态
	 *
	 * @return 返回当前运行状态，true 表示正在运行，false 表示未运行
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 处理 WebSocket 请求
	 *
	 * @param exchange WebSocket 服务器交换对象
	 * @param handler  WebSocket 处理程序
	 * @return 返回一个 Mono<Void>，表示处理请求的结果
	 */
	@Override
	public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
		// 获取请求信息
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		HttpHeaders headers = request.getHeaders();

		// 检查请求方法是否为 GET
		if (HttpMethod.GET != method) {
			return Mono.error(new MethodNotAllowedException(
					request.getMethodValue(), Collections.singleton(HttpMethod.GET)));
		}

		// 检查 Upgrade 头部信息是否为 WebSocket
		if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
			return handleBadRequest(exchange, "Invalid 'Upgrade' header: " + headers);
		}

		// 检查 Connection 头部信息是否包含 Upgrade
		List<String> connectionValue = headers.getConnection();
		if (!connectionValue.contains("Upgrade") && !connectionValue.contains("upgrade")) {
			return handleBadRequest(exchange, "Invalid 'Connection' header: " + headers);
		}

		// 获取 Sec-WebSocket-Key 头部信息
		String key = headers.getFirst(SEC_WEBSOCKET_KEY);
		if (key == null) {
			return handleBadRequest(exchange, "Missing \"Sec-WebSocket-Key\" header");
		}

		// 选择协议
		String protocol = selectProtocol(headers, handler);

		// 初始化属性并执行升级策略
		return initAttributes(exchange).flatMap(attributes ->
				this.upgradeStrategy.upgrade(exchange, handler, protocol,
						() -> createHandshakeInfo(exchange, request, protocol, attributes))
		);
	}

	/**
	 * 处理请求错误情况
	 *
	 * @param exchange WebSocket 服务器交换对象
	 * @param reason   错误原因描述
	 * @return 返回一个 Mono<Void>，表示处理错误的结果
	 */
	private Mono<Void> handleBadRequest(ServerWebExchange exchange, String reason) {
		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + reason);
		}
		return Mono.error(new ServerWebInputException(reason));
	}

	/**
	 * 选择协议
	 *
	 * @param headers WebSocket 请求的头部信息
	 * @param handler WebSocket 处理程序
	 * @return 返回选中的协议，如果没有匹配的协议则返回 null
	 */
	@Nullable
	private String selectProtocol(HttpHeaders headers, WebSocketHandler handler) {
		// 获取 Sec-WebSocket-Protocol 头部信息
		String protocolHeader = headers.getFirst(SEC_WEBSOCKET_PROTOCOL);
		if (protocolHeader != null) {
			// 获取处理程序支持的子协议列表
			List<String> supportedProtocols = handler.getSubProtocols();
			// 遍历请求中的协议列表
			for (String protocol : StringUtils.commaDelimitedListToStringArray(protocolHeader)) {
				// 如果处理程序支持当前协议，则返回该协议
				if (supportedProtocols.contains(protocol)) {
					return protocol;
				}
			}
		}
		// 如果没有匹配的协议，则返回 null
		return null;
	}

	/**
	 * 初始化属性
	 *
	 * @param exchange WebSocket 服务器交换对象
	 * @return 返回一个 Mono，包含从会话中过滤并收集的属性映射
	 */
	private Mono<Map<String, Object>> initAttributes(ServerWebExchange exchange) {
		// 检查会话属性过滤器是否为空
		if (this.sessionAttributePredicate == null) {
			// 如果为空，返回一个空属性的 Mono
			return EMPTY_ATTRIBUTES;
		}
		// 如果不为空，从会话中过滤并收集属性
		return exchange.getSession().map(session ->
				session.getAttributes().entrySet().stream()
						.filter(entry -> this.sessionAttributePredicate.test(entry.getKey()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}


	/**
	 * 创建握手信息
	 *
	 * @param exchange   WebSocket 服务器交换对象
	 * @param request    WebSocket 服务器请求对象
	 * @param protocol   所选协议（可为 null）
	 * @param attributes 属性映射
	 * @return 返回一个 HandshakeInfo 对象，包含了握手所需的信息
	 */
	private HandshakeInfo createHandshakeInfo(ServerWebExchange exchange, ServerHttpRequest request,
											  @Nullable String protocol, Map<String, Object> attributes) {

		// 获取请求的 URI
		URI uri = request.getURI();
		// 复制请求头，因为在握手 HTTP 交换完成后，服务器实现可能会重用请求头
		HttpHeaders headers = new HttpHeaders();
		headers.addAll(request.getHeaders());
		// 获取请求中的 Cookie
		MultiValueMap<String, HttpCookie> cookies = request.getCookies();
		// 获取身份信息
		Mono<Principal> principal = exchange.getPrincipal();
		// 获取日志前缀
		String logPrefix = exchange.getLogPrefix();
		// 获取远程地址
		InetSocketAddress remoteAddress = request.getRemoteAddress();

		// 返回一个包含握手信息的 HandshakeInfo 对象
		return new HandshakeInfo(uri, headers, cookies, principal, protocol, remoteAddress, attributes, logPrefix);
	}

}
