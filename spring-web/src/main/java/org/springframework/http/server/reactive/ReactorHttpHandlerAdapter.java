/*
 * Copyright 2002-2018 the original author or authors.
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

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URISyntaxException;
import java.util.function.BiFunction;

/**
 * 将 {@link HttpHandler} 适配为 Reactor Netty 的通道处理函数。
 *
 * <p>此适配器允许使用 {@link HttpHandler} 处理 HTTP 请求，并将其转换为 Reactor Netty 的 {@link Mono<Void>} 形式。
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorHttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(ReactorHttpHandlerAdapter.class);

	/**
	 * Http处理器
	 */
	private final HttpHandler httpHandler;

	/**
	 * 构造一个适配器实例。
	 *
	 * @param httpHandler 要适配的 {@link HttpHandler} 实例，不能为空
	 */
	public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	@Override
	public Mono<Void> apply(HttpServerRequest reactorRequest, HttpServerResponse reactorResponse) {
		// 使用 Reactor响应 的内存分配器创建 Netty数据缓冲区工厂
		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(reactorResponse.alloc());

		try {
			// 创建 Reactor服务器Http请求 和 Reactor服务器Http响应 对象
			ReactorServerHttpRequest request = new ReactorServerHttpRequest(reactorRequest, bufferFactory);
			ServerHttpResponse response = new ReactorServerHttpResponse(reactorResponse, bufferFactory);

			if (request.getMethod() == HttpMethod.HEAD) {
				// 如果请求的方法是 HEAD，则使用 Http头部响应装饰器 装饰响应对象
				response = new HttpHeadResponseDecorator(response);
			}

			// 调用 Http处理器 处理请求和响应，并返回处理结果
			return this.httpHandler.handle(request, response)
					// 处理处理失败时的日志输出
					.doOnError(ex -> logger.trace(request.getLogPrefix() + "Failed to complete: " + ex.getMessage()))
					// 处理处理成功时的日志输出
					.doOnSuccess(aVoid -> logger.trace(request.getLogPrefix() + "Handling completed"));
		} catch (URISyntaxException ex) {
			// 如果 URI 解析失败，则记录错误日志
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to get request URI: " + ex.getMessage());
			}
			// 设置响应状态为 400 Bad Request
			reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
			// 返回空的 Mono
			return Mono.empty();
		}
	}

}
