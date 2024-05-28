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

package org.springframework.web.server.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * 通过设置响应状态码来处理 {@link ResponseStatusException}。
 *
 * <p>默认情况下，成功解析的异常不会显示堆栈跟踪。
 * 使用 {@link #setWarnLogCategory(String)} 来启用带有堆栈跟踪的日志记录。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ResponseStatusExceptionHandler implements WebExceptionHandler {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ResponseStatusExceptionHandler.class);

	/**
	 * 警告日志记录器
	 */
	@Nullable
	private Log warnLogger;


	/**
	 * 设置警告日志记录器的日志类别。
	 * <p>默认为没有警告日志记录。指定此设置以激活特定类别的警告日志记录。
	 *
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 * @since 5.1
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (!updateResponse(exchange.getResponse(), ex)) {
			// 如果无法更新响应，直接返回异常 Mono
			return Mono.error(ex);
		}

		// 在spring-webmvc中镜像AbstractHandlerExceptionResolver...
		String logPrefix = exchange.getLogPrefix();
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			// 检查 警告日志记录器 是否可用，如果可用且 警告 级别启用，则记录警告日志
			this.warnLogger.warn(logPrefix + formatError(ex, exchange.getRequest()));
		} else if (logger.isDebugEnabled()) {
			// 否则记录调试日志
			logger.debug(logPrefix + formatError(ex, exchange.getRequest()));
		}

		// 完成响应，返回一个空的 Mono
		return exchange.getResponse().setComplete();
	}


	private String formatError(Throwable ex, ServerHttpRequest request) {
		// 获取异常类的简单名称和消息
		String className = ex.getClass().getSimpleName();
		String message = LogFormatUtils.formatValue(ex.getMessage(), -1, true);
		// 获取请求的路径
		String path = request.getURI().getRawPath();
		// 构建日志字符串，描述已解析的异常类型、消息和请求的 HTTP 方法及路径
		return "Resolved [" + className + ": " + message + "] for HTTP " + request.getMethod() + " " + path;
	}

	private boolean updateResponse(ServerHttpResponse response, Throwable ex) {
		// 初始化结果为 false
		boolean result = false;
		// 确定异常的 HTTP 状态码
		HttpStatus httpStatus = determineStatus(ex);
		// 获取异常的 HTTP 状态码，若不存在则使用异常原始状态码
		int code = (httpStatus != null ? httpStatus.value() : determineRawStatusCode(ex));
		// 如果状态码存在
		if (code != -1) {
			// 如果设置原始状态码成功
			if (response.setRawStatusCode(code)) {
				// 如果异常是 ResponseStatusException 类型
				if (ex instanceof ResponseStatusException) {
					// 将 ResponseStatusException 的响应头添加到响应中
					((ResponseStatusException) ex).getResponseHeaders()
							.forEach((name, values) ->
									values.forEach(value -> response.getHeaders().add(name, value)));
				}
				// 更新结果为 true
				result = true;
			}
		} else {
			// 获取异常的原因
			Throwable cause = ex.getCause();
			// 如果原因不为空
			if (cause != null) {
				// 更新响应，递归处理原因
				result = updateResponse(response, cause);
			}
		}
		// 返回结果
		return result;
	}

	/**
	 * 确定给定异常的 HTTP 状态。
	 * <p>从 5.3 开始，此方法始终返回 {@code null}，此时将使用 {@link #determineRawStatusCode(Throwable)}。
	 *
	 * @param ex 要检查的异常
	 * @return 相关联的 HTTP 状态，如果没有则为 {@code null}
	 * @deprecated 从 5.3 开始，建议使用 {@link #determineRawStatusCode(Throwable)}。
	 */
	@Nullable
	@Deprecated
	protected HttpStatus determineStatus(Throwable ex) {
		return null;
	}

	/**
	 * 这个方法确定给定异常的原始状态码。
	 *
	 * @param ex 要检查的异常
	 * @return 相关联的 HTTP 状态码，如果无法推断则返回 -1。
	 * @since 5.3
	 */
	protected int determineRawStatusCode(Throwable ex) {
		if (ex instanceof ResponseStatusException) {
			// 如果是 ResponseStatusException 类型的异常，获取其原始状态码
			return ((ResponseStatusException) ex).getRawStatusCode();
		}
		// 否则返回 -1，表示无法推断状态码
		return -1;
	}


}
