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

package org.springframework.web.socket.sockjs.transport;

import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HandshakeInterceptorChain;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.support.AbstractSockJsService;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 一个基本的 {@link org.springframework.web.socket.sockjs.SockJsService} 实现，
 * 支持基于 SPI 的传输处理和会话管理。
 *
 * <p>基于 {@link TransportHandler} SPI。{@code TransportHandlers} 还可以实现
 * {@link SockJsSessionFactory} 和 {@link HandshakeHandler} 接口。
 *
 * <p>有关请求映射的重要详细信息，请参见 {@link AbstractSockJsService} 基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class TransportHandlingSockJsService extends AbstractSockJsService implements SockJsServiceConfig, Lifecycle {

	/**
	 * Jackson2库 是否存在
	 */
	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", TransportHandlingSockJsService.class.getClassLoader());

	/**
	 * 传输类型 —— 传输处理器映射
	 */
	private final Map<TransportType, TransportHandler> handlers = new EnumMap<>(TransportType.class);

	/**
	 * 消息编码器
	 */
	@Nullable
	private SockJsMessageCodec messageCodec;

	/**
	 * WebSocket 握手请求拦截器。
	 */
	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();
	/**
	 * 会话编号 —— SockJs会话 映射
	 */
	private final Map<String, SockJsSession> sessions = new ConcurrentHashMap<>();

	/**
	 * 会话清除任务
	 */
	@Nullable
	private ScheduledFuture<?> sessionCleanupTask;

	/**
	 * 是否正在运行
	 */
	private volatile boolean running;


	/**
	 * 使用给定的 {@link TransportHandler handler} 类型创建一个 TransportHandlingSockJsService。
	 *
	 * @param scheduler 一个用于心跳消息和移除超时会话的任务调度器;
	 *                  提供的 TaskScheduler 应作为 Spring bean 声明，以确保它在启动时初始化并在应用程序停止时关闭
	 * @param handlers  要使用的一个或多个 {@link TransportHandler} 实现
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, TransportHandler... handlers) {
		this(scheduler, Arrays.asList(handlers));
	}

	/**
	 * 使用给定的 {@link TransportHandler handler} 类型创建一个 TransportHandlingSockJsService。
	 *
	 * @param scheduler 一个用于心跳消息和移除超时会话的任务调度器;
	 *                  提供的 TaskScheduler 应作为 Spring bean 声明，以确保它在启动时初始化并在应用程序停止时关闭
	 * @param handlers  要使用的一个或多个 {@link TransportHandler} 实现
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlers) {
		super(scheduler);

		// 如果 SockJs的处理器 为空
		if (CollectionUtils.isEmpty(handlers)) {
			// 记录警告日志：没有为 TransportHandlingSockJsService 指定传输处理程序
			logger.warn("No transport handlers specified for TransportHandlingSockJsService");
		} else {
			// 遍历传输处理程序
			for (TransportHandler handler : handlers) {
				// 初始化传输处理程序
				handler.initialize(this);
				// 将传输类型与处理程序关联并放入 handlers 映射中
				this.handlers.put(handler.getTransportType(), handler);
			}
		}

		// 如果存在 jackson2 库
		if (jackson2Present) {
			// 使用 Jackson2SockJsMessageCodec 作为消息编解码器
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}


	/**
	 * 返回每种传输类型的注册处理程序。
	 */
	public Map<TransportType, TransportHandler> getTransportHandlers() {
		return Collections.unmodifiableMap(this.handlers);
	}

	/**
	 * 设置用于编码和解码 SockJS 消息的编码器。
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		this.messageCodec = messageCodec;
	}

	@Override
	public SockJsMessageCodec getMessageCodec() {
		Assert.state(this.messageCodec != null, "A SockJsMessageCodec is required but not available: " +
				"Add Jackson to the classpath, or configure a custom SockJsMessageCodec.");
		return this.messageCodec;
	}

	/**
	 * 配置一个或多个 WebSocket 握手请求拦截器。
	 */
	public void setHandshakeInterceptors(@Nullable List<HandshakeInterceptor> interceptors) {
		this.interceptors.clear();
		if (interceptors != null) {
			this.interceptors.addAll(interceptors);
		}
	}

	/**
	 * 返回配置好的 WebSocket 握手请求拦截器。
	 */
	public List<HandshakeInterceptor> getHandshakeInterceptors() {
		return this.interceptors;
	}


	@Override
	public void start() {
		// 如果当前实例不在运行状态
		if (!isRunning()) {
			// 将运行状态设置为true
			this.running = true;
			// 遍历存储在handlers中的每个TransportHandler对象
			for (TransportHandler handler : this.handlers.values()) {
				// 检查handler是否是Lifecycle的实例
				if (handler instanceof Lifecycle) {
					// 如果是Lifecycle的实例，则调用其start方法
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		// 如果当前服务正在运行
		if (isRunning()) {
			// 将运行状态设置为 false
			this.running = false;
			// 遍历所有传输处理程序
			for (TransportHandler handler : this.handlers.values()) {
				// 如果传输处理程序实现了 Lifecycle 接口
				if (handler instanceof Lifecycle) {
					// 调用停止方法
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
	protected void handleRawWebSocketRequest(ServerHttpRequest request, ServerHttpResponse response,
											 WebSocketHandler handler) throws IOException {

		// 获取传输处理器
		TransportHandler transportHandler = this.handlers.get(TransportType.WEBSOCKET);

		// 如果传输处理器不是HandshakeHandler的实例
		if (!(transportHandler instanceof HandshakeHandler)) {
			// 记录错误日志，指示没有配置用于原始WebSocket消息的处理程序
			logger.error("No handler configured for raw WebSocket messages");
			// 设置响应状态码为404
			response.setStatusCode(HttpStatus.NOT_FOUND);
			// 返回，结束方法执行
			return;
		}

		// 使用拦截器列表和处理程序 创建 握手拦截器链。
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);

		HandshakeFailureException failure = null;

		try {
			// 创建存储请求属性的Map对象
			Map<String, Object> attributes = new HashMap<>();
			// 如果 应用握手前方法 返回false，则返回
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				return;
			}
			// 调用 传输处理器 的 doHandshake 方法进行握手
			((HandshakeHandler) transportHandler).doHandshake(request, response, handler, attributes);
			// 应用握手之后的拦截器
			chain.applyAfterHandshake(request, response, null);
		} catch (HandshakeFailureException ex) {
			// 捕捉HandshakeFailureException异常
			failure = ex;
		} catch (Exception ex) {
			// 捕捉其他异常
			failure = new HandshakeFailureException("Uncaught failure for request " + request.getURI(), ex);
		} finally {
			// 最终执行的操作
			if (failure != null) {
				// 应用握手之后的拦截器，传入失败信息
				chain.applyAfterHandshake(request, response, failure);
				// 抛出异常
				throw failure;
			}
		}
	}

	@Override
	protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
										  WebSocketHandler handler, String sessionId, String transport) throws SockJsException {// 根据传输类型的值获取传输类型
		TransportType transportType = TransportType.fromValue(transport);
		// 如果传输类型为 null
		if (transportType == null) {
			// 如果日志启用了警告级别
			if (logger.isWarnEnabled()) {
				// 记录警告日志，提示未知传输类型
				logger.warn(LogFormatUtils.formatValue("Unknown transport type for " + request.getURI(), -1, true));
			}
			// 设置响应状态码为 404（未找到）
			response.setStatusCode(HttpStatus.NOT_FOUND);
			// 结束方法执行
			return;
		}

		// 获取传输处理器
		TransportHandler transportHandler = this.handlers.get(transportType);
		// 如果传输处理器为 null
		if (transportHandler == null) {
			// 如果日志启用了警告级别
			if (logger.isWarnEnabled()) {
				// 记录警告日志，提示没有找到传输处理器
				logger.warn(LogFormatUtils.formatValue("No TransportHandler for " + request.getURI(), -1, true));
			}
			// 设置响应状态码为 404（未找到）
			response.setStatusCode(HttpStatus.NOT_FOUND);
			// 结束方法执行
			return;
		}

		SockJsException failure = null;
		// 创建握手拦截器链
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);

		try {
			// 获取传输类型支持的 HTTP 方法
			HttpMethod supportedMethod = transportType.getHttpMethod();
			// 如果当前请求的方法与支持的方法不匹配
			if (supportedMethod != request.getMethod()) {
				// 如果当前请求的方法为 OPTIONS，并且传输类型支持 CORS
				if (request.getMethod() == HttpMethod.OPTIONS && transportType.supportsCors()) {
					// 检查跨域并设置响应状态码为 204（无内容）
					if (checkOrigin(request, response, HttpMethod.OPTIONS, supportedMethod)) {
						response.setStatusCode(HttpStatus.NO_CONTENT);
						// 添加缓存响应头
						addCacheHeaders(response);
					}
					// 如果传输类型支持 CORS
				} else if (transportType.supportsCors()) {
					// 发送方法不允许的响应
					sendMethodNotAllowed(response, supportedMethod, HttpMethod.OPTIONS);
					// 如果传输类型不支持 CORS
				} else {
					// 发送方法不允许的响应
					sendMethodNotAllowed(response, supportedMethod);
				}
				// 结束方法执行
				return;
			}

			// 获取 SockJS 会话
			SockJsSession session = this.sessions.get(sessionId);
			// 是否为新会话标志初始化为 false
			boolean isNewSession = false;
			// 如果会话为 null
			if (session == null) {
				// 如果传输处理器是 SockJS 会话工厂
				if (transportHandler instanceof SockJsSessionFactory) {
					// 创建 SockJS 会话工厂
					SockJsSessionFactory sessionFactory = (SockJsSessionFactory) transportHandler;
					// 创建会话前的拦截器应用，如果返回 false，则结束方法执行
					Map<String, Object> attributes = new HashMap<>();
					if (!chain.applyBeforeHandshake(request, response, attributes)) {
						return;
					}
					// 创建新的 SockJS 会话
					session = createSockJsSession(sessionId, sessionFactory, handler, attributes);
					// 新会话标志设置为 true
					isNewSession = true;
					// 如果传输处理器不是 SockJS 会话工厂
				} else {
					// 设置响应状态码为 404（未找到）
					response.setStatusCode(HttpStatus.NOT_FOUND);
					// 如果日志级别为调试
					if (logger.isDebugEnabled()) {
						// 记录调试日志，提示找不到会话
						logger.debug("Session not found, sessionId=" + sessionId +
								". The session may have been closed " +
								"(e.g. missed heart-beat) while a message was coming in.");
					}
					// 结束方法执行
					return;
				}
				// 如果会话不为 null
			} else {
				// 获取会话的主体
				Principal principal = session.getPrincipal();
				// 如果主体不为 null，并且与请求的主体不相同
				if (principal != null && !principal.equals(request.getPrincipal())) {
					// 记录调试日志，提示会话的用户与请求的用户不匹配
					logger.debug("The user for the session does not match the user for the request.");
					// 设置响应状态码为 404（未找到）
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
			}

			// 如果传输类型发送不缓存指令
			if (transportType.sendsNoCacheInstruction()) {
				// 添加不缓存的响应头
				addNoCacheHeaders(response);
			}
			// 如果传输类型支持 CORS，并且跨域检查不通过
			if (transportType.supportsCors() && !checkOrigin(request, response)) {
				// 结束方法执行
				return;
			}

			// 处理传输请求
			transportHandler.handleRequest(request, response, handler, session);

			// 如果是新会话，并且响应是 ServletServerHttpResponse 的实例
			if (isNewSession && (response instanceof ServletServerHttpResponse)) {
				// 获取响应状态码
				int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
				// 如果状态码属于 4xx 客户端错误
				if (HttpStatus.valueOf(status).is4xxClientError()) {
					// 移除会话
					this.sessions.remove(sessionId);
				}
			}

			// 应用握手后的拦截器
			chain.applyAfterHandshake(request, response, null);
		} catch (SockJsException ex) {
			failure = ex;
		} catch (Exception ex) {
			failure = new SockJsException("Uncaught failure for request " + request.getURI(), sessionId, ex);
		} finally {
			// 如果存在异常
			if (failure != null) {
				// 应用握手后的拦截器，将异常传播
				chain.applyAfterHandshake(request, response, failure);
				throw failure;
			}
		}
	}

	@Override
	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		// 如果父类的校验请求方法不通过
		if (!super.validateRequest(serverId, sessionId, transport)) {
			// 返回false
			return false;
		}

		// 如果允许的来源不为空且不包含"*"，或者允许的来源模式不为空
		if (!getAllowedOrigins().isEmpty() && !getAllowedOrigins().contains("*") ||
				!getAllowedOriginPatterns().isEmpty()) {
			// 根据传输类型值获取对应的传输类型对象
			TransportType transportType = TransportType.fromValue(transport);
			// 如果传输类型为null，或者不支持来源验证
			if (transportType == null || !transportType.supportsOrigin()) {
				// 如果启用了警告级别的日志
				if (logger.isWarnEnabled()) {
					// 记录警告日志，指示启用了来源检查但传输类型不支持
					logger.warn("Origin check enabled but transport '" + transport + "' does not support it.");
				}
				// 返回false
				return false;
			}
		}

		// 返回true
		return true;
	}

	private SockJsSession createSockJsSession(String sessionId, SockJsSessionFactory sessionFactory,
											  WebSocketHandler handler, Map<String, Object> attributes) {

		// 尝试从会话集合中获取会话对象
		SockJsSession session = this.sessions.get(sessionId);
		// 如果会话对象存在，则直接返回
		if (session != null) {
			return session;
		}
		// 如果会话对象不存在，并且会话清理任务为null，则调度会话任务
		if (this.sessionCleanupTask == null) {
			scheduleSessionTask();
		}
		// 使用会话工厂创建会话对象
		session = sessionFactory.createSession(sessionId, handler, attributes);
		// 将其加入会话集合中后返回
		this.sessions.put(sessionId, session);
		return session;
	}

	private void scheduleSessionTask() {
		// 使用会话映射对象进行同步，确保线程安全
		synchronized (this.sessions) {
			// 如果会话清理任务不为null，直接返回
			if (this.sessionCleanupTask != null) {
				return;
			}
			// 使用任务调度器scheduleAtFixedRate方法创建一个定时任务，用于清理无效会话
			this.sessionCleanupTask = getTaskScheduler().scheduleAtFixedRate(() -> {
				// 创建一个存储已移除会话ID的列表
				List<String> removedIds = new ArrayList<>();
				// 遍历会话映射中的每个SockJs会话对象
				for (SockJsSession session : this.sessions.values()) {
					try {
						// 如果会话的最后活跃时间超过了设定的断开延迟时间
						if (session.getTimeSinceLastActive() > getDisconnectDelay()) {
							// 从会话映射中移除该会话
							this.sessions.remove(session.getId());
							// 将会话ID添加到已移除ID列表中
							removedIds.add(session.getId());
							// 关闭会话
							session.close();
						}
					} catch (Throwable ex) {
						// 可能是正常工作流程的一部分（例如，浏览器标签关闭）
						logger.debug("Failed to close " + session, ex);
					}
				}
				// 如果启用了调试级别的日志，并且已移除ID列表不为空
				if (logger.isDebugEnabled() && !removedIds.isEmpty()) {
					// 记录调试日志，指示已关闭了多少个会话及其ID
					logger.debug("Closed " + removedIds.size() + " sessions: " + removedIds);
				}
				// 定时任务的执行间隔为断开延迟时间
			}, getDisconnectDelay());
		}
	}

}
