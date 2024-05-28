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

package org.springframework.web.server.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 将 {@link WebHandler} 适配到 {@link HttpHandler} 合同的默认适配器。
 *
 * <p>默认情况下，创建并配置 {@link DefaultServerWebExchange}，然后调用目标 {@code WebHandler}。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	/**
	 * 用于断开连接的客户端异常的专用日志类别。
	 * <p>Servlet 容器不会公开客户端断开连接的回调；参见
	 * <a href="https://github.com/eclipse-ee4j/servlet-api/issues/44">eclipse-ee4j/servlet-api#44</a>。
	 * <p>为了避免填充日志记录中的不必要的堆栈跟踪，我们努力在每个服务器上基于网络失败进行识别，
	 * 然后在单独的日志类别下以 DEBUG 级别记录简单的单行消息，或仅在 TRACE 级别记录完整的堆栈跟踪。
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.server.DisconnectedClient";

	/**
	 * AbstractSockJsSession 中存在类似的声明..
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(
			Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

	/**
	 * 丢失的客户端记录器
	 */
	private static final Log lostClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

	/**
	 * 网络会话管理器
	 */
	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	/**
	 * 服务端编码配置
	 */
	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	/**
	 * 区域上下文解析器
	 */
	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	/**
	 * 转发报头转换器
	 */
	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 是否记录可能敏感信息（在 DEBUG 级别记录表单数据，在 TRACE 级别记录头部）。
	 */
	private boolean enableLoggingRequestDetails = false;


	public HttpWebHandlerAdapter(WebHandler delegate) {
		super(delegate);
	}


	/**
	 * 配置用于管理 Web 会话的自定义 {@link WebSessionManager}。提供的实例设置在每个创建的
	 * {@link DefaultServerWebExchange} 上。
	 * <p>默认情况下，此属性设置为 {@link DefaultWebSessionManager}。
	 *
	 * @param sessionManager 要使用的会话管理器
	 */
	public void setSessionManager(WebSessionManager sessionManager) {
		Assert.notNull(sessionManager, "WebSessionManager must not be null");
		this.sessionManager = sessionManager;
	}

	/**
	 * 返回配置的 {@link WebSessionManager}。
	 */
	public WebSessionManager getSessionManager() {
		return this.sessionManager;
	}

	/**
	 * 配置自定义 {@link ServerCodecConfigurer}。提供的实例设置在每个创建的
	 * {@link DefaultServerWebExchange} 上。
	 * <p>默认情况下，此属性设置为 {@link ServerCodecConfigurer#create()}。
	 *
	 * @param codecConfigurer 要使用的编解码器配置器
	 */
	public void setCodecConfigurer(ServerCodecConfigurer codecConfigurer) {
		Assert.notNull(codecConfigurer, "ServerCodecConfigurer is required");
		// 设置编解码器配置器
		this.codecConfigurer = codecConfigurer;

		// 初始化请求详细日志记录开关为false
		this.enableLoggingRequestDetails = false;
		// 检查读取器是否支持请求详细日志记录
		this.codecConfigurer.getReaders().stream()
				.filter(LoggingCodecSupport.class::isInstance)
				.forEach(reader -> {
					if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
						this.enableLoggingRequestDetails = true;
					}
				});
	}

	/**
	 * 返回配置的 {@link ServerCodecConfigurer}。
	 */
	public ServerCodecConfigurer getCodecConfigurer() {
		if (this.codecConfigurer == null) {
			// 如果编解码器配置器为空，则创建一个新的编解码器配置器
			setCodecConfigurer(ServerCodecConfigurer.create());
		}
		// 返回编解码器配置器
		return this.codecConfigurer;
	}

	/**
	 * 配置自定义 {@link LocaleContextResolver}。提供的实例设置在每个创建的
	 * {@link DefaultServerWebExchange} 上。
	 * <p>默认情况下，此属性设置为 {@link org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver}。
	 *
	 * @param resolver 要使用的区域设置上下文解析器
	 */
	public void setLocaleContextResolver(LocaleContextResolver resolver) {
		Assert.notNull(resolver, "LocaleContextResolver is required");
		this.localeContextResolver = resolver;
	}

	/**
	 * 返回配置的 {@link LocaleContextResolver}。
	 */
	public LocaleContextResolver getLocaleContextResolver() {
		return this.localeContextResolver;
	}

	/**
	 * 启用处理转发的头部，可以提取和删除，也可以仅删除。
	 * <p>默认情况下，此属性未设置。
	 *
	 * @param transformer 要使用的转换器
	 * @since 5.1
	 */
	public void setForwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		Assert.notNull(transformer, "ForwardedHeaderTransformer is required");
		this.forwardedHeaderTransformer = transformer;
	}

	/**
	 * 返回配置的 {@link ForwardedHeaderTransformer}。
	 *
	 * @since 5.1
	 */
	@Nullable
	public ForwardedHeaderTransformer getForwardedHeaderTransformer() {
		return this.forwardedHeaderTransformer;
	}

	/**
	 * 配置与 Web 应用程序关联的 {@code ApplicationContext}，如果通过
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)} 初始化。
	 *
	 * @param applicationContext 上下文
	 * @since 5.0.3
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回已配置的 {@code ApplicationContext} (如果有)。
	 *
	 * @since 5.0.3
	 */
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 在设置完所有属性后必须调用此方法以完成初始化。
	 */
	public void afterPropertiesSet() {
		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': form data and headers will be " + value);
		}
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		// 如果存在转发头转换器
		if (this.forwardedHeaderTransformer != null) {
			try {
				// 尝试应用转发头转换器到请求
				request = this.forwardedHeaderTransformer.apply(request);
			} catch (Throwable ex) {
				// 如果应用转发头转换器失败
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to apply forwarded headers to " + formatRequest(request), ex);
				}
				// 设置响应状态码为400 Bad Request
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				// 返回一个完成的响应
				return response.setComplete();
			}
		}
		// 创建服务器Web交换对象
		ServerWebExchange exchange = createExchange(request, response);

		// 跟踪调试信息
		LogFormatUtils.traceDebug(logger, traceOn ->
				exchange.getLogPrefix() + formatRequest(exchange.getRequest()) +
						(traceOn ? ", headers=" + formatHeaders(exchange.getRequest().getHeaders()) : ""));

		// 处理交换对象
		return getDelegate().handle(exchange)
				// 处理成功时记录响应
				.doOnSuccess(aVoid -> logResponse(exchange))
				// 处理错误时继续处理
				.onErrorResume(ex -> handleUnresolvedError(exchange, ex))
				// 设置响应为已完成
				.then(Mono.defer(response::setComplete));
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager,
				getCodecConfigurer(), getLocaleContextResolver(), this.applicationContext);
	}

	/**
	 * 格式化请求以便记录，包括 HTTP 方法和 URL。
	 * <p>默认情况下，这将打印 HTTP 方法、URL 路径和查询。
	 *
	 * @param request 要格式化的请求
	 * @return 要显示的字符串，永远不会为空或 {@code null}
	 */
	protected String formatRequest(ServerHttpRequest request) {
		// 获取原始查询字符串
		String rawQuery = request.getURI().getRawQuery();
		// 构建查询字符串，如果原始查询字符串不为空，则加上问号
		String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
		// 构建HTTP请求的字符串表示，包括方法、路径和查询字符串
		return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
	}

	private void logResponse(ServerWebExchange exchange) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			// 获取响应的状态码
			HttpStatus status = exchange.getResponse().getStatusCode();
			// 构建日志信息，包括完成状态和可能的响应头
			return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") +
					(traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
		});
	}

	private String formatHeaders(HttpHeaders responseHeaders) {
		return this.enableLoggingRequestDetails ?
				responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
	}

	private Mono<Void> handleUnresolvedError(ServerWebExchange exchange, Throwable ex) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String logPrefix = exchange.getLogPrefix();

		// 有时，远程调用错误看起来像是断开连接的客户端。
		// 尝试在“isDisconnectedClient”检查之前先设置响应。
		// 如果成功设置响应状态码为500 Internal Server Error
		if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
			// 记录500服务器错误日志
			logger.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
			// 返回一个空的Mono
			return Mono.empty();
		} else if (isDisconnectedClientError(ex)) {
			// 如果是断开连接的客户端错误
			// 如果丢失客户端日志记录器的跟踪级别是TRACE，则记录客户端断开连接
			if (lostClientLogger.isTraceEnabled()) {
				lostClientLogger.trace(logPrefix + "Client went away", ex);
			} else if (lostClientLogger.isDebugEnabled()) {
				// 如果丢失客户端日志记录器的调试级别是DEBUG
				// 记录客户端断开连接的信息
				lostClientLogger.debug(logPrefix + "Client went away: " + ex +
						" (stacktrace at TRACE level for '" + DISCONNECTED_CLIENT_LOG_CATEGORY + "')");
			}
			// 返回一个空的Mono
			return Mono.empty();
		} else {
			// 如果响应已经提交，并且不是断开连接的客户端错误
			// 在响应提交后，将错误传播到服务器，并记录错误日志
			logger.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request) +
					", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
			// 返回一个包含错误的Mono
			return Mono.error(ex);
		}
	}

	private boolean isDisconnectedClientError(Throwable ex) {
		// 获取异常堆栈中最具体的原因的消息
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		// 如果消息不为空
		if (message != null) {
			// 将消息转换为小写
			String text = message.toLowerCase();
			// 如果消息包含"broken pipe"或"connection reset by peer"，则返回true
			if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
				return true;
			}
		}
		// 如果异常类型的简单类名存在于已知的断开连接的客户端异常集合中，则返回true
		return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName());
	}

}
