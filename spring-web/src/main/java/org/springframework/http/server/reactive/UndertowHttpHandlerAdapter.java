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

package org.springframework.http.server.reactive;

import io.undertow.server.HttpServerExchange;
import org.apache.commons.logging.Log;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 将 {@link HttpHandler} 适配到 Undertow 的 {@link io.undertow.server.HttpHandler}。
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {
	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(UndertowHttpHandlerAdapter.class);

	/**
	 * Http处理器
	 */
	private final HttpHandler httpHandler;

	/**
	 * 数据缓冲工厂，默认的共享的数据缓冲工厂
	 */
	private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;


	public UndertowHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	public void setDataBufferFactory(DataBufferFactory bufferFactory) {
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		this.bufferFactory = bufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.bufferFactory;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) {
		UndertowServerHttpRequest request = null;
		try {
			// 创建 Undertow服务器Http请求 对象
			request = new UndertowServerHttpRequest(exchange, getDataBufferFactory());
		} catch (URISyntaxException ex) {
			// 如果发生 URISyntaxException 异常
			if (logger.isWarnEnabled()) {
				// 输出警告级别日志，记录获取请求 URI 失败的异常信息
				logger.debug("Failed to get request URI: " + ex.getMessage());
			}
			// 设置交换的状态码为 400 Bad Request
			exchange.setStatusCode(400);
			// 方法结束，返回
			return;
		}

		//  创建 Undertow服务器Http响应 对象
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange, getDataBufferFactory(), request);

		// 如果请求的方法为 HEAD
		if (request.getMethod() == HttpMethod.HEAD) {
			// 使用 HttpHeadResponseDecorator 封装 响应 对象
			response = new HttpHeadResponseDecorator(response);
		}

		// 创建 处理程序结果订阅服务器 对象，用于处理结果订阅
		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(exchange, request);

		//  处理请求和响应，并订阅 处理程序结果订阅服务器 对象处理结果
		this.httpHandler.handle(request, response).subscribe(resultSubscriber);
	}


	private static class HandlerResultSubscriber implements Subscriber<Void> {
		/**
		 * Http服务端交换
		 */
		private final HttpServerExchange exchange;

		/**
		 * 日志前缀
		 */
		private final String logPrefix;


		public HandlerResultSubscriber(HttpServerExchange exchange, UndertowServerHttpRequest request) {
			this.exchange = exchange;
			this.logPrefix = request.getLogPrefix();
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// 无操作
		}

		@Override
		public void onError(Throwable ex) {
			// 输出跟踪级别日志，记录失败完成的消息和异常信息
			logger.trace(this.logPrefix + "Failed to complete: " + ex.getMessage());

			// 如果响应已经开始发送
			if (this.exchange.isResponseStarted()) {
				try {
					// 输出调试级别日志，记录关闭连接的操作
					logger.debug(this.logPrefix + "Closing connection");
					// 关闭与交换的连接
					this.exchange.getConnection().close();
				} catch (IOException ex2) {
					// 忽略关闭连接时的 IOException 异常
				}
			} else {
				// 输出调试级别日志，记录设置 HttpServerExchange 状态为 500 服务器错误的操作
				logger.debug(this.logPrefix + "Setting HttpServerExchange status to 500 Server Error");
				// 设置交换的状态码为 500
				this.exchange.setStatusCode(500);
				// 结束交换
				this.exchange.endExchange();
			}
		}

		@Override
		public void onComplete() {
			// 记录跟踪日志
			logger.trace(this.logPrefix + "Handling completed");
			// 结束交换
			this.exchange.endExchange();
		}
	}

}
