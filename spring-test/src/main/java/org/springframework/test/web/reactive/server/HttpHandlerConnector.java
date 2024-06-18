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

package org.springframework.test.web.reactive.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.function.Function;

/**
 * 处理请求的连接器，通过调用 {@link HttpHandler} 处理请求，而不是向网络套接字发出实际请求。
 *
 * <p>该连接器内部使用和适配 {@link MockClientHttpRequest} 和 {@link MockClientHttpResponse}，
 * 将其转换为 {@link MockServerHttpRequest} 和 {@link MockServerHttpResponse}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHandlerConnector implements ClientHttpConnector {
	/**
	 * 日志记录器
	 */
	private static Log logger = LogFactory.getLog(HttpHandlerConnector.class);

	/**
	 * Http处理者
	 */
	private final HttpHandler handler;


	/**
	 * 使用指定的 {@link HttpHandler} 构造一个 {@link HttpHandlerConnector} 实例。
	 *
	 * @param handler 处理请求的 {@link HttpHandler} 实例
	 */
	public HttpHandlerConnector(HttpHandler handler) {
		Assert.notNull(handler, "HttpHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod httpMethod, URI uri,
											Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		return Mono.defer(() -> doConnect(httpMethod, uri, requestCallback))
				.subscribeOn(Schedulers.parallel());
	}

	private Mono<ClientHttpResponse> doConnect(
			HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// unsafe(): 我们正在拦截，已经序列化了 Publisher 信号
		Sinks.Empty<Void> requestWriteSink = Sinks.unsafe().empty();
		Sinks.Empty<Void> handlerSink = Sinks.unsafe().empty();
		ClientHttpResponse[] savedResponse = new ClientHttpResponse[1];

		// 创建 MockClientHttpRequest 对象
		MockClientHttpRequest mockClientRequest = new MockClientHttpRequest(httpMethod, uri);
		// 创建 MockServerHttpResponse 对象
		MockServerHttpResponse mockServerResponse = new MockServerHttpResponse();

		// 设置 模拟客户端请求 的写入处理器
		mockClientRequest.setWriteHandler(requestBody -> {
			log("Invoking HttpHandler for ", httpMethod, uri);
			// 适配请求，并获取 模拟服务器请求 对象
			ServerHttpRequest mockServerRequest = adaptRequest(mockClientRequest, requestBody);
			// 准备响应，并获取 要使用的响应 对象
			ServerHttpResponse responseToUse = prepareResponse(mockServerResponse, mockServerRequest);
			// 调用处理器处理请求，并订阅处理结果
			this.handler.handle(mockServerRequest, responseToUse).subscribe(
					aVoid -> {
					},
					// 忽略结果：信号无法竞争
					handlerSink::tryEmitError,
					handlerSink::tryEmitEmpty);
			return Mono.empty();
		});

		// 设置 模拟服务器响应 的写入处理器
		mockServerResponse.setWriteHandler(responseBody ->
				Mono.fromRunnable(() -> {
					log("Creating client response for ", httpMethod, uri);
					// 适配响应，并保存在 savedResponse[0] 中
					savedResponse[0] = adaptResponse(mockServerResponse, responseBody);
				}));

		// 输出写入客户端请求的日志
		log("Writing client request for ", httpMethod, uri);
		// 应用请求回调，并订阅请求写入结果
		requestCallback.apply(mockClientRequest).subscribe(
				aVoid -> {
				},
				// 忽略结果：信号无法竞争
				requestWriteSink::tryEmitError,
				requestWriteSink::tryEmitEmpty);

		// 等待 请求写入接收器 和 处理器接收器 的信号都完成，如果出错，则处理异常
		return Mono.when(requestWriteSink.asMono(), handlerSink.asMono())
				.onErrorMap(ex -> {
					ClientHttpResponse response = savedResponse[0];
					// 如果已保存的响应不为空，则抛出 FailureAfterResponseCompletedException 异常
					return response != null ? new FailureAfterResponseCompletedException(response, ex) : ex;
				})
				// 然后返回一个 Mono，从 第一个已保存响应 获取响应或适配空响应
				.then(Mono.fromCallable(() -> savedResponse[0] != null ?
						savedResponse[0] : adaptResponse(mockServerResponse, Flux.empty())));
	}

	private void log(String message, HttpMethod httpMethod, URI uri) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("%s %s \"%s\"", message, httpMethod, uri));
		}
	}

	private ServerHttpRequest adaptRequest(MockClientHttpRequest request, Publisher<DataBuffer> body) {
		// 获取请求的方法（GET、POST 等）
		HttpMethod method = request.getMethod();
		// 获取请求的 URI
		URI uri = request.getURI();
		// 获取请求的 Http头部 对象
		HttpHeaders headers = request.getHeaders();
		// 获取请求的 cookies
		MultiValueMap<String, HttpCookie> cookies = request.getCookies();
		// 创建 MockServerHttpRequest 对象
		return MockServerHttpRequest.method(method, uri).headers(headers).cookies(cookies).body(body);
	}

	private ServerHttpResponse prepareResponse(ServerHttpResponse response, ServerHttpRequest request) {
		return (request.getMethod() == HttpMethod.HEAD ? new HttpHeadResponseDecorator(response) : response);
	}

	private ClientHttpResponse adaptResponse(MockServerHttpResponse response, Flux<DataBuffer> body) {
		// 获取响应的原始状态码
		Integer status = response.getRawStatusCode();
		// 创建 MockClientHttpResponse 对象，如果 status 不为 null，则使用 状态码；否则默认为 200
		MockClientHttpResponse clientResponse = new MockClientHttpResponse((status != null) ? status : 200);
		// 将响应的 头部信息 放入 客户端响应 的 响应头 中
		clientResponse.getHeaders().putAll(response.getHeaders());
		// 将响应的 cookies 放入 客户端响应 的 cookies 中
		clientResponse.getCookies().putAll(response.getCookies());
		// 设置 客户端响应 的响应体
		clientResponse.setBody(body);
		// 返回构建好的 客户端响应 对象
		return clientResponse;
	}


	/**
	 * 表示在服务器响应已完成后，通过 {@link ServerHttpResponse#writeWith} 或 {@link ServerHttpResponse#setComplete()} 发生错误，
	 * 并且不能再更改响应的异常。此异常包装了错误，并提供了访问 {@link #getCompletedResponse() 完成的响应} 的方法。
	 * <p>在实际运行的服务器上，发生错误的时机取决于服务器何时提交响应，错误可能会或可能不会改变响应。
	 * 因此，在没有服务器的测试中，该异常被包装并允许传播，以便应用程序得到警告。
	 *
	 * @since 5.2.2
	 */
	@SuppressWarnings("serial")
	public static final class FailureAfterResponseCompletedException extends RuntimeException {

		/**
		 * 已完成的客户端响应
		 */
		private final ClientHttpResponse completedResponse;

		/**
		 * 构造一个 {@link FailureAfterResponseCompletedException} 实例。
		 *
		 * @param response 已完成的客户端响应
		 * @param cause    异常原因
		 */
		private FailureAfterResponseCompletedException(ClientHttpResponse response, Throwable cause) {
			super("Error occurred after response was completed: " + response, cause);
			this.completedResponse = response;
		}

		/**
		 * 获取已完成的客户端响应。
		 *
		 * @return 已完成的客户端响应
		 */
		public ClientHttpResponse getCompletedResponse() {
			return this.completedResponse;
		}
	}

}
