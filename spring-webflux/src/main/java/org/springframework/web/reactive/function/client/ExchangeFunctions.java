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

package org.springframework.web.reactive.function.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 创建 ExchangeFunction 的静态工厂方法集合。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class ExchangeFunctions {

	private static final Log logger = LogFactory.getLog(ExchangeFunctions.class);


	/**
	 * 使用给定的客户端HTTP连接器创建ExchangeFunction。
	 * 这等同于调用 {@link #create(ClientHttpConnector, ExchangeStrategies)} 并传递 {@link ExchangeStrategies#withDefaults()}。
	 *
	 * @param connector 用于连接服务器的连接器
	 * @return 创建的 ExchangeFunction
	 */
	public static ExchangeFunction create(ClientHttpConnector connector) {
		return create(connector, ExchangeStrategies.withDefaults());
	}

	/**
	 * 使用给定的客户端HTTP连接器和ExchangeStrategies创建ExchangeFunction。
	 *
	 * @param connector  用于连接服务器的连接器
	 * @param strategies 要使用的 ExchangeStrategies
	 * @return 创建的 ExchangeFunction
	 */
	public static ExchangeFunction create(ClientHttpConnector connector, ExchangeStrategies strategies) {
		return new DefaultExchangeFunction(connector, strategies);
	}


	/**
	 * 默认的ExchangeFunction实现。
	 */
	private static class DefaultExchangeFunction implements ExchangeFunction {

		/**
		 * 客户端HTTP连接器。
		 */
		private final ClientHttpConnector connector;

		/**
		 * 交换策略。
		 */
		private final ExchangeStrategies strategies;

		/**
		 * 是否启用记录请求详细信息的标志。
		 */
		private boolean enableLoggingRequestDetails;


		/**
		 * 构造函数，接受连接器和策略。
		 *
		 * @param connector  连接器
		 * @param strategies 交换策略
		 */
		public DefaultExchangeFunction(ClientHttpConnector connector, ExchangeStrategies strategies) {
			// 检查连接器和交换策略是否为 null
			Assert.notNull(connector, "ClientHttpConnector must not be null");
			Assert.notNull(strategies, "ExchangeStrategies must not be null");

			// 设置连接器和交换策略
			this.connector = connector;
			this.strategies = strategies;

			// 检查消息写入器中是否有启用请求详情日志记录的实例，并相应地设置标志
			strategies.messageWriters().stream()
					.filter(LoggingCodecSupport.class::isInstance)
					.forEach(reader -> {
						if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
							this.enableLoggingRequestDetails = true;
						}
					});
		}

		/**
		 * 交换方法，用于发起请求和获取响应。
		 *
		 * @param clientRequest 客户端请求
		 * @return 响应的Mono
		 */
		@Override
		public Mono<ClientResponse> exchange(ClientRequest clientRequest) {
			// 检查 ClientRequest 是否为 null
			Assert.notNull(clientRequest, "ClientRequest must not be null");

			// 获取请求方法和 URL
			HttpMethod httpMethod = clientRequest.method();
			URI url = clientRequest.url();

			// 连接并发出请求，处理连接和请求过程
			return this.connector
					.connect(httpMethod, url, httpRequest -> clientRequest.writeTo(httpRequest, this.strategies))
					// 记录请求
					.doOnRequest(n -> logRequest(clientRequest))
					// 取消时关闭连接
					.doOnCancel(() -> logger.debug(clientRequest.logPrefix() + "Cancel signal (to close connection)"))
					// 错误处理
					.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, t -> wrapException(t, clientRequest))
					.map(httpResponse -> {
						// 记录响应
						String logPrefix = getLogPrefix(clientRequest, httpResponse);
						logResponse(httpResponse, logPrefix);

						// 创建并返回 DefaultClientResponse
						return new DefaultClientResponse(
								httpResponse, this.strategies, logPrefix, httpMethod.name() + " " + url,
								() -> createRequest(clientRequest));
					});

		}

		/**
		 * 记录请求信息的私有方法。
		 *
		 * @param request 客户端请求
		 */
		private void logRequest(ClientRequest request) {
			LogFormatUtils.traceDebug(logger, traceOn ->
					request.logPrefix() + "HTTP " + request.method() + " " + request.url() +
							(traceOn ? ", headers=" + formatHeaders(request.headers()) : "")
			);
		}

		/**
		 * 获取日志前缀的私有方法。
		 *
		 * @param request  客户端请求
		 * @param response 客户端响应
		 * @return 日志前缀
		 */
		private String getLogPrefix(ClientRequest request, ClientHttpResponse response) {
			return request.logPrefix() + "[" + response.getId() + "] ";
		}

		/**
		 * 记录响应信息的私有方法。
		 *
		 * @param response  客户端响应
		 * @param logPrefix 日志前缀
		 */
		private void logResponse(ClientHttpResponse response, String logPrefix) {
			LogFormatUtils.traceDebug(logger, traceOn -> {
				int code = response.getRawStatusCode();
				HttpStatus status = HttpStatus.resolve(code);
				return logPrefix + "Response " + (status != null ? status : code) +
						(traceOn ? ", headers=" + formatHeaders(response.getHeaders()) : "");
			});
		}

		/**
		 * 格式化请求头信息的私有方法。
		 *
		 * @param headers 请求头
		 * @return 格式化后的请求头信息
		 */
		private String formatHeaders(HttpHeaders headers) {
			return this.enableLoggingRequestDetails ? headers.toString() : headers.isEmpty() ? "{}" : "{masked}";
		}

		/**
		 * 封装异常的私有方法。
		 *
		 * @param t   异常
		 * @param r   客户端请求
		 * @param <T> 泛型类型
		 * @return 封装了异常的Mono
		 */
		private <T> Mono<T> wrapException(Throwable t, ClientRequest r) {
			return Mono.error(() -> new WebClientRequestException(t, r.method(), r.url(), r.headers()));
		}

		/**
		 * 创建请求的私有方法。
		 *
		 * @param request 客户端请求
		 * @return HttpRequest实例
		 */
		private HttpRequest createRequest(ClientRequest request) {
			return new HttpRequest() {

				@Override
				public HttpMethod getMethod() {
					return request.method();
				}

				@Override
				public String getMethodValue() {
					return request.method().name();
				}

				@Override
				public URI getURI() {
					return request.url();
				}

				@Override
				public HttpHeaders getHeaders() {
					return request.headers();
				}
			};
		}
	}

}
