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

package org.springframework.web.server.handler;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link WebHandler} 装饰器，在委托 {@link WebHandler} 后调用一个或多个 {@link WebExceptionHandler WebExceptionHandlers}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExceptionHandlingWebHandler extends WebHandlerDecorator {
	/**
	 * 异常处理器列表
	 */
	private final List<WebExceptionHandler> exceptionHandlers;


	/**
	 * 创建给定委托的 {@code ExceptionHandlingWebHandler}。
	 *
	 * @param delegate 要委托的 WebHandler
	 * @param handlers 要应用的 WebExceptionHandlers
	 */
	public ExceptionHandlingWebHandler(WebHandler delegate, List<WebExceptionHandler> handlers) {
		super(delegate);
		List<WebExceptionHandler> handlersToUse = new ArrayList<>();
		handlersToUse.add(new CheckpointInsertingHandler());
		handlersToUse.addAll(handlers);
		this.exceptionHandlers = Collections.unmodifiableList(handlersToUse);
	}


	/**
	 * 返回配置的异常处理程序的只读列表。
	 */
	public List<WebExceptionHandler> getExceptionHandlers() {
		return this.exceptionHandlers;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		Mono<Void> completion;
		try {
			// 调用父类的处理方法获取完成信号
			completion = super.handle(exchange);
		} catch (Throwable ex) {
			// 如果发生异常，创建一个错误信号
			completion = Mono.error(ex);
		}

		// 遍历异常处理器
		for (WebExceptionHandler handler : this.exceptionHandlers) {
			// 使用onErrorResume操作处理异常
			completion = completion.onErrorResume(ex -> handler.handle(exchange, ex));
		}
		// 返回完成信号
		return completion;
	}


	/**
	 * WebExceptionHandler，用于插入包含当前 URL 信息的检查点。
	 * 必须是第一个，以确保在异常处理之前捕获错误信号，并且例如将其转换为错误响应。
	 *
	 * @since 5.2
	 */
	private static class CheckpointInsertingHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			ServerHttpRequest request = exchange.getRequest();
			// 获取原始查询字符串
			String rawQuery = request.getURI().getRawQuery();
			// 构建查询字符串，如果原始查询字符串不为空，则加上问号
			String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
			// 获取HTTP方法
			HttpMethod httpMethod = request.getMethod();
			// 构建HTTP请求的描述字符串，包括方法、路径和查询字符串
			String description = "HTTP " + httpMethod + " \"" + request.getPath() + query + "\"";
			// 创建一个错误信号并返回，附带异常和描述信息
			return Mono.<Void>error(ex).checkpoint(description + " [ExceptionHandlingWebHandler]");
		}
	}

}
